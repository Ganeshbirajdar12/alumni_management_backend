package com.example.AlumniManagementSytem.config;

import com.example.AlumniManagementSytem.Model.User;
import com.example.AlumniManagementSytem.Repository.UserRepository;
import com.example.AlumniManagementSytem.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuperAdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    @Value("${app.super-admin.first-name:Super}")
    private String firstName;

    @Value("${app.super-admin.last-name:Admin}")
    private String lastName;

    @Value("${app.super-admin.phone:+910000000000}")
    private String phone;

    @Override
    public void run(String... args) {
        log.info("Checking for super admin...");

        // 1. Does any SUPER_ADMIN already exist?
        boolean saExists = userRepository.existsByRole(UserRole.ROLE_SUPER_ADMIN);

        if (saExists) {
            log.info("✅ Super admin already exists. Skipping seed.");
            return;
        }

        // 2. Is the email already taken by a non-SA user?
        if (userRepository.existsByEmail(superAdminEmail.toLowerCase())) {
            log.warn("⚠️ Email {} is already taken. Cannot seed super admin.",
                    superAdminEmail);
            return;
        }

        // 3. Create SUPER_ADMIN
        User superAdmin = User.builder()
                .email(superAdminEmail.toLowerCase())
                .password(passwordEncoder.encode(superAdminPassword))
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phone)
                .role(UserRole.ROLE_SUPER_ADMIN)
                .emailVerified(true)
                .profileCompleted(true)
                .isActive(true)
                .isDeleted(false)
                .build();

        userRepository.save(superAdmin);

        log.info("═══════════════════════════════════════════════════");
        log.info("👑 SUPER ADMIN CREATED");
        log.info("   Email:    {}", superAdminEmail);
        log.info("   Password: {}", superAdminPassword);
        log.info("   ⚠️  CHANGE THE PASSWORD AFTER FIRST LOGIN!");
        log.info("═══════════════════════════════════════════════════");
    }
}