package com.sentinelx.sentinelx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentinelx.sentinelx.config.RiskProperties;
import com.sentinelx.sentinelx.dto.IngestDtos;
import com.sentinelx.sentinelx.entity.Alert;
import com.sentinelx.sentinelx.entity.AttackType;
import com.sentinelx.sentinelx.entity.DetectionMethod;
import com.sentinelx.sentinelx.entity.Incident;
import com.sentinelx.sentinelx.entity.Severity;
import com.sentinelx.sentinelx.repository.AlertRepository;
import com.sentinelx.sentinelx.repository.SecurityEventRepository;
import com.sentinelx.sentinelx.service.engine.DetectionResult;
import com.sentinelx.sentinelx.service.engine.EventTracker;
import com.sentinelx.sentinelx.service.engine.RuleEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LogIngestionService {

    private static final Logger log = LoggerFactory.getLogger(LogIngestionService.class);

    private final SecurityEventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final RuleEngineService ruleEngine;
    private final EventTracker tracker;
    private final MlClientService mlClient;
    private final RiskScoringService riskScoring;
    private final IncidentService incidentService;
    private final ThreatIntelService threatIntelService;
    private final RiskProperties riskProps;

    public LogIngestionService(SecurityEventRepository eventRepository,
                               AlertRepository alertRepository,
                               RuleEngineService ruleEngine,
                               EventTracker tracker,
                               MlClientService mlClient,
                               RiskScoringService riskScoring,
                               IncidentService incidentService,
                               ThreatIntelService threatIntelService,
                               RiskProperties riskProps) {
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.ruleEngine = ruleEngine;
        this.tracker = tracker;
        this.mlClient = mlClient;
        this.riskScoring = riskScoring;
        this.incidentService = incidentService;
        this.threatIntelService = threatIntelService;
        this.riskProps = riskProps;
    }

    public IngestDtos.ProcessingOutcome ingest(IngestDtos.IngestRequest req) {
        com.sentinelx.sentinelx.entity.SecurityEvent event = normalize(req);

        tracker.record(event);
        var stats = tracker.stats(safe(event.getSourceIp(), "unknown"));

        MlPrediction prediction = null;
        try {
            Map<String, Object> features = mlClient.buildFeatures(
                    event.getTimestamp(),
                    nz(event.getFailedAttempts()) + Math.max(0, stats.failuresRecent() - nz(event.getFailedAttempts())),
                    nzl(event.getBytes()),
                    stats.requestsRecent(),
                    stats.distinctEndpointsHour(),
                    sessionDurationMin(stats));
            prediction = mlClient.predict(features);
        } catch (Exception ex) {
            log.debug("Feature extraction failed: {}", ex.getMessage());
        }
        if (prediction != null) {
            event.setAnomalyScore(prediction.anomalyScore());
        }
        event.setProcessed(true);
        eventRepository.save(event);

        List<DetectionResult> detections = ruleEngine.evaluate(event);

        List<IngestDtos.AlertSummary> alertSummaries = new ArrayList<>();
        List<UUID> incidentIds = new ArrayList<>();

        if (!detections.isEmpty()) {
            for (DetectionResult d : detections) {
                boolean hybrid = prediction != null && prediction.anomaly();
                DetectionMethod method = hybrid ? DetectionMethod.HYBRID : DetectionMethod.RULE;

                boolean intelBad = isIntelMalicious(event.getSourceIp());
                int risk = riskScoring.score(d.getSeverity(), d.getConfidence(),
                        prediction != null ? prediction.anomalyScore() : 0d,
                        d.getRepeatCount(), intelBad);
                Severity band = riskScoring.bandOf(risk);

                Alert alert = new Alert();
                alert.setCreatedAt(Instant.now());
                alert.setAttackType(d.getAttackType());
                alert.setDetectionMethod(method);
                alert.setSeverity(band);
                alert.setRiskScore(risk);
                alert.setConfidence(d.getConfidence());
                alert.setSourceIp(event.getSourceIp());
                alert.setTarget(buildTarget(event));
                alert.setEvidence(JsonUtil.write(d.getEvidence()));
                alert.setExplanation(d.getExplanation());
                alert.setStatus(com.sentinelx.sentinelx.entity.AlertStatus.NEW);
                alert.setAnomalyScore(prediction != null ? prediction.anomalyScore() : null);
                alert.setEventId(event.getId());
                alert = alertRepository.save(alert);
                alertSummaries.add(toSummary(alert));

                Incident incident = incidentService.createOrUpdate(alert, event, prediction, intelBad, band);
                if (incident != null && !incidentIds.contains(incident.getId())) {
                    incidentIds.add(incident.getId());
                }
            }
        } else if (prediction != null && prediction.anomaly()) {
            double anomalyScore = prediction.anomalyScore();
            int preliminary = anomalyScore >= 0.8 ? 60 : anomalyScore >= 0.6 ? 40 : 25;
            Severity baseSev = riskScoring.bandOf(preliminary);
            int risk = riskScoring.score(baseSev, 0.5, anomalyScore, 1, false);
            Severity band = riskScoring.bandOf(risk);

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("model", "autoencoder");
            evidence.put("modelVersion", prediction.modelVersion());
            evidence.put("rawError", prediction.rawError());
            evidence.put("threshold", prediction.threshold());
            evidence.put("anomalyScore", anomalyScore);
            evidence.put("eventType", event.getEventType());
            evidence.put("sourceIp", event.getSourceIp());

            Alert alert = new Alert();
            alert.setCreatedAt(Instant.now());
            alert.setAttackType(AttackType.ANOMALY);
            alert.setDetectionMethod(DetectionMethod.AI);
            alert.setSeverity(band);
            alert.setRiskScore(risk);
            alert.setConfidence(anomalyScore);
            alert.setSourceIp(event.getSourceIp());
            alert.setTarget(buildTarget(event));
            alert.setEvidence(JsonUtil.write(evidence));
            alert.setExplanation("Autoencoder reconstruction error (" + prediction.rawError()
                    + ") exceeded the learned normal-behavior threshold (" + prediction.threshold()
                    + "). The behavioral pattern of this event was not seen during training.");
            alert.setStatus(com.sentinelx.sentinelx.entity.AlertStatus.NEW);
            alert.setAnomalyScore(anomalyScore);
            alert.setEventId(event.getId());
            alert = alertRepository.save(alert);
            alertSummaries.add(toSummary(alert));

            Incident incident = incidentService.createOrUpdate(alert, event, prediction, false, band);
            if (incident != null) {
                incidentIds.add(incident.getId());
            }
        }

        return new IngestDtos.ProcessingOutcome(
                event.getId(), alertSummaries, incidentIds,
                prediction != null && prediction.anomaly(),
                prediction != null ? prediction.anomalyScore() : null);
    }

    private com.sentinelx.sentinelx.entity.SecurityEvent normalize(IngestDtos.IngestRequest req) {
        com.sentinelx.sentinelx.entity.SecurityEvent e = new com.sentinelx.sentinelx.entity.SecurityEvent();
        e.setEventId(req.eventId() == null ? "EVT-" + UUID.randomUUID().toString().substring(0, 8) : req.eventId());
        e.setTimestamp(req.timestamp() == null ? Instant.now() : req.timestamp());
        e.setSourceIp(truncate(req.sourceIp(), 64));
        e.setDestinationIp(truncate(req.destinationIp(), 64));
        e.setUsername(truncate(req.username(), 64));
        e.setSource(truncate(req.source(), 64));
        e.setEndpoint(truncate(req.endpoint(), 500));
        e.setHttpMethod(req.httpMethod() == null ? null : req.httpMethod().toUpperCase().trim());
        e.setStatus(req.status());
        e.setProtocol(truncate(req.protocol(), 16));
        e.setPort(req.port());
        e.setBytes(req.bytes());
        e.setFailedAttempts(req.failedAttempts());
        e.setRawMessage(truncate(req.rawMessage(), 3900));
        e.setEventType(inferEventType(req));
        return e;
    }

    private String inferEventType(IngestDtos.IngestRequest req) {
        String declared = req.eventType() == null ? "" : req.eventType().trim().toUpperCase();
        if (!declared.isEmpty()) {
            return declared;
        }
        Integer status = req.status();
        String path = safe(req.endpoint(), "").toLowerCase();
        if ((status != null && (status == 401 || status == 403)) && path.contains("login")) {
            return "LOGIN_FAILED";
        }
        if (status != null && status < 400 && path.contains("login")) {
            return "LOGIN_SUCCESS";
        }
        if (status != null && status >= 400) {
            return "HTTP_ERROR";
        }
        return "HTTP_REQUEST";
    }

    private boolean isIntelMalicious(String ip) {
        try {
            if (ip == null || ip.isBlank() || ip.startsWith("10.") || ip.startsWith("192.168.")) {
                return false;
            }
            return threatIntelService.lookup(ip).malicious();
        } catch (Exception ex) {
            return false;
        }
    }

    private double sessionDurationMin(com.sentinelx.sentinelx.service.engine.TrackerStats stats) {
        return Math.min(240, stats.distinctEndpointsHour() * 1.5);
    }

    private String buildTarget(com.sentinelx.sentinelx.entity.SecurityEvent e) {
        String t = safe(e.getEndpoint(), "");
        if (!t.isEmpty()) {
            return truncate(t, 200);
        }
        return safe(e.getDestinationIp(), "n/a") + ":" + (e.getPort() == null ? "?" : e.getPort());
    }

    private IngestDtos.AlertSummary toSummary(Alert a) {
        return new IngestDtos.AlertSummary(a.getId(), a.getAttackType().name(),
                a.getSeverity().name(), a.getRiskScore(), a.getDetectionMethod().name());
    }

    private static String safe(String s, String def) {
        return s == null || s.isBlank() ? def : s;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static long nzl(Long v) {
        return v == null ? 0L : v;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
