package com.example.AlumniManagementSytem.DTOs.request;

import com.example.AlumniManagementSytem.enums.EventCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull(message = "Category is required")
    private EventCategory category;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    private LocalDateTime endDate;

    private Boolean isOnline = false;

    private String location;

    private String meetingLink;

    private String coverImage;

    @Min(value = 1, message = "Max participants must be at least 1")
    private Integer maxParticipants;

    private LocalDateTime registrationDeadline;
}