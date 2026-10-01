package com.example.AlumniManagementSytem.ServiceImpl;

import com.example.AlumniManagementSytem.Model.AuditLog;
import com.example.AlumniManagementSytem.Repository.AuditLogRepository;
import com.example.AlumniManagementSytem.Service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void log(Long actorId, String actorEmail, String action,
                    Long targetId, String targetEmail, String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actorId(actorId)
                    .actorEmail(actorEmail)
                    .action(action)
                    .targetId(targetId)
                    .targetEmail(targetEmail)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit logged: {} by {}", action, actorEmail);
        } catch (Exception e) {
            // Never let audit logging break the main flow
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    @Override
    public void log(Long actorId, String actorEmail, String action,
                    Long targetId, String targetEmail, String details) {
        log(actorId, actorEmail, action, targetId, targetEmail, details, null);
    }

    @Override
    public List<AuditLog> getAllLogs() {
        // Latest 200 logs
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 200))
                .getContent();
    }

    @Override
    public List<AuditLog> getLogsByActor(Long actorId) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId);
    }
}