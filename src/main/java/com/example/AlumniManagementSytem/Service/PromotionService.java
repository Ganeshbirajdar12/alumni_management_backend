package com.example.AlumniManagementSytem.Service;

import com.example.AlumniManagementSytem.DTOs.response.PromotionRequestResponse;

import java.util.List;

public interface PromotionService {

    // ==================== STUDENT SIDE ====================

    // Student requests OTP
    void requestOtp(String studentEmail);

    // Student verifies OTP
    PromotionRequestResponse verifyOtp(String studentEmail, String otp);

    // Student checks status
    PromotionRequestResponse getMyStatus(String studentEmail);

    // ==================== ADMIN SIDE ====================

    // Admin views pending requests
    List<PromotionRequestResponse> getPendingRequests();

    // Admin approves
    PromotionRequestResponse approve(Long requestId, Long adminId, String remarks);

    // Admin rejects
    PromotionRequestResponse reject(Long requestId, Long adminId, String reason);
}