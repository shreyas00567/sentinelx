package com.sentinelx.sentinelx.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "detection_rules")
public class DetectionRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_key", nullable = false, unique = true, length = 32)
    private String ruleKey;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 256)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int threshold;

    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Severity severity;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleKey() { return ruleKey; }
    public void setRuleKey(String ruleKey) { this.ruleKey = ruleKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }
    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
}
