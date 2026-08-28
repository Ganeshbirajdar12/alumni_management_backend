package com.example.AlumniManagementSytem.DTOs.request;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Size(min = 4, max = 4, message = "Graduation year must be 4 digits")
    private String graduationYear;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Degree must not exceed 100 characters")
    private String degree;

    @Size(max = 20, message = "Roll number must not exceed 20 characters")
    private String rollNumber;

    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String currentCompany;

    @Size(max = 255, message = "Position must not exceed 255 characters")
    private String currentPosition;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    private Integer yearsOfExperience;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 500, message = "Profile picture URL must not exceed 500 characters")
    private String profilePicture;

    @Size(max = 2000, message = "Bio must not exceed 2000 characters")
    private String bio;

    @Size(max = 2000, message = "Skills must not exceed 2000 characters")
    private String skills;

    @Size(max = 255, message = "LinkedIn URL must not exceed 255 characters")
    private String linkedinUrl;

    @Size(max = 255, message = "GitHub URL must not exceed 255 characters")
    private String githubUrl;

    @Size(max = 255, message = "Twitter URL must not exceed 255 characters")
    private String twitterUrl;

    @Size(max = 255, message = "Personal website must not exceed 255 characters")
    private String personalWebsite;

    private Boolean emailNotifications;
}