package com.example.AlumniManagementSytem.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String status; // REGISTERED, CANCELLED, ATTENDED
    private LocalDateTime rsvpAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime attendedAt;
}