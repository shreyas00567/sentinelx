package com.sentinelx.sentinelx.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "destination_ip", length = 64)
    private String destinationIp;

    @Column(length = 64)
    private String username;

    @Column(length = 64)
    private String source;

    @Column(length = 512)
    private String endpoint;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    private Integer status;

    @Column(length = 16)
    private String protocol;

    private Integer port;

    @Column(name = "bytes_sent")
    private Long bytes;

    @Column(name = "failed_attempts")
    private Integer failedAttempts;

    @Column(name = "event_type", length = 64)
    private String eventType;

    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "attack_type", length = 32)
    private String attackType;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Severity severity;

    @Column(name = "anomaly_score")
    private Double anomalyScore;

    @Column(nullable = false)
    private boolean processed = false;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getDestinationIp() { return destinationIp; }
    public void setDestinationIp(String destinationIp) { this.destinationIp = destinationIp; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public Long getBytes() { return bytes; }
    public void setBytes(Long bytes) { this.bytes = bytes; }
    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }
    public String getAttackType() { return attackType; }
    public void setAttackType(String attackType) { this.attackType = attackType; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(Double anomalyScore) { this.anomalyScore = anomalyScore; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
}
