package com.sentinelx.sentinelx.service;

import com.sentinelx.sentinelx.entity.AuditLog;
import com.sentinelx.sentinelx.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String username, String action, String details, HttpServletRequest request) {
        AuditLog entry = new AuditLog();
        entry.setTimestamp(Instant.now());
        entry.setUsername(username);
        entry.setAction(action);
        entry.setDetails(details == null ? "" : details);
        entry.setIpAddress(clientIp(request));
        repository.save(entry);
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
