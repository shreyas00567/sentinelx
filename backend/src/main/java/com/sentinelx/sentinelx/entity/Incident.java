package com.sentinelx.sentinelx.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 256)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "attack_type", length = 32, nullable = false)
    private AttackType attackType;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Severity severity;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(length = 256)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_method", length = 16, nullable = false)
    private DetectionMethod detectionMethod;

    @Column(name = "ai_score")
    private Double aiScore;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "assigned_to", length = 64)
    private String assignedTo;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "alert_count", nullable = false)
    private int alertCount = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public AttackType getAttackType() { return attackType; }
    public void setAttackType(AttackType attackType) { this.attackType = attackType; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public DetectionMethod getDetectionMethod() { return detectionMethod; }
    public void setDetectionMethod(DetectionMethod detectionMethod) { this.detectionMethod = detectionMethod; }
    public Double getAiScore() { return aiScore; }
    public void setAiScore(Double aiScore) { this.aiScore = aiScore; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(String recommendedActions) { this.recommendedActions = recommendedActions; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getAlertCount() { return alertCount; }
    public void setAlertCount(int alertCount) { this.alertCount = alertCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
