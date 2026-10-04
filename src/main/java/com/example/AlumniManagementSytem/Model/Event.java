package com.example.AlumniManagementSytem.Model;

import com.example.AlumniManagementSytem.enums.EventCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_date", columnList = "event_date"),
        @Index(name = "idx_event_category", columnList = "category"),
        @Index(name = "idx_event_organizer", columnList = "organizer_id"),
        @Index(name = "idx_event_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventCategory category;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @Column(length = 255)
    private String location;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "reminder_24h_sent", nullable = false)
    private Boolean reminder24hSent = false;

    @Column(name = "reminder_1h_sent", nullable = false)
    private Boolean reminder1hSent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (isOnline == null) isOnline = false;
        if (isFeatured == null) isFeatured = false;
        if (isActive == null) isActive = true;
        if (isDeleted == null) isDeleted = false;
        if (reminder24hSent == null) reminder24hSent = false;
        if (reminder1hSent == null) reminder1hSent = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}