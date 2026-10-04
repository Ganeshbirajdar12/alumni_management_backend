package com.example.AlumniManagementSytem.DTOs.response;

import com.example.AlumniManagementSytem.enums.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private EventCategory category;
    private LocalDateTime eventDate;
    private LocalDateTime endDate;
    private Boolean isOnline;
    private String location;
    private String meetingLink;
    private String coverImage;
    private Integer maxParticipants;
    private LocalDateTime registrationDeadline;
    private Boolean isFeatured;

    // Organizer info
    private Long organizerId;
    private String organizerName;
    private String organizerEmail;

    // Registration stats
    private long registeredCount;
    private boolean isFull;
    private boolean registrationOpen;

    // Current user's status
    private boolean userRegistered;
    private String userStatus; // REGISTERED, CANCELLED, ATTENDED, or null

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}