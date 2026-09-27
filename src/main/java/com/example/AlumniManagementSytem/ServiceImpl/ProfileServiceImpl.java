package com.example.AlumniManagementSytem.ServiceImpl;


import com.example.AlumniManagementSytem.DTOs.request.ProfileUpdateRequest;
import com.example.AlumniManagementSytem.DTOs.response.ProfileResponse;
import com.example.AlumniManagementSytem.Exception.BadRequestException;
import com.example.AlumniManagementSytem.Exception.ResourceNotFoundException;
import com.example.AlumniManagementSytem.Model.User;
import com.example.AlumniManagementSytem.Repository.UserRepository;
import com.example.AlumniManagementSytem.Service.ProfileService;
import com.example.AlumniManagementSytem.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(String email, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        boolean isAlumni = user.getRole() == UserRole.ROLE_ALUMNI;

        // ✅ Always allowed fields (Student + Alumni)
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null) user.setLastName(request.getLastName().trim());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getGraduationYear() != null) user.setGraduationYear(request.getGraduationYear());
        if (request.getDepartment() != null) user.setDepartment(request.getDepartment());
        if (request.getDegree() != null) user.setDegree(request.getDegree());
        if (request.getRollNumber() != null) user.setRollNumber(request.getRollNumber());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getProfilePicture() != null) user.setProfilePicture(request.getProfilePicture());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getSkills() != null) user.setSkills(request.getSkills());
        if (request.getLinkedinUrl() != null) user.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getGithubUrl() != null) user.setGithubUrl(request.getGithubUrl());
        if (request.getTwitterUrl() != null) user.setTwitterUrl(request.getTwitterUrl());
        if (request.getPersonalWebsite() != null) user.setPersonalWebsite(request.getPersonalWebsite());
        if (request.getEmailNotifications() != null) user.setEmailNotifications(request.getEmailNotifications());

        // 🔒 Work info — ONLY for alumni
        if (isAlumni) {
            if (request.getCurrentCompany() != null) user.setCurrentCompany(request.getCurrentCompany());
            if (request.getCurrentPosition() != null) user.setCurrentPosition(request.getCurrentPosition());
            if (request.getIndustry() != null) user.setIndustry(request.getIndustry());
            if (request.getYearsOfExperience() != null) user.setYearsOfExperience(request.getYearsOfExperience());
        } else {
            // 🚫 Student tried to send work info → reject entire request
            boolean triedToUpdateWorkInfo =
                    request.getCurrentCompany() != null ||
                            request.getCurrentPosition() != null ||
                            request.getIndustry() != null ||
                            request.getYearsOfExperience() != null;

            if (triedToUpdateWorkInfo) {
                throw new BadRequestException(
                        "Only alumni can update professional information. " +
                                "Please request a promotion to unlock these fields."
                );
            }
        }

        // Update profile completion flag
        user.setProfileCompleted(isProfileComplete(user));

        user = userRepository.save(user);
        log.info("Profile updated for {} (role: {})", email, user.getRole());

        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfilePicture(String email, String profilePictureUrl) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setProfilePicture(profilePictureUrl);
        user = userRepository.save(user);

        log.info("Profile picture updated for user: {}", email);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public void deleteProfilePicture(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setProfilePicture(null);
        userRepository.save(user);

        log.info("Profile picture deleted for user: {}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProfileComplete(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return isProfileComplete(user);
    }

    private boolean isProfileComplete(User user) {
        return user.getFirstName() != null && !user.getFirstName().isEmpty() &&
                user.getLastName() != null && !user.getLastName().isEmpty() &&
                user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() &&
                user.getGraduationYear() != null && !user.getGraduationYear().isEmpty() &&
                user.getDepartment() != null && !user.getDepartment().isEmpty() &&
                user.getCurrentCompany() != null && !user.getCurrentCompany().isEmpty() &&
                user.getCurrentPosition() != null && !user.getCurrentPosition().isEmpty() &&
                user.getLocation() != null && !user.getLocation().isEmpty();
    }

    private ProfileResponse mapToProfileResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .graduationYear(user.getGraduationYear())
                .department(user.getDepartment())
                .degree(user.getDegree())
                .rollNumber(user.getRollNumber())
                .currentCompany(user.getCurrentCompany())
                .currentPosition(user.getCurrentPosition())
                .industry(user.getIndustry())
                .yearsOfExperience(user.getYearsOfExperience())
                .location(user.getLocation())
                .profilePicture(user.getProfilePicture())
                .bio(user.getBio())
                .skills(user.getSkills())
                .linkedinUrl(user.getLinkedinUrl())
                .githubUrl(user.getGithubUrl())
                .twitterUrl(user.getTwitterUrl())
                .personalWebsite(user.getPersonalWebsite())
                .emailVerified(user.getEmailVerified())
                .profileCompleted(user.getProfileCompleted())
                .emailNotifications(user.getEmailNotifications())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}