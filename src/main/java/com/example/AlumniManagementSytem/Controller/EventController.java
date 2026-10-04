package com.example.AlumniManagementSytem.Controller;

import com.example.AlumniManagementSytem.DTOs.request.EventRequest;
import com.example.AlumniManagementSytem.DTOs.response.EventResponse;
import com.example.AlumniManagementSytem.DTOs.response.PaginatedResponse;
import com.example.AlumniManagementSytem.DTOs.response.RegistrationResponse;
import com.example.AlumniManagementSytem.Service.EventService;
import com.example.AlumniManagementSytem.enums.EventCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ==================== BROWSE ====================

    @GetMapping
    public ResponseEntity<PaginatedResponse<EventResponse>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) Boolean onlineOnly,
            @RequestParam(defaultValue = "true") boolean upcomingOnly,
            Authentication auth) {

        String email = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(eventService.getEvents(
                page, size, search, category, onlineOnly, upcomingOnly, email));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<EventResponse>> getFeatured(Authentication auth) {
        String email = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(eventService.getFeaturedEvents(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id, Authentication auth) {
        String email = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(eventService.getEventById(id, email));
    }

    // ==================== CREATE / UPDATE / DELETE ====================

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request,
            Authentication auth) {
        return ResponseEntity.ok(eventService.createEvent(request, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request,
            Authentication auth) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEvent(
            @PathVariable Long id,
            Authentication auth) {
        eventService.deleteEvent(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Event deleted"));
    }

    // ==================== RSVP ====================

    @PostMapping("/{id}/rsvp")
    public ResponseEntity<RegistrationResponse> rsvp(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(eventService.rsvp(id, auth.getName()));
    }

    @DeleteMapping("/{id}/rsvp")
    public ResponseEntity<Map<String, String>> cancelRsvp(
            @PathVariable Long id,
            Authentication auth) {
        eventService.cancelRsvp(id, auth.getName());
        return ResponseEntity.ok(Map.of("message", "RSVP cancelled"));
    }

    @GetMapping("/{id}/attendees")
    public ResponseEntity<List<RegistrationResponse>> getAttendees(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(eventService.getAttendees(id, auth.getName()));
    }

    // ==================== MY EVENTS ====================

    @GetMapping("/my-organized")
    public ResponseEntity<List<EventResponse>> getMyOrganized(Authentication auth) {
        return ResponseEntity.ok(eventService.getMyOrganizedEvents(auth.getName()));
    }

    @GetMapping("/my-rsvps")
    public ResponseEntity<List<EventResponse>> getMyRsvps(Authentication auth) {
        return ResponseEntity.ok(eventService.getMyRsvps(auth.getName()));
    }

    // ==================== ADMIN ====================

    @PostMapping("/{id}/feature")
    public ResponseEntity<EventResponse> toggleFeatured(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.toggleFeatured(id));
    }

    // ==================== CHECK-IN ====================

    @PostMapping("/{id}/check-in/{userId}")
    public ResponseEntity<RegistrationResponse> markAttended(
            @PathVariable Long id,
            @PathVariable Long userId,
            Authentication auth) {
        return ResponseEntity.ok(eventService.markAttended(id, userId, auth.getName()));
    }
}