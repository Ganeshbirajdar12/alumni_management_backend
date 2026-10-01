package com.example.AlumniManagementSytem.Service;

import com.example.AlumniManagementSytem.DTOs.response.UserResponse;

import java.util.List;

public interface AdminManagementService {

    // Get all admins (ADMIN + SUPER_ADMIN)
    List<UserResponse> getAllAdmins();

    // Create new ADMIN — returns created admin
    UserResponse createAdmin(String firstName, String lastName,
                             String email, String password, Long actorId);

    // Deactivate an admin
    void deactivateAdmin(Long adminId, Long actorId);

    // Activate an admin
    void activateAdmin(Long adminId, Long actorId);

    // Soft delete an admin
    void deleteAdmin(Long adminId, Long actorId);

    // Reset password — returns new plain password
    String resetPassword(Long adminId, Long actorId);
}