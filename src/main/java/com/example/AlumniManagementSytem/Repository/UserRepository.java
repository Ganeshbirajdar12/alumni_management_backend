package com.example.AlumniManagementSytem.Repository;


import com.example.AlumniManagementSytem.Model.User;
import com.example.AlumniManagementSytem.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Find users by role
    List<User> findByRole(UserRole role);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByRollNumber(String rollNumber);

    Boolean existsByEmail(String email);

    Boolean existsByPhoneNumber(String phoneNumber);

    Boolean existsByRollNumber(String rollNumber);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLocked = false " +
            "WHERE u.email = :email")
    void resetFailedAttempts(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 " +
            "WHERE u.email = :email")
    void incrementFailedAttempts(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountLocked = true, " +
            "u.accountLockedUntil = :lockTime WHERE u.email = :email")
    void lockAccount(@Param("email") String email,
                     @Param("lockTime") LocalDateTime lockTime);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, " +
            "u.lastLoginIp = :ipAddress WHERE u.email = :email")
    void updateLoginInfo(@Param("email") String email,
                         @Param("loginTime") LocalDateTime loginTime,
                         @Param("ipAddress") String ipAddress);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.email = :email")
    void verifyEmail(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.password = :newPassword WHERE u.email = :email")
    void updatePassword(@Param("email") String email,
                        @Param("newPassword") String newPassword);

    long countByRole(UserRole role);
}