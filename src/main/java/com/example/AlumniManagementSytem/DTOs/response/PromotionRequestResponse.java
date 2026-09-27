package com.example.AlumniManagementSytem.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequestResponse {

    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String department;
    private String graduationYear;
    private String status;
    private String adminRemarks;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;
}