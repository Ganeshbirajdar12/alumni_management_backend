package com.example.AlumniManagementSytem.ServiceImpl;

import com.example.AlumniManagementSytem.DTOs.response.PromotionRequestResponse;
import com.example.AlumniManagementSytem.Exception.BadRequestException;
import com.example.AlumniManagementSytem.Exception.ResourceNotFoundException;
import com.example.AlumniManagementSytem.Model.PromotionRequest;
import com.example.AlumniManagementSytem.Model.User;
import com.example.AlumniManagementSytem.Repository.PromotionRequestRepository;
import com.example.AlumniManagementSytem.Repository.UserRepository;

import com.example.AlumniManagementSytem.Service.PromotionService;
import com.example.AlumniManagementSytem.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final UserRepository userRepository;
    private final PromotionRequestRepository promotionRepository;
    private final EmailService emailService;

    // ==================== STUDENT SIDE ====================

    @Override
    @Transactional
    public void requestOtp(String studentEmail) {
        log.info("OTP requested by: {}", studentEmail);

        // 1. Find student
        User student = userRepository.findByEmail(studentEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Check if already alumni
        if (student.getRole() == UserRole.ROLE_ALUMNI) {
            throw new BadRequestException("You are already an alumni!");
        }

        // 3. Check for existing pending requests
        boolean hasPending = promotionRepository.existsByUserIdAndStatusIn(
                student.getId(),
                Arrays.asList("PENDING_OTP", "PENDING_APPROVAL")
        );
        if (hasPending) {
            throw new BadRequestException("You already have a pending promotion request");
        }

        // 4. Check 24h cooldown since last request
        promotionRepository.findTopByUserIdOrderByRequestedAtDesc(student.getId())
                .ifPresent(lastRequest -> {
                    LocalDateTime cooldownEnd = lastRequest.getRequestedAt().plusHours(24);
                    if (LocalDateTime.now().isBefore(cooldownEnd)) {
                        long minutesLeft = java.time.Duration.between(
                                LocalDateTime.now(), cooldownEnd).toMinutes();
                        throw new BadRequestException(
                                "Please wait " + minutesLeft + " more minutes before requesting again");
                    }
                });

        // 5. Generate OTP (6 digits)
        String otp = generateOtp();

        // 6. Save promotion request
        PromotionRequest request = new PromotionRequest();
        request.setUserId(student.getId());
        request.setEmail(student.getEmail());
        request.setOtp(otp);
        request.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        request.setOtpVerified(false);
        request.setStatus("PENDING_OTP");
        request.setRequestedAt(LocalDateTime.now());
        promotionRepository.save(request);

        // 7. Send OTP email
        emailService.sendPromotionOtp(student.getEmail(), student.getFullName(), otp);

        log.info("OTP sent to {} (expires in 10 min)", studentEmail);
    }

    @Override
    @Transactional
    public PromotionRequestResponse verifyOtp(String studentEmail, String otp) {
        log.info("OTP verification for: {}", studentEmail);

        // 1. Find student
        User student = userRepository.findByEmail(studentEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Find latest PENDING_OTP request
        PromotionRequest request = promotionRepository
                .findTopByUserIdAndStatusOrderByRequestedAtDesc(student.getId(), "PENDING_OTP")
                .orElseThrow(() -> new BadRequestException("No pending OTP request found"));

        // 3. Check if OTP expired
        if (LocalDateTime.now().isAfter(request.getOtpExpiresAt())) {
            request.setStatus("EXPIRED");
            promotionRepository.save(request);
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        // 4. Check OTP matches
        if (!request.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }

        // 5. Mark OTP as verified, status = PENDING_APPROVAL
        request.setOtpVerified(true);
        request.setStatus("PENDING_APPROVAL");
        promotionRepository.save(request);

        // 6. Send pending approval email
        emailService.sendPendingApprovalEmail(student.getEmail(), student.getFullName());

        log.info("OTP verified for {} - now pending admin approval", studentEmail);

        return mapToResponse(request, student);
    }

    @Override
    public PromotionRequestResponse getMyStatus(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PromotionRequest request = promotionRepository
                .findTopByUserIdOrderByRequestedAtDesc(student.getId())
                .orElse(null);

        if (request == null) {
            return null;
        }

        return mapToResponse(request, student);
    }

    // ==================== ADMIN SIDE ====================

    @Override
    public List<PromotionRequestResponse> getPendingRequests() {
        return promotionRepository.findByStatusOrderByRequestedAtAsc("PENDING_APPROVAL")
                .stream()
                .map(req -> {
                    User user = userRepository.findById(req.getUserId()).orElse(null);
                    return mapToResponse(req, user);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PromotionRequestResponse approve(Long requestId, Long adminId, String remarks) {
        log.info("Admin {} approving request {}", adminId, requestId);

        // 1. Find request
        PromotionRequest request = promotionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        // 2. Check status
        if (!"PENDING_APPROVAL".equals(request.getStatus())) {
            throw new BadRequestException("This request is not pending approval");
        }

        // 3. Find user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 4. Promote to alumni
        user.setRole(UserRole.ROLE_ALUMNI);
        userRepository.save(user);

        // 5. Update request
        request.setStatus("APPROVED");
        request.setAdminId(adminId);
        request.setAdminRemarks(remarks);
        request.setResolvedAt(LocalDateTime.now());
        promotionRepository.save(request);

        // 6. Send approval email
        emailService.sendApprovalEmail(user.getEmail(), user.getFullName());

        log.info("User {} promoted to ALUMNI", user.getEmail());

        return mapToResponse(request, user);
    }

    @Override
    @Transactional
    public PromotionRequestResponse reject(Long requestId, Long adminId, String reason) {
        log.info("Admin {} rejecting request {}", adminId, requestId);

        // 1. Find request
        PromotionRequest request = promotionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        // 2. Check status
        if (!"PENDING_APPROVAL".equals(request.getStatus())) {
            throw new BadRequestException("This request is not pending approval");
        }

        // 3. Find user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 4. Update request
        request.setStatus("REJECTED");
        request.setAdminId(adminId);
        request.setAdminRemarks(reason);
        request.setResolvedAt(LocalDateTime.now());
        promotionRepository.save(request);

        // 5. Send rejection email
        emailService.sendRejectionEmail(user.getEmail(), user.getFullName(), reason);

        log.info("Promotion rejected for user: {}", user.getEmail());

        return mapToResponse(request, user);
    }

    // ==================== HELPERS ====================

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private PromotionRequestResponse mapToResponse(PromotionRequest request, User user) {
        return PromotionRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .email(request.getEmail())
                .fullName(user != null ? user.getFullName() : null)
                .department(user != null ? user.getDepartment() : null)
                .graduationYear(user != null ? user.getGraduationYear() : null)
                .status(request.getStatus())
                .adminRemarks(request.getAdminRemarks())
                .requestedAt(request.getRequestedAt())
                .resolvedAt(request.getResolvedAt())
                .build();
    }
}