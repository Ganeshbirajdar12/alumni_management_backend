package com.example.AlumniManagementSytem.Scheduler;

import com.example.AlumniManagementSytem.Model.Event;
import com.example.AlumniManagementSytem.Model.EventRegistration;
import com.example.AlumniManagementSytem.Model.User;
import com.example.AlumniManagementSytem.Repository.EventRegistrationRepository;
import com.example.AlumniManagementSytem.Repository.EventRepository;
import com.example.AlumniManagementSytem.Repository.UserRepository;
import com.example.AlumniManagementSytem.ServiceImpl.EmailService;
import com.example.AlumniManagementSytem.enums.RegistrationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventReminderScheduler {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Runs every hour to send 24h reminders.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void send24HourReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusHours(23);
        LocalDateTime to = now.plusHours(25);

        List<Event> events = eventRepository.findEventsFor24hReminder(from, to);

        if (events.isEmpty()) return;

        log.info("Sending 24h reminders for {} event(s)", events.size());

        for (Event event : events) {
            sendRemindersForEvent(event, "24 hours");
            event.setReminder24hSent(true);
            eventRepository.save(event);
        }
    }

    /**
     * Runs every 15 minutes to send 1h reminders.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void send1HourReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusMinutes(45);
        LocalDateTime to = now.plusMinutes(75);

        List<Event> events = eventRepository.findEventsFor1hReminder(from, to);

        if (events.isEmpty()) return;

        log.info("Sending 1h reminders for {} event(s)", events.size());

        for (Event event : events) {
            sendRemindersForEvent(event, "1 hour");
            event.setReminder1hSent(true);
            eventRepository.save(event);
        }
    }

    /**
     * Send reminder emails to all registered users for an event.
     */
    private void sendRemindersForEvent(Event event, String timeLeft) {
        List<EventRegistration> registrations =
                registrationRepository.findByEventIdAndStatus(event.getId(), RegistrationStatus.REGISTERED);

        for (EventRegistration reg : registrations) {
            User user = userRepository.findById(reg.getUserId()).orElse(null);
            if (user != null) {
                try {
                    emailService.sendEventReminder(
                            user.getEmail(), user.getFullName(), event, timeLeft);
                } catch (Exception e) {
                    log.error("Failed to send reminder to {}: {}", user.getEmail(), e.getMessage());
                }
            }
        }
    }
}