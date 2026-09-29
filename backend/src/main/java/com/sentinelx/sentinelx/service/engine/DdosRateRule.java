package com.sentinelx.sentinelx.service.engine;

import com.sentinelx.sentinelx.entity.AttackType;
import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import com.sentinelx.sentinelx.entity.Severity;
import com.sentinelx.sentinelx.entity.SecurityEvent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class DdosRateRule implements DetectionRuleHandler {

    @Override
    public String ruleKey() {
        return "DDOS";
    }

    @Override
    public Optional<DetectionResult> evaluate(SecurityEvent event,
                                              DetectionRuleEntity config,
                                              TrackerStats stats) {
        if (config == null || stats.requestsRecent() < config.getThreshold()) {
            return Optional.empty();
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("requestsLast60s", stats.requestsRecent());
        evidence.put("threshold", config.getThreshold());
        evidence.put("sourceIp", event.getSourceIp());
        evidence.put("targetEndpoint", event.getEndpoint());

        String explanation = "Source " + event.getSourceIp() + " issued " + stats.requestsRecent()
                + " requests in the last 60 seconds (threshold " + config.getThreshold()
                + "), consistent with a high-rate denial-of-service pattern.";

        return Optional.of(new DetectionResult(ruleKey(), AttackType.DDOS,
                config.getSeverity(), 0.85, stats.requestsRecent(), explanation, evidence));
    }
}
