package com.sentinelx.sentinelx.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ViewDtos {

    private ViewDtos() {}

    public record AlertDto(
            UUID id,
            Instant createdAt,
            String attackType,
            String detectionMethod,
            String severity,
            int riskScore,
            double confidence,
            String sourceIp,
            String target,
            Object evidence,
            String explanation,
            String status,
            Double anomalyScore,
            UUID eventId) {}

    public record IncidentDto(
            UUID id,
            String title,
            String attackType,
            String severity,
            int riskScore,
            String sourceIp,
            String target,
            String detectionMethod,
            Double aiScore,
            Object evidence,
            String explanation,
            List<String> recommendedActions,
            String status,
            String assignedTo,
            String notes,
            int alertCount,
            Instant createdAt,
            Instant updatedAt,
            Instant resolvedAt) {}

    public record LogDto(
            UUID id,
            String eventId,
            Instant timestamp,
            String sourceIp,
            String destinationIp,
            String username,
            String source,
            String endpoint,
            String httpMethod,
            Integer status,
            String protocol,
            Integer port,
            Long bytes,
            Integer failedAttempts,
            String eventType,
            String rawMessage,
            String attackType,
            String severity,
            Double anomalyScore,
            boolean processed) {}

    public record UpdateAlertRequest(String status) {}

    public record UpdateIncidentRequest(String status, String assignedTo, String notesAppend, String title) {}
}
