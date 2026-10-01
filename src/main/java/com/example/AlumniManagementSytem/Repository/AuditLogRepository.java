package com.example.AlumniManagementSytem.Repository;

import com.example.AlumniManagementSytem.Model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Get logs by a specific actor
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    // Get logs by action type (e.g., all ADMIN_CREATED)
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

    // Get all logs, newest first (paginated)
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}