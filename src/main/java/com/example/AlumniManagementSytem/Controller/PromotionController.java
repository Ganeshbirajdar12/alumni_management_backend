package com.example.AlumniManagementSytem.Controller;

import com.example.AlumniManagementSytem.DTOs.response.PromotionRequestResponse;
import com.example.AlumniManagementSytem.Service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /**
     * STEP 1: Student requests OTP
     * POST /api/v1/promotion/request-otp
     */
    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, String>> requestOtp(Authentication authentication) {
        String email = authentication.getName();
        promotionService.requestOtp(email);
        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to your email. Valid for 10 minutes.",
                "status", "success"
        ));
    }

    /**
     * STEP 2: Student verifies OTP
     * POST /api/v1/promotion/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<PromotionRequestResponse> verifyOtp(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String otp = request.get("otp");
        if (otp == null || otp.isEmpty()) {
            throw new com.example.AlumniManagementSytem.Exception.BadRequestException("OTP is required");
        }

        String email = authentication.getName();
        PromotionRequestResponse response = promotionService.verifyOtp(email, otp);
        return ResponseEntity.ok(response);
    }

    /**
     * Check my promotion status
     * GET /api/v1/promotion/status
     */
    @GetMapping("/status")
    public ResponseEntity<PromotionRequestResponse> getStatus(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(promotionService.getMyStatus(email));
    }
}