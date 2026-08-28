package com.example.AlumniManagementSytem.Service;


import com.example.AlumniManagementSytem.DTOs.request.ProfileUpdateRequest;
import com.example.AlumniManagementSytem.DTOs.response.ProfileResponse;

public interface ProfileService {
    ProfileResponse getProfile(String email);
    ProfileResponse getProfileById(Long id);
    ProfileResponse updateProfile(String email, ProfileUpdateRequest request);
    ProfileResponse updateProfilePicture(String email, String profilePictureUrl);
    void deleteProfilePicture(String email);
    boolean isProfileComplete(String email);
}