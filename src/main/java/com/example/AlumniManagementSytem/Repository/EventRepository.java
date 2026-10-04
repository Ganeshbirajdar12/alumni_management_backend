package com.example.AlumniManagementSytem.Repository;

import com.example.AlumniManagementSytem.Model.Event;
import com.example.AlumniManagementSytem.enums.EventCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Find active, non-deleted event by id
    @Query("SELECT e FROM Event e WHERE e.id = :id AND e.isDeleted = false")
    java.util.Optional<Event> findActiveById(@Param("id") Long id);

    // List all active upcoming events
    @Query("SELECT e FROM Event e WHERE e.isDeleted = false AND e.isActive = true " +
            "AND e.eventDate >= :now ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingEvents(@Param("now") LocalDateTime now, Pageable pageable);

    // List all active events (including past)
    @Query("SELECT e FROM Event e WHERE e.isDeleted = false AND e.isActive = true " +
            "ORDER BY e.eventDate DESC")
    Page<Event> findAllActive(Pageable pageable);

    // Filter by category
    @Query("SELECT e FROM Event e WHERE e.isDeleted = false AND e.isActive = true " +
            "AND e.category = :category AND e.eventDate >= :now ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingByCategory(@Param("category") EventCategory category,
                                       @Param("now") LocalDateTime now,
                                       Pageable pageable);

    // My organized events
    @Query("SELECT e FROM Event e WHERE e.organizerId = :userId AND e.isDeleted = false " +
            "ORDER BY e.eventDate DESC")
    List<Event> findByOrganizerId(@Param("userId") Long userId);

    // Events needing 24h reminder
    @Query("SELECT e FROM Event e WHERE e.isDeleted = false AND e.isActive = true " +
            "AND e.reminder24hSent = false " +
            "AND e.eventDate BETWEEN :from AND :to")
    List<Event> findEventsFor24hReminder(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    // Events needing 1h reminder
    @Query("SELECT e FROM Event e WHERE e.isDeleted = false AND e.isActive = true " +
            "AND e.reminder1hSent = false " +
            "AND e.eventDate BETWEEN :from AND :to")
    List<Event> findEventsFor1hReminder(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    // Featured events
    @Query("SELECT e FROM Event e WHERE e.isDeleted = false AND e.isActive = true " +
            "AND e.isFeatured = true AND e.eventDate >= :now ORDER BY e.eventDate ASC")
    List<Event> findFeaturedEvents(@Param("now") LocalDateTime now, Pageable pageable);
}