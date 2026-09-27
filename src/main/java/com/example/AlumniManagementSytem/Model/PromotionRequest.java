package com.example.AlumniManagementSytem.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_requests")
@Data
public class PromotionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which student is requesting
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String email;

    // 6-digit OTP
    @Column(nullable = false, length = 6)
    private String otp;

    @Column(name = "otp_expires_at", nullable = false)
    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_verified", nullable = false)
    private Boolean otpVerified = false;

    // PENDING_OTP, PENDING_APPROVAL, APPROVED, REJECTED, EXPIRED
    @Column(nullable = false, length = 30)
    private String status = "PENDING_OTP";

    // Admin who approved/rejected
    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_remarks", length = 500)
    private String adminRemarks;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}