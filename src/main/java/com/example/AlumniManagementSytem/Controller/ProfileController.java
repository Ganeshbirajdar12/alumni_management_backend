package com.example.AlumniManagementSytem.Controller;

import com.example.AlumniManagementSytem.DTOs.request.ProfileUpdateRequest;
import com.example.AlumniManagementSytem.DTOs.response.ProfileResponse;
import com.example.AlumniManagementSytem.Service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile Management", description = "User profile management APIs")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(profileService.getProfile(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get profile by ID")
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable Long id) {
        return ResponseEntity.ok(profileService.getProfileById(id));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(profileService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/picture")
    @Operation(summary = "Update profile picture")
    public ResponseEntity<ProfileResponse> updateProfilePicture(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        return ResponseEntity.ok(
                profileService.updateProfilePicture(authentication.getName(), request.get("profilePicture"))
        );
    }

    @DeleteMapping("/picture")
    @Operation(summary = "Delete profile picture")
    public ResponseEntity<Void> deleteProfilePicture(Authentication authentication) {
        profileService.deleteProfilePicture(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/complete")
    @Operation(summary = "Check if profile is complete")
    public ResponseEntity<Map<String, Boolean>> isProfileComplete(Authentication authentication) {
        return ResponseEntity.ok(
                Map.of("complete", profileService.isProfileComplete(authentication.getName()))
        );
    }
}