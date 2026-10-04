package com.example.AlumniManagementSytem.Model;

import com.example.AlumniManagementSytem.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_registrations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_user", columnNames = {"event_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_reg_event", columnList = "event_id"),
                @Index(name = "idx_reg_user", columnList = "user_id"),
                @Index(name = "idx_reg_status", columnList = "status")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.REGISTERED;

    @Column(name = "rsvp_at", nullable = false)
    private LocalDateTime rsvpAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "attended_at")
    private LocalDateTime attendedAt;

    @PrePersist
    protected void onCreate() {
        if (rsvpAt == null) {
            rsvpAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RegistrationStatus.REGISTERED;
        }
    }
}