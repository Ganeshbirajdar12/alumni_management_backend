package com.example.AlumniManagementSytem.ServiceImpl;

import com.example.AlumniManagementSytem.DTOs.response.UserResponse;
import com.example.AlumniManagementSytem.Exception.BadRequestException;
import com.example.AlumniManagementSytem.Exception.ResourceNotFoundException;
import com.example.AlumniManagementSytem.Model.User;
import com.example.AlumniManagementSytem.Repository.UserRepository;
import com.example.AlumniManagementSytem.Service.AdminManagementService;
import com.example.AlumniManagementSytem.Service.AuditService;

import com.example.AlumniManagementSytem.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminManagementServiceImpl implements AdminManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final EmailService emailService;

    // ==================== LIST ====================

    @Override
    public List<UserResponse> getAllAdmins() {
        return userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN
                        || u.getRole() == UserRole.ROLE_SUPER_ADMIN)
                .filter(u -> Boolean.FALSE.equals(u.getIsDeleted()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== CREATE ====================

    @Override
    @Transactional
    public UserResponse createAdmin(String firstName, String lastName,
                                    String email, String password, Long actorId) {
        log.info("Creating admin: {}", email);

        // Validate email
        if (email == null || email.isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (userRepository.existsByEmail(email.toLowerCase())) {
            throw new BadRequestException("Email already registered");
        }

        // Auto-generate password if not provided
        String finalPassword = (password == null || password.isEmpty())
                ? generatePassword()
                : password;

        // Create admin user
        User admin = User.builder()
                .email(email.toLowerCase().trim())
                .password(passwordEncoder.encode(finalPassword))
                .firstName(firstName != null ? firstName.trim() : "")
                .lastName(lastName != null ? lastName.trim() : "")
                .role(UserRole.ROLE_ADMIN)
                .emailVerified(true)
                .profileCompleted(true)
                .isActive(true)
                .isDeleted(false)
                .build();

        userRepository.save(admin);

        // Send welcome email (don't fail the whole thing if email fails)
        try {
            emailService.sendAdminWelcomeEmail(
                    admin.getEmail(),
                    admin.getFullName(),
                    finalPassword
            );
        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }

        // Audit log
        User actor = userRepository.findById(actorId).orElse(null);
        auditService.log(
                actorId,
                actor != null ? actor.getEmail() : "SYSTEM",
                "ADMIN_CREATED",
                admin.getId(),
                admin.getEmail(),
                "New admin created: " + admin.getFullName()
        );

        log.info("Admin {} created successfully", email);
        return mapToResponse(admin);
    }

    // ==================== DEACTIVATE ====================

    @Override
    @Transactional
    public void deactivateAdmin(Long adminId, Long actorId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        // Cannot deactivate SUPER_ADMIN
        if (admin.getRole() == UserRole.ROLE_SUPER_ADMIN) {
            throw new BadRequestException("Cannot deactivate super admin");
        }

        admin.setIsActive(false);
        userRepository.save(admin);

        // Audit
        User actor = userRepository.findById(actorId).orElse(null);
        auditService.log(
                actorId,
                actor != null ? actor.getEmail() : "SYSTEM",
                "ADMIN_DEACTIVATED",
                admin.getId(),
                admin.getEmail(),
                "Admin deactivated"
        );

        log.info("Admin {} deactivated", admin.getEmail());
    }

    // ==================== ACTIVATE ====================

    @Override
    @Transactional
    public void activateAdmin(Long adminId, Long actorId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        admin.setIsActive(true);
        userRepository.save(admin);

        User actor = userRepository.findById(actorId).orElse(null);
        auditService.log(
                actorId,
                actor != null ? actor.getEmail() : "SYSTEM",
                "ADMIN_ACTIVATED",
                admin.getId(),
                admin.getEmail(),
                "Admin activated"
        );

        log.info("Admin {} activated", admin.getEmail());
    }

    // ==================== DELETE ====================

    @Override
    @Transactional
    public void deleteAdmin(Long adminId, Long actorId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        // Cannot delete SUPER_ADMIN
        if (admin.getRole() == UserRole.ROLE_SUPER_ADMIN) {
            throw new BadRequestException("Cannot delete super admin");
        }

        // Soft delete
        admin.setIsDeleted(true);
        admin.setIsActive(false);
        userRepository.save(admin);

        User actor = userRepository.findById(actorId).orElse(null);
        auditService.log(
                actorId,
                actor != null ? actor.getEmail() : "SYSTEM",
                "ADMIN_DELETED",
                admin.getId(),
                admin.getEmail(),
                "Admin soft-deleted"
        );

        log.info("Admin {} deleted", admin.getEmail());
    }

    // ==================== RESET PASSWORD ====================

    @Override
    @Transactional
    public String resetPassword(Long adminId, Long actorId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        String newPassword = generatePassword();
        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);

        User actor = userRepository.findById(actorId).orElse(null);
        auditService.log(
                actorId,
                actor != null ? actor.getEmail() : "SYSTEM",
                "PASSWORD_RESET_BY_ADMIN",
                admin.getId(),
                admin.getEmail(),
                "Password reset by super admin"
        );

        log.info("Password reset for admin: {}", admin.getEmail());
        return newPassword;
    }

    // ==================== HELPERS ====================

    private String generatePassword() {
        // Example output: "Abc7@xyz9"
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "@#$%&*";
        SecureRandom rnd = new SecureRandom();

        StringBuilder sb = new StringBuilder();
        sb.append(chars.charAt(rnd.nextInt(chars.length())));
        sb.append(digits.charAt(rnd.nextInt(digits.length())));
        sb.append(special.charAt(rnd.nextInt(special.length())));
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        sb.append(digits.charAt(rnd.nextInt(digits.length())));
        return sb.toString();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .profileCompleted(user.getProfileCompleted())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}