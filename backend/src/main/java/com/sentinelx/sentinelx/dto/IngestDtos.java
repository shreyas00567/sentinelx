package com.sentinelx.sentinelx.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IngestDtos {

    private IngestDtos() {}

    public record IngestRequest(
            String eventId,
            Instant timestamp,
            @NotBlank String sourceIp,
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
            String rawMessage) {}

    public record BatchIngestRequest(List<@Valid IngestRequest> events) {}

    public record AlertSummary(UUID id, String attackType, String severity, int riskScore, String method) {}

    public record ProcessingOutcome(
            UUID eventId,
            List<AlertSummary> alerts,
            List<UUID> incidentIds,
            Boolean aiAnomaly,
            Double anomalyScore) {}

    public record SimulatorResult(
            String scenario,
            int eventsGenerated,
            List<AlertSummary> alerts,
            List<UUID> incidentIds,
            int aiAnomalies) {}
}
