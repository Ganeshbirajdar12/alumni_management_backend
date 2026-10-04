package com.example.AlumniManagementSytem.Repository;

import com.example.AlumniManagementSytem.Model.EventRegistration;
import com.example.AlumniManagementSytem.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    // Find a specific registration
    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    // Count active registrations for an event
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    // All registrations for an event (excluding cancelled)
    List<EventRegistration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);

    // All registrations by a user
    List<EventRegistration> findByUserIdAndStatus(Long userId, RegistrationStatus status);

    // Check if user is already registered
    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, RegistrationStatus status);
}