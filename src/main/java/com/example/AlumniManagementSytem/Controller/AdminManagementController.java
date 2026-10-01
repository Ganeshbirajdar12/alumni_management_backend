package com.example.AlumniManagementSytem.Controller;

import com.example.AlumniManagementSytem.DTOs.response.UserResponse;
import com.example.AlumniManagementSytem.Exception.ResourceNotFoundException;
import com.example.AlumniManagementSytem.Model.AuditLog;
import com.example.AlumniManagementSytem.Model.User;
import com.example.AlumniManagementSytem.Repository.UserRepository;
import com.example.AlumniManagementSytem.Service.AdminManagementService;
import com.example.AlumniManagementSytem.Service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    // ==================== ADMIN MANAGEMENT ====================

    /**
     * Get all admins
     * GET /api/v1/super-admin/admins
     */
    @GetMapping("/admins")
    public ResponseEntity<List<UserResponse>> getAllAdmins() {
        return ResponseEntity.ok(adminManagementService.getAllAdmins());
    }

    /**
     * Create new admin
     * POST /api/v1/super-admin/admins
     * Body: { firstName, lastName, email, password? }
     */
    @PostMapping("/admins")
    public ResponseEntity<UserResponse> createAdmin(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        Long actorId = getCurrentUserId(auth);

        UserResponse created = adminManagementService.createAdmin(
                body.get("firstName"),
                body.get("lastName"),
                body.get("email"),
                body.get("password"),
                actorId
        );

        return ResponseEntity.ok(created);
    }

    /**
     * Deactivate admin
     * PUT /api/v1/super-admin/admins/{id}/deactivate
     */
    @PutMapping("/admins/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateAdmin(
            @PathVariable Long id,
            Authentication auth) {

        Long actorId = getCurrentUserId(auth);
        adminManagementService.deactivateAdmin(id, actorId);

        return ResponseEntity.ok(Map.of("message", "Admin deactivated"));
    }

    /**
     * Activate admin
     * PUT /api/v1/super-admin/admins/{id}/activate
     */
    @PutMapping("/admins/{id}/activate")
    public ResponseEntity<Map<String, String>> activateAdmin(
            @PathVariable Long id,
            Authentication auth) {

        Long actorId = getCurrentUserId(auth);
        adminManagementService.activateAdmin(id, actorId);

        return ResponseEntity.ok(Map.of("message", "Admin activated"));
    }

    /**
     * Delete admin
     * DELETE /api/v1/super-admin/admins/{id}
     */
    @DeleteMapping("/admins/{id}")
    public ResponseEntity<Map<String, String>> deleteAdmin(
            @PathVariable Long id,
            Authentication auth) {

        Long actorId = getCurrentUserId(auth);
        adminManagementService.deleteAdmin(id, actorId);

        return ResponseEntity.ok(Map.of("message", "Admin deleted"));
    }

    /**
     * Reset admin's password
     * POST /api/v1/super-admin/admins/{id}/reset-password
     */
    @PostMapping("/admins/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable Long id,
            Authentication auth) {

        Long actorId = getCurrentUserId(auth);
        String newPassword = adminManagementService.resetPassword(id, actorId);

        return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully",
                "newPassword", newPassword
        ));
    }

    // ==================== AUDIT LOGS ====================

    /**
     * Get audit logs
     * GET /api/v1/super-admin/audit-logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditService.getAllLogs());
    }

    // ==================== HELPERS ====================

    /**
     * Fetch the current user's DB id from the JWT-authenticated email
     */
    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Authentication required");
        }
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}