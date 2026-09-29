package com.sentinelx.sentinelx.service.engine;

import java.util.LinkedHashMap;
import java.util.Map;

public class DetectionResult {

    private final String ruleKey;
    private final com.sentinelx.sentinelx.entity.AttackType attackType;
    private final com.sentinelx.sentinelx.entity.Severity severity;
    private final double confidence;
    private final int repeatCount;
    private final String explanation;
    private final Map<String, Object> evidence;

    public DetectionResult(String ruleKey,
                           com.sentinelx.sentinelx.entity.AttackType attackType,
                           com.sentinelx.sentinelx.entity.Severity severity,
                           double confidence,
                           int repeatCount,
                           String explanation,
                           Map<String, Object> evidence) {
        this.ruleKey = ruleKey;
        this.attackType = attackType;
        this.severity = severity;
        this.confidence = confidence;
        this.repeatCount = repeatCount;
        this.explanation = explanation;
        this.evidence = new LinkedHashMap<>(evidence);
    }

    public String getRuleKey() { return ruleKey; }
    public com.sentinelx.sentinelx.entity.AttackType getAttackType() { return attackType; }
    public com.sentinelx.sentinelx.entity.Severity getSeverity() { return severity; }
    public double getConfidence() { return confidence; }
    public int getRepeatCount() { return repeatCount; }
    public String getExplanation() { return explanation; }
    public Map<String, Object> getEvidence() { return evidence; }
}
