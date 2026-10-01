package com.example.AlumniManagementSytem.Service;

import com.example.AlumniManagementSytem.Model.AuditLog;

import java.util.List;

public interface AuditService {

    // Log an action (with IP)
    void log(Long actorId, String actorEmail, String action,
             Long targetId, String targetEmail, String details, String ipAddress);

    // Log an action (without IP — convenience)
    void log(Long actorId, String actorEmail, String action,
             Long targetId, String targetEmail, String details);

    // Get all logs (latest 200)
    List<AuditLog> getAllLogs();

    // Get logs by actor
    List<AuditLog> getLogsByActor(Long actorId);
}