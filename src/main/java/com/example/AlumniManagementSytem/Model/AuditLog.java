package com.example.AlumniManagementSytem.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actor_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who performed the action
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_email", length = 100)
    private String actorEmail;

    // What they did (e.g., "ADMIN_CREATED", "PROMOTION_APPROVED")
    @Column(nullable = false, length = 50)
    private String action;

    // Who/what was affected
    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_email", length = 100)
    private String targetEmail;

    // Extra info (free text)
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}