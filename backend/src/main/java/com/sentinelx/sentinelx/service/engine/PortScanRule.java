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
public class PortScanRule implements DetectionRuleHandler {

    @Override
    public String ruleKey() {
        return "PORT_SCAN";
    }

    @Override
    public Optional<DetectionResult> evaluate(SecurityEvent event,
                                              DetectionRuleEntity config,
                                              TrackerStats stats) {
        if (config == null || event.getPort() == null || stats.distinctPortsRecent() < config.getThreshold()) {
            return Optional.empty();
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("distinctPorts", stats.distinctPortsRecent());
        evidence.put("threshold", config.getThreshold());
        evidence.put("windowSeconds", config.getWindowSeconds());
        evidence.put("sourceIp", event.getSourceIp());
        evidence.put("currentPort", event.getPort());

        String explanation = "Source " + event.getSourceIp() + " contacted " + stats.distinctPortsRecent()
                + " distinct ports within " + config.getWindowSeconds()
                + "s, a pattern typical of reconnaissance/port scanning.";

        return Optional.of(new DetectionResult(ruleKey(), AttackType.PORT_SCAN,
                config.getSeverity(), 0.8, stats.distinctPortsRecent(), explanation, evidence));
    }
}
