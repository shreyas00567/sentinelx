package com.sentinelx.sentinelx.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "attack_type", length = 32, nullable = false)
    private AttackType attackType;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_method", length = 16, nullable = false)
    private DetectionMethod detectionMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Severity severity;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(length = 256)
    private String target;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AlertStatus status = AlertStatus.NEW;

    @Column(name = "anomaly_score")
    private Double anomalyScore;

    @Column(name = "event_id")
    private UUID eventId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public AttackType getAttackType() { return attackType; }
    public void setAttackType(AttackType attackType) { this.attackType = attackType; }
    public DetectionMethod getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(DetectionMethod detectionMethod) { this.detectionMethod = detectionMethod; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public Double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(Double anomalyScore) { this.anomalyScore = anomalyScore; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
}
