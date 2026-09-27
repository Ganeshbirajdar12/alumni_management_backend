package com.example.AlumniManagementSytem.Controller;

import com.example.AlumniManagementSytem.DTOs.response.PromotionRequestResponse;
import com.example.AlumniManagementSytem.Service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionService promotionService;

    /**
     * Get all pending promotion requests
     * GET /api/v1/admin/promotions/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PromotionRequestResponse>> getPending() {
        return ResponseEntity.ok(promotionService.getPendingRequests());
    }

    /**
     * Approve a promotion
     * POST /api/v1/admin/promotions/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<PromotionRequestResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {

        Long adminId = 1L; // TODO: get from auth
        String remarks = body != null ? body.get("remarks") : null;

        return ResponseEntity.ok(promotionService.approve(id, adminId, remarks));
    }

    /**
     * Reject a promotion
     * POST /api/v1/admin/promotions/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<PromotionRequestResponse> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        Long adminId = 1L; // TODO: get from auth
        String reason = body.get("reason");

        if (reason == null || reason.isEmpty()) {
            throw new com.example.AlumniManagementSytem.Exception.BadRequestException(
                    "Rejection reason is required");
        }

        return ResponseEntity.ok(promotionService.reject(id, adminId, reason));
    }
}