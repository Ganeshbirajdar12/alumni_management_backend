package com.example.AlumniManagementSytem.Service;

import com.example.AlumniManagementSytem.DTOs.request.EventRequest;
import com.example.AlumniManagementSytem.DTOs.response.EventResponse;
import com.example.AlumniManagementSytem.DTOs.response.RegistrationResponse;
import com.example.AlumniManagementSytem.DTOs.response.PaginatedResponse;
import com.example.AlumniManagementSytem.enums.EventCategory;

import java.util.List;

public interface EventService {

    // ==================== BROWSE ====================
    PaginatedResponse<EventResponse> getEvents(int page, int size, String search,
                                               EventCategory category,
                                               Boolean onlineOnly,
                                               boolean upcomingOnly,
                                               String currentUserEmail);

    List<EventResponse> getFeaturedEvents(String currentUserEmail);

    EventResponse getEventById(Long id, String currentUserEmail);

    // ==================== CREATE / UPDATE / DELETE ====================
    EventResponse createEvent(EventRequest request, String organizerEmail);

    EventResponse updateEvent(Long id, EventRequest request, String organizerEmail);

    void deleteEvent(Long id, String requesterEmail);

    // ==================== RSVP ====================
    RegistrationResponse rsvp(Long eventId, String userEmail);

    void cancelRsvp(Long eventId, String userEmail);

    List<RegistrationResponse> getAttendees(Long eventId, String requesterEmail);

    // ==================== MY EVENTS ====================
    List<EventResponse> getMyOrganizedEvents(String userEmail);

    List<EventResponse> getMyRsvps(String userEmail);

    // ==================== ADMIN ====================
    EventResponse toggleFeatured(Long id);

    // ==================== CHECK-IN ====================
    RegistrationResponse markAttended(Long eventId, Long userId, String organizerEmail);
}