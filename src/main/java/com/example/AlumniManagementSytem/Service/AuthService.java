package com.example.AlumniManagementSytem.Service;


import com.example.AlumniManagementSytem.DTOs.request.ChangePasswordRequest;
import com.example.AlumniManagementSytem.DTOs.request.LoginRequest;
import com.example.AlumniManagementSytem.DTOs.request.RefreshTokenRequest;
import com.example.AlumniManagementSytem.DTOs.request.RegisterRequest;
import com.example.AlumniManagementSytem.DTOs.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String refreshToken);
    void changePassword(String email, ChangePasswordRequest request);
    void verifyEmail(String token);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}