package com.example.AlumniManagementSytem.ServiceImpl;

import com.example.AlumniManagementSytem.DTOs.request.EventRequest;
import com.example.AlumniManagementSytem.DTOs.response.EventResponse;
import com.example.AlumniManagementSytem.DTOs.response.PaginatedResponse;
import com.example.AlumniManagementSytem.DTOs.response.RegistrationResponse;
import com.example.AlumniManagementSytem.Exception.BadRequestException;
import com.example.AlumniManagementSytem.Exception.ResourceNotFoundException;
import com.example.AlumniManagementSytem.Model.Event;
import com.example.AlumniManagementSytem.Model.EventRegistration;
import com.example.AlumniManagementSytem.Model.User;
import com.example.AlumniManagementSytem.Repository.EventRegistrationRepository;
import com.example.AlumniManagementSytem.Repository.EventRepository;
import com.example.AlumniManagementSytem.Repository.UserRepository;
import com.example.AlumniManagementSytem.Service.AuditService;
import com.example.AlumniManagementSytem.ServiceImpl.EmailService;
import com.example.AlumniManagementSytem.Service.EventService;
import com.example.AlumniManagementSytem.enums.EventCategory;
import com.example.AlumniManagementSytem.enums.RegistrationStatus;
import com.example.AlumniManagementSytem.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    // ==================== BROWSE ====================

    @Override
    public PaginatedResponse<EventResponse> getEvents(int page, int size, String search,
                                                      EventCategory category,
                                                      Boolean onlineOnly,
                                                      boolean upcomingOnly,
                                                      String currentUserEmail) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime now = LocalDateTime.now();

        Page<Event> eventsPage;

        if (category != null && upcomingOnly) {
            eventsPage = eventRepository.findUpcomingByCategory(category, now, pageable);
        } else if (upcomingOnly) {
            eventsPage = eventRepository.findUpcomingEvents(now, pageable);
        } else {
            eventsPage = eventRepository.findAllActive(pageable);
        }

        // Apply in-memory filters for search and onlineOnly
        List<EventResponse> filtered = eventsPage.getContent().stream()
                .filter(e -> search == null || search.isEmpty()
                        || e.getTitle().toLowerCase().contains(search.toLowerCase())
                        || (e.getLocation() != null && e.getLocation().toLowerCase().contains(search.toLowerCase())))
                .filter(e -> onlineOnly == null || onlineOnly.equals(e.getIsOnline()))
                .map(e -> mapToEventResponse(e, currentUserEmail))
                .collect(Collectors.toList());

        return PaginatedResponse.<EventResponse>builder()
                .content(filtered)
                .page(eventsPage.getNumber())
                .size(eventsPage.getSize())
                .totalElements(eventsPage.getTotalElements())
                .totalPages(eventsPage.getTotalPages())
                .first(eventsPage.isFirst())
                .last(eventsPage.isLast())
                .build();
    }

    @Override
    public List<EventResponse> getFeaturedEvents(String currentUserEmail) {
        return eventRepository.findFeaturedEvents(LocalDateTime.now(), PageRequest.of(0, 6))
                .stream()
                .map(e -> mapToEventResponse(e, currentUserEmail))
                .collect(Collectors.toList());
    }

    @Override
    public EventResponse getEventById(Long id, String currentUserEmail) {
        Event event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return mapToEventResponse(event, currentUserEmail);
    }

    // ==================== CREATE ====================

    @Override
    @Transactional
    public EventResponse createEvent(EventRequest request, String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate: only ALUMNI, ADMIN, SUPER_ADMIN can create
        if (organizer.getRole() != UserRole.ROLE_ALUMNI
                && organizer.getRole() != UserRole.ROLE_ADMIN
                && organizer.getRole() != UserRole.ROLE_SUPER_ADMIN) {
            throw new BadRequestException("Only alumni and admins can create events");
        }

        // Validate online vs location
        if (Boolean.TRUE.equals(request.getIsOnline())) {
            if (request.getMeetingLink() == null || request.getMeetingLink().isEmpty()) {
                throw new BadRequestException("Meeting link is required for online events");
            }
        } else {
            if (request.getLocation() == null || request.getLocation().isEmpty()) {
                throw new BadRequestException("Location is required for in-person events");
            }
        }

        Event event = Event.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .eventDate(request.getEventDate())
                .endDate(request.getEndDate())
                .isOnline(request.getIsOnline() != null ? request.getIsOnline() : false)
                .location(request.getLocation())
                .meetingLink(request.getMeetingLink())
                .coverImage(request.getCoverImage())
                .maxParticipants(request.getMaxParticipants())
                .registrationDeadline(request.getRegistrationDeadline())
                .organizerId(organizer.getId())
                .isFeatured(false)
                .isActive(true)
                .isDeleted(false)
                .reminder24hSent(false)
                .reminder1hSent(false)
                .build();

        event = eventRepository.save(event);
        log.info("Event created: {} by {}", event.getTitle(), organizerEmail);

        auditService.log(organizer.getId(), organizerEmail, "EVENT_CREATED",
                event.getId(), null, "Event created: " + event.getTitle());

        return mapToEventResponse(event, organizerEmail);
    }

    // ==================== UPDATE ====================

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request, String organizerEmail) {
        Event event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        User requester = userRepository.findByEmail(organizerEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only organizer OR admin can edit
        boolean isOrganizer = event.getOrganizerId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == UserRole.ROLE_ADMIN
                || requester.getRole() == UserRole.ROLE_SUPER_ADMIN;

        if (!isOrganizer && !isAdmin) {
            throw new BadRequestException("You can only edit your own events");
        }

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setEventDate(request.getEventDate());
        event.setEndDate(request.getEndDate());
        event.setIsOnline(request.getIsOnline() != null ? request.getIsOnline() : false);
        event.setLocation(request.getLocation());
        event.setMeetingLink(request.getMeetingLink());
        event.setCoverImage(request.getCoverImage());
        event.setMaxParticipants(request.getMaxParticipants());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setUpdatedAt(LocalDateTime.now());

        event = eventRepository.save(event);
        log.info("Event updated: {}", event.getTitle());

        auditService.log(requester.getId(), organizerEmail, "EVENT_UPDATED",
                event.getId(), null, "Event updated: " + event.getTitle());

        return mapToEventResponse(event, organizerEmail);
    }

    // ==================== DELETE ====================

    @Override
    @Transactional
    public void deleteEvent(Long id, String requesterEmail) {
        Event event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        User requester = userRepository.findByEmail(requesterEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isOrganizer = event.getOrganizerId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == UserRole.ROLE_ADMIN
                || requester.getRole() == UserRole.ROLE_SUPER_ADMIN;

        if (!isOrganizer && !isAdmin) {
            throw new BadRequestException("You can only delete your own events");
        }

        // Soft delete
        event.setIsDeleted(true);
        event.setIsActive(false);
        eventRepository.save(event);

        log.info("Event deleted: {}", event.getTitle());

        auditService.log(requester.getId(), requesterEmail, "EVENT_DELETED",
                event.getId(), null, "Event deleted: " + event.getTitle());
    }

    // ==================== RSVP ====================

    @Override
    @Transactional
    public RegistrationResponse rsvp(Long eventId, String userEmail) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check event is in future
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot RSVP to past events");
        }

        // Check deadline
        if (event.getRegistrationDeadline() != null
                && LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new BadRequestException("Registration deadline has passed");
        }

        // Check already registered
        var existing = registrationRepository.findByEventIdAndUserId(eventId, user.getId());
        if (existing.isPresent()) {
            EventRegistration reg = existing.get();
            if (reg.getStatus() == RegistrationStatus.REGISTERED
                    || reg.getStatus() == RegistrationStatus.ATTENDED) {
                throw new BadRequestException("You are already registered");
            }
            // Was cancelled → re-register
            reg.setStatus(RegistrationStatus.REGISTERED);
            reg.setRsvpAt(LocalDateTime.now());
            reg.setCancelledAt(null);
            registrationRepository.save(reg);
            return mapToRegistrationResponse(reg, event, user);
        }

        // Check capacity
        if (event.getMaxParticipants() != null) {
            long count = registrationRepository.countByEventIdAndStatus(
                    eventId, RegistrationStatus.REGISTERED);
            if (count >= event.getMaxParticipants()) {
                throw new BadRequestException("Event is full");
            }
        }

        // Create registration
        EventRegistration reg = EventRegistration.builder()
                .eventId(eventId)
                .userId(user.getId())
                .status(RegistrationStatus.REGISTERED)
                .rsvpAt(LocalDateTime.now())
                .build();

        reg = registrationRepository.save(reg);
        log.info("User {} RSVP'd to event {}", userEmail, event.getTitle());

        // Send confirmation email
        try {
            emailService.sendRsvpConfirmation(user.getEmail(), user.getFullName(), event);
        } catch (Exception e) {
            log.error("Failed to send RSVP confirmation: {}", e.getMessage());
        }

        return mapToRegistrationResponse(reg, event, user);
    }

    @Override
    @Transactional
    public void cancelRsvp(Long eventId, String userEmail) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EventRegistration reg = registrationRepository.findByEventIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new BadRequestException("You are not registered for this event"));

        if (reg.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BadRequestException("Already cancelled");
        }

        // Check deadline
        if (event.getRegistrationDeadline() != null
                && LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new BadRequestException("Cannot cancel after registration deadline");
        }

        // Check event hasn't passed
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot cancel past events");
        }

        reg.setStatus(RegistrationStatus.CANCELLED);
        reg.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(reg);

        log.info("User {} cancelled RSVP for {}", userEmail, event.getTitle());
    }

    @Override
    public List<RegistrationResponse> getAttendees(Long eventId, String requesterEmail) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        User requester = userRepository.findByEmail(requesterEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isOrganizer = event.getOrganizerId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == UserRole.ROLE_ADMIN
                || requester.getRole() == UserRole.ROLE_SUPER_ADMIN;

        // Must be organizer, admin, OR RSVP'd user
        boolean isRegistered = registrationRepository
                .existsByEventIdAndUserIdAndStatus(eventId, requester.getId(), RegistrationStatus.REGISTERED);

        if (!isOrganizer && !isAdmin && !isRegistered) {
            throw new BadRequestException("You must be registered to see the attendee list");
        }

        return registrationRepository.findByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED)
                .stream()
                .map(reg -> {
                    User u = userRepository.findById(reg.getUserId()).orElse(null);
                    return mapToRegistrationResponse(reg, event, u);
                })
                .collect(Collectors.toList());
    }

    // ==================== MY EVENTS ====================

    @Override
    public List<EventResponse> getMyOrganizedEvents(String userEmail) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return eventRepository.findByOrganizerId(user.getId())
                .stream()
                .map(e -> mapToEventResponse(e, userEmail))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventResponse> getMyRsvps(String userEmail) {
        User user = userRepository.findByEmail(userEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return registrationRepository.findByUserIdAndStatus(user.getId(), RegistrationStatus.REGISTERED)
                .stream()
                .map(reg -> eventRepository.findActiveById(reg.getEventId()).orElse(null))
                .filter(e -> e != null)
                .map(e -> mapToEventResponse(e, userEmail))
                .collect(Collectors.toList());
    }

    // ==================== ADMIN ====================

    @Override
    @Transactional
    public EventResponse toggleFeatured(Long id) {
        Event event = eventRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        event.setIsFeatured(!Boolean.TRUE.equals(event.getIsFeatured()));
        event = eventRepository.save(event);

        log.info("Event {} featured status: {}", event.getTitle(), event.getIsFeatured());
        return mapToEventResponse(event, null);
    }

    // ==================== CHECK-IN ====================

    @Override
    @Transactional
    public RegistrationResponse markAttended(Long eventId, Long userId, String organizerEmail) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        User organizer = userRepository.findByEmail(organizerEmail.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isOrganizer = event.getOrganizerId().equals(organizer.getId());
        boolean isAdmin = organizer.getRole() == UserRole.ROLE_ADMIN
                || organizer.getRole() == UserRole.ROLE_SUPER_ADMIN;

        if (!isOrganizer && !isAdmin) {
            throw new BadRequestException("Only the organizer can mark attendance");
        }

        EventRegistration reg = registrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BadRequestException("User is not registered for this event"));

        reg.setStatus(RegistrationStatus.ATTENDED);
        reg.setAttendedAt(LocalDateTime.now());
        registrationRepository.save(reg);

        User user = userRepository.findById(userId).orElse(null);

        log.info("User {} marked as ATTENDED for event {}", userId, event.getTitle());
        return mapToRegistrationResponse(reg, event, user);
    }

    // ==================== HELPERS ====================

    private EventResponse mapToEventResponse(Event event, String currentUserEmail) {
        User organizer = userRepository.findById(event.getOrganizerId()).orElse(null);

        long registeredCount = registrationRepository.countByEventIdAndStatus(
                event.getId(), RegistrationStatus.REGISTERED);

        boolean isFull = event.getMaxParticipants() != null
                && registeredCount >= event.getMaxParticipants();

        boolean registrationOpen = event.getEventDate().isAfter(LocalDateTime.now())
                && !isFull
                && (event.getRegistrationDeadline() == null
                || LocalDateTime.now().isBefore(event.getRegistrationDeadline()));

        boolean userRegistered = false;
        String userStatus = null;

        if (currentUserEmail != null) {
            User currentUser = userRepository.findByEmail(currentUserEmail.toLowerCase()).orElse(null);
            if (currentUser != null) {
                var reg = registrationRepository.findByEventIdAndUserId(event.getId(), currentUser.getId());
                if (reg.isPresent()) {
                    userRegistered = reg.get().getStatus() == RegistrationStatus.REGISTERED
                            || reg.get().getStatus() == RegistrationStatus.ATTENDED;
                    userStatus = reg.get().getStatus().name();
                }
            }
        }

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .endDate(event.getEndDate())
                .isOnline(event.getIsOnline())
                .location(event.getLocation())
                .meetingLink(event.getMeetingLink())
                .coverImage(event.getCoverImage())
                .maxParticipants(event.getMaxParticipants())
                .registrationDeadline(event.getRegistrationDeadline())
                .isFeatured(event.getIsFeatured())
                .organizerId(event.getOrganizerId())
                .organizerName(organizer != null ? organizer.getFullName() : null)
                .organizerEmail(organizer != null ? organizer.getEmail() : null)
                .registeredCount(registeredCount)
                .isFull(isFull)
                .registrationOpen(registrationOpen)
                .userRegistered(userRegistered)
                .userStatus(userStatus)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private RegistrationResponse mapToRegistrationResponse(EventRegistration reg, Event event, User user) {
        return RegistrationResponse.builder()
                .id(reg.getId())
                .eventId(reg.getEventId())
                .eventTitle(event != null ? event.getTitle() : null)
                .userId(reg.getUserId())
                .userFullName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .status(reg.getStatus().name())
                .rsvpAt(reg.getRsvpAt())
                .cancelledAt(reg.getCancelledAt())
                .attendedAt(reg.getAttendedAt())
                .build();
    }
}