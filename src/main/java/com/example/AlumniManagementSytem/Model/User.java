package com.example.AlumniManagementSytem.Model;


import com.example.AlumniManagementSytem.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "phone_number")
        },
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_role", columnList = "role"),
                @Index(name = "idx_user_is_active", columnList = "is_active")
        }
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"refreshTokens"})
@ToString(callSuper = true, exclude = {"password", "refreshTokens"})
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone_number", unique = true, length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    // Academic Information
    @Column(name = "graduation_year", length = 4)
    private String graduationYear;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String degree;

    @Column(name = "roll_number", unique = true, length = 20)
    private String rollNumber;

    // Professional Information
    @Column(name = "current_company", length = 255)
    private String currentCompany;

    @Column(name = "current_position", length = 255)
    private String currentPosition;

    @Column(length = 100)
    private String industry;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    // Personal Information
    @Column(length = 255)
    private String location;

    @Column(name = "profile_picture", length = 500)
    private String profilePicture;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String skills;

    // Social Links
    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(name = "github_url", length = 255)
    private String githubUrl;

    @Column(name = "twitter_url", length = 255)
    private String twitterUrl;

    @Column(name = "personal_website", length = 255)
    private String personalWebsite;

    // Account Status
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "profile_completed")
    private Boolean profileCompleted = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    // Preferences
    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();

    // UserDetails Implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add role as authority
        authorities.add(new SimpleGrantedAuthority(role.name()));

        // Add individual permissions
        if (permissions != null) {
            authorities.addAll(
                    permissions.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toSet())
            );
        }

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (accountLocked != null && accountLocked) {
            if (accountLockedUntil != null && LocalDateTime.now().isAfter(accountLockedUntil)) {
                // Unlock account
                this.accountLocked = false;
                this.failedLoginAttempts = 0;
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(getIsActive()) ;
//                && Boolean.TRUE.equals(emailVerified);
    }

    // Helper Methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void recordLoginSuccess(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
        this.accountLockedUntil = null;
    }

    public void recordLoginFailure() {
        this.failedLoginAttempts = (this.failedLoginAttempts != null ? this.failedLoginAttempts : 0) + 1;
        if (this.failedLoginAttempts >= 5) {
            this.accountLocked = true;
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }

    public void addRefreshToken(RefreshToken token) {
        refreshTokens.add(token);
        token.setUser(this);
    }

    public void removeRefreshToken(RefreshToken token) {
        refreshTokens.remove(token);
        token.setUser(null);
    }
}