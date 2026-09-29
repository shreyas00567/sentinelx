package com.sentinelx.sentinelx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentinelx.sentinelx.config.RiskProperties;
import com.sentinelx.sentinelx.dto.ViewDtos;
import com.sentinelx.sentinelx.entity.Alert;
import com.sentinelx.sentinelx.entity.AttackType;
import com.sentinelx.sentinelx.entity.Incident;
import com.sentinelx.sentinelx.entity.IncidentStatus;
import com.sentinelx.sentinelx.entity.Severity;
import com.sentinelx.sentinelx.exception.ApiException;
import com.sentinelx.sentinelx.repository.IncidentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final RiskProperties riskProps;

    public IncidentService(IncidentRepository incidentRepository, RiskProperties riskProps) {
        this.incidentRepository = incidentRepository;
        this.riskProps = riskProps;
    }

    public Incident createOrUpdate(Alert alert,
                                   com.sentinelx.sentinelx.entity.SecurityEvent event,
                                   MlPrediction prediction,
                                   boolean intelBad,
                                   Severity band) {
        if (alert.getRiskScore() < riskProps.getIncidentThreshold()) {
            return null;
        }
        Instant dedupeWindow = Instant.now().minusSeconds(riskProps.getIncidentDedupeHours() * 3600L);
        List<Incident> similar = incidentRepository.findOpenSimilar(
                alert.getSourceIp(), alert.getAttackType(),
                List.of(IncidentStatus.OPEN, IncidentStatus.INVESTIGATING), dedupeWindow);

        if (!similar.isEmpty()) {
            Incident existing = similar.get(0);
            existing.setAlertCount(existing.getAlertCount() + 1);
            if (alert.getRiskScore() > existing.getRiskScore()) {
                existing.setRiskScore(alert.getRiskScore());
                existing.setSeverity(band);
            }
            if (prediction != null && (existing.getAiScore() == null || prediction.anomalyScore() > existing.getAiScore())) {
                existing.setAiScore(prediction.anomalyScore());
            }
            existing.setEvidence(JsonUtil.write(mergeEvidence(existing, alert)));
            existing.setUpdatedAt(Instant.now());
            return incidentRepository.save(existing);
        }

        Incident inc = new Incident();
        inc.setTitle(titleFor(alert));
        inc.setAttackType(alert.getAttackType());
        inc.setSeverity(band);
        inc.setRiskScore(alert.getRiskScore());
        inc.setSourceIp(alert.getSourceIp());
        inc.setTarget(alert.getTarget());
        inc.setDetectionMethod(alert.getDetectionMethod());
        inc.setAiScore(prediction != null ? prediction.anomalyScore() : null);
        inc.setEvidence(alert.getEvidence());
        inc.setExplanation(alert.getExplanation());
        inc.setRecommendedActions(JsonUtil.write(recommendedActions(alert.getAttackType(), intelBad)));
        inc.setStatus(IncidentStatus.OPEN);
        inc.setNotes("");
        inc.setAlertCount(1);
        inc.setCreatedAt(Instant.now());
        inc.setUpdatedAt(Instant.now());
        return incidentRepository.save(inc);
    }

    public Incident update(UUID id, ViewDtos.UpdateIncidentRequest req, String actor) {
        Incident inc = incidentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Incident not found"));

        StringBuilder audit = new StringBuilder();
        if (req.status() != null && !req.status().isBlank()) {
            IncidentStatus target;
            try {
                target = IncidentStatus.valueOf(req.status().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown status: " + req.status());
            }
            if (target != inc.getStatus() && !inc.getStatus().canTransitionTo(target)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Invalid lifecycle transition " + inc.getStatus() + " -> " + target);
            }
            if (target == IncidentStatus.RESOLVED) {
                inc.setResolvedAt(Instant.now());
            } else {
                inc.setResolvedAt(null);
            }
            inc.setStatus(target);
            audit.append("status->").append(target).append("; ");
        }
        if (req.assignedTo() != null) {
            inc.setAssignedTo(req.assignedTo().isBlank() ? null : req.assignedTo().trim());
            audit.append("assigned; ");
        }
        if (req.title() != null && !req.title().isBlank()) {
            inc.setTitle(req.title().trim());
        }
        if (req.notesAppend() != null && !req.notesAppend().isBlank()) {
            String stamp = actor == null ? "system" : actor;
            String note = "[" + Instant.now() + " | " + stamp + "] " + req.notesAppend().trim();
            inc.setNotes(inc.getNotes() == null || inc.getNotes().isEmpty()
                    ? note : inc.getNotes() + "\n" + note);
            audit.append("note added");
        }
        inc.setUpdatedAt(Instant.now());
        return incidentRepository.save(inc);
    }

    public ViewDtos.IncidentDto toDto(Incident i) {
        JsonNode actions = JsonUtil.read(i.getRecommendedActions() == null ? "[]" : i.getRecommendedActions());
        List<String> actionList = new ArrayList<>();
        actions.forEach(n -> actionList.add(n.asText()));
        return new ViewDtos.IncidentDto(
                i.getId(), i.getTitle(), i.getAttackType().name(), i.getSeverity().name(),
                i.getRiskScore(), i.getSourceIp(), i.getTarget(), i.getDetectionMethod().name(),
                i.getAiScore(), safeParse(i.getEvidence()), i.getExplanation(), actionList,
                i.getStatus().name(), i.getAssignedTo(), i.getNotes(), i.getAlertCount(),
                i.getCreatedAt(), i.getUpdatedAt(), i.getResolvedAt());
    }

    public static Map<String, Object> mergeEvidence(Incident existing, Alert alert) {
        Map<String, Object> root = new LinkedHashMap<>();
        JsonNode prev = JsonUtil.read(existing.getEvidence() == null ? "{}" : existing.getEvidence());
        if (prev.isObject()) {
            prev.fields().forEachRemaining(e -> root.put(e.getKey(), e.getValue()));
        } else {
            root.put("history", prev.toString());
        }
        root.put("latestAlertId", alert.getId().toString());
        root.put("latestRisk", alert.getRiskScore());
        root.put("lastSeen", Instant.now().toString());
        return root;
    }

    private static List<String> recommendedActions(AttackType type, boolean intelBad) {
        List<String> actions = new ArrayList<>();
        switch (type) {
            case BRUTE_FORCE -> {
                actions.add("Block or rate-limit source IP at the firewall/WAF");
                actions.add("Enforce MFA and account lockout for targeted accounts");
                actions.add("Reset credentials for attempted usernames");
            }
            case SQL_INJECTION -> {
                actions.add("Verify parameterized queries on the targeted endpoint");
                actions.add("Review WAF logs and block offending payload signatures");
                actions.add("Audit database accounts for unexpected activity");
            }
            case XSS -> {
                actions.add("Apply output encoding / CSP headers on the affected page");
                actions.add("Search application logs for session hijack indicators");
                actions.add("Sanitize user input fields accepting HTML");
            }
            case DDOS -> {
                actions.add("Enable rate limiting / traffic scrubbing");
                actions.add("Contact upstream ISP or CDN protection");
                actions.add("Scale protected service horizontally");
            }
            case PORT_SCAN -> {
                actions.add("Close unnecessary exposed ports");
                actions.add("Block scanning IP at network perimeter");
                actions.add("Verify host firewall alerts");
            }
            case ANOMALY -> {
                actions.add("Interview/verify the involved user account");
                actions.add("Review recent access history for the account");
                actions.add("Rotate credentials if behavior cannot be explained");
            }
        }
        if (intelBad) {
            actions.add("Threat intel flagged this source as malicious - consider immediate blocklist entry");
        }
        return actions;
    }

    private static String titleFor(Alert a) {
        String ip = a.getSourceIp() == null ? "unknown" : a.getSourceIp();
        return switch (a.getAttackType()) {
            case BRUTE_FORCE -> "Brute-force attack from " + ip;
            case SQL_INJECTION -> "SQL injection attempt from " + ip;
            case XSS -> "Cross-site scripting attempt from " + ip;
            case DDOS -> "High-rate DoS traffic from " + ip;
            case PORT_SCAN -> "Port scan/reconnaissance from " + ip;
            case ANOMALY -> "AI-detected behavioral anomaly (" + ip + ")";
        };
    }

    private static Object safeParse(String json) {
        JsonNode n = JsonUtil.read(json == null ? "{}" : json);
        return n.isMissingNode() ? json : JsonUtil.convert(n, Object.class);
    }
}
