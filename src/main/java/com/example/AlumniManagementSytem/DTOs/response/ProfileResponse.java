package com.example.AlumniManagementSytem.DTOs.response;


import com.example.AlumniManagementSytem.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private UserRole role;
    private String graduationYear;
    private String department;
    private String degree;
    private String rollNumber;
    private String currentCompany;
    private String currentPosition;
    private String industry;
    private Integer yearsOfExperience;
    private String location;
    private String profilePicture;
    private String bio;
    private String skills;
    private String linkedinUrl;
    private String githubUrl;
    private String twitterUrl;
    private String personalWebsite;
    private Boolean emailVerified;
    private Boolean profileCompleted;
    private Boolean emailNotifications;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}