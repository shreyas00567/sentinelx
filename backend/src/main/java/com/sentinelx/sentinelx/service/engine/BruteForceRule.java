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
public class BruteForceRule implements DetectionRuleHandler {

    @Override
    public String ruleKey() {
        return "BRUTE_FORCE";
    }

    @Override
    public Optional<DetectionResult> evaluate(SecurityEvent event,
                                              DetectionRuleEntity config,
                                              TrackerStats stats) {
        if (config == null || stats.failuresRecent() < config.getThreshold()) {
            return Optional.empty();
        }
        int threshold = config.getThreshold();
        int failures = stats.failuresRecent();
        double confidence = Math.min(0.99, 0.5 + failures / (threshold * 2.0));

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("failedLogins", failures);
        evidence.put("threshold", threshold);
        evidence.put("windowSeconds", config.getWindowSeconds());
        evidence.put("sourceIp", event.getSourceIp());
        evidence.put("usernamesTried", stats.recentUsernames());
        evidence.put("lastEndpoint", event.getEndpoint());

        String explanation = failures + " failed login attempts from " + event.getSourceIp()
                + " within the last " + config.getWindowSeconds()
                + "s (threshold " + threshold + "), indicating a brute-force password attack.";

        return Optional.of(new DetectionResult(ruleKey(), AttackType.BRUTE_FORCE,
                config.getSeverity(), confidence, failures, explanation, evidence));
    }
}
