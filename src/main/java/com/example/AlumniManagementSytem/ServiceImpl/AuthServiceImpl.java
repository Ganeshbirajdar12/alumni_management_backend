package com.example.AlumniManagementSytem.ServiceImpl;

import com.example.AlumniManagementSytem.DTOs.request.ChangePasswordRequest;
import com.example.AlumniManagementSytem.DTOs.request.LoginRequest;
import com.example.AlumniManagementSytem.DTOs.request.RefreshTokenRequest;
import com.example.AlumniManagementSytem.DTOs.request.RegisterRequest;
import com.example.AlumniManagementSytem.DTOs.response.AuthResponse;
import com.example.AlumniManagementSytem.DTOs.response.UserResponse;
import com.example.AlumniManagementSytem.Exception.BadRequestException;
import com.example.AlumniManagementSytem.Exception.DuplicateResourceException;
import com.example.AlumniManagementSytem.Exception.ResourceNotFoundException;
import com.example.AlumniManagementSytem.Exception.UnauthorizedException;
import com.example.AlumniManagementSytem.Model.RefreshToken;
import com.example.AlumniManagementSytem.Model.User;
import com.example.AlumniManagementSytem.Repository.RefreshTokenRepository;
import com.example.AlumniManagementSytem.Repository.UserRepository;
import com.example.AlumniManagementSytem.Security.JwtTokenProvider;
import com.example.AlumniManagementSytem.Service.AuthService;
import com.example.AlumniManagementSytem.enums.TokenType;
import com.example.AlumniManagementSytem.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Check if phone number already exists
        if (request.getPhoneNumber() != null &&
                userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("User", "phone number", request.getPhoneNumber());
        }

        // Check if roll number already exists (if provided)
        if (request.getRollNumber() != null &&
                userRepository.existsByRollNumber(request.getRollNumber())) {
            throw new DuplicateResourceException("User", "roll number", request.getRollNumber());
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(request.getPhoneNumber())
                .role(UserRole.ROLE_STUDENT)
                .graduationYear(request.getGraduationYear())
                .department(request.getDepartment())
                .degree(request.getDegree())
                .rollNumber(request.getRollNumber())
                .emailVerified(false)
                .profileCompleted(false)
                .isActive(true)
                .isDeleted(false)
                .build();

        // Save user
        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}", user.getId());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = generateRefreshToken(user);

        // Build response
        return buildAuthResponse(user, accessToken, refreshToken, true);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt with email: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Check if user is active
        if (Boolean.FALSE.equals(user.getIsActive())) {
            log.warn("Login attempt on deactivated account: {}", request.getEmail());
            throw new UnauthorizedException("Account is deactivated. Please contact support.");
        }

        // Check if account is locked
        if (!user.isAccountNonLocked()) {
            log.warn("Login attempt on locked account: {}", request.getEmail());
            throw new UnauthorizedException("Account is temporarily locked due to too many failed attempts. " +
                    "Please try again after 30 minutes or contact support.");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Record successful login
            user.recordLoginSuccess(request.getIpAddress());
            userRepository.save(user);

            // Revoke all previous refresh tokens for security
            refreshTokenRepository.revokeAllUserTokens(user.getId());

            // Generate new tokens
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = generateRefreshToken(user);

            log.info("User logged in successfully: {}", user.getEmail());

            return buildAuthResponse(user, accessToken, refreshToken, false);

        } catch (BadCredentialsException e) {
            // Record failed login attempt
            user.recordLoginFailure();
            userRepository.save(user);

            log.warn("Failed login attempt for user: {}", request.getEmail());
            throw e; // This will be caught by GlobalExceptionHandler
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(token -> {
                    // Validate token
                    if (Boolean.TRUE.equals(token.getIsRevoked())) {
                        throw new UnauthorizedException("Refresh token has been revoked");
                    }

                    if (Boolean.TRUE.equals(token.getIsUsed())) {
                        // Token reuse detected - possible token theft
                        // Revoke all tokens for this user
                        refreshTokenRepository.revokeAllUserTokens(token.getUser().getId());
                        log.warn("Token reuse detected for user: {}", token.getUser().getEmail());
                        throw new UnauthorizedException("Token reuse detected. All sessions have been invalidated.");
                    }

                    if (token.isExpired()) {
                        throw new UnauthorizedException("Refresh token has expired. Please login again.");
                    }

                    // Mark current token as used (token rotation)
                    token.setIsUsed(true);
                    refreshTokenRepository.save(token);

                    // Generate new tokens
                    User user = token.getUser();
                    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
                    String newRefreshToken = generateRefreshToken(user);

                    log.info("Token refreshed successfully for user: {}", user.getEmail());

                    return buildAuthResponse(user, accessToken, newRefreshToken, false);
                })
                .orElseThrow(() -> {
                    log.warn("Refresh token not found: {}", requestRefreshToken);
                    return new UnauthorizedException("Invalid refresh token");
                });
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            refreshTokenRepository.revokeToken(refreshToken);
            log.info("User logged out successfully, refresh token revoked");
        }
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Check if new password is same as old
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // Validate new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation don't match");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all tokens for security
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        log.info("Password changed successfully for user: {}", email);
    }

    @Override
    public void verifyEmail(String token) {
        // Extract email from token
        String email = jwtTokenProvider.getEmailFromToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email is already verified");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", email);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Generate password reset token
        String resetToken = jwtTokenProvider.generatePasswordResetToken(user.getEmail());

        // TODO: Send email with reset link
        // emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        log.info("Password reset token generated for user: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Validate token
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BadRequestException("Invalid or expired password reset token");
        }

        // Get email from token
        String email = jwtTokenProvider.getEmailFromToken(token);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all tokens
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        log.info("Password reset successfully for user: {}", email);
    }

    // ==================== Private Helper Methods ====================

    private String generateRefreshToken(User user) {
        String token = jwtTokenProvider.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .tokenType(TokenType.REFRESH_TOKEN.name())
                .expiryDate(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpiration()))
                .isRevoked(false)
                .isUsed(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        user.addRefreshToken(refreshToken);

        return token;
    }

    private AuthResponse buildAuthResponse(User user, String accessToken,
                                           String refreshToken, boolean isFirstLogin) {
        UserResponse userResponse = mapToUserResponse(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getAccessTokenExpiration()))
                .user(userResponse)
                .firstLogin(isFirstLogin)
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .graduationYear(user.getGraduationYear())
                .department(user.getDepartment())
                .degree(user.getDegree())
                .rollNumber(user.getRollNumber())
                .currentCompany(user.getCurrentCompany())
                .currentPosition(user.getCurrentPosition())
                .industry(user.getIndustry())
                .yearsOfExperience(user.getYearsOfExperience())
                .location(user.getLocation())
                .profilePicture(user.getProfilePicture())
                .bio(user.getBio())
                .skills(user.getSkills())
                .linkedinUrl(user.getLinkedinUrl())
                .githubUrl(user.getGithubUrl())
                .twitterUrl(user.getTwitterUrl())
                .personalWebsite(user.getPersonalWebsite())
                .emailVerified(user.getEmailVerified())
                .profileCompleted(user.getProfileCompleted())
                .lastLoginAt(user.getLastLoginAt())
                .emailNotifications(user.getEmailNotifications())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}