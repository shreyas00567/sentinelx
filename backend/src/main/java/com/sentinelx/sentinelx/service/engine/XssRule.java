package com.sentinelx.sentinelx.service.engine;

import com.sentinelx.sentinelx.entity.AttackType;
import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import com.sentinelx.sentinelx.entity.Severity;
import com.sentinelx.sentinelx.entity.SecurityEvent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class XssRule implements DetectionRuleHandler {

    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("(?i)<\\s*script[^>]*>"),
            Pattern.compile("(?i)<\\s*/\\s*script"),
            Pattern.compile("(?i)javascript\\s*:"),
            Pattern.compile("(?i)on\\w+\\s*=\\s*[\"']?\\s*(alert|confirm|prompt|eval|fetch)\\s*\\("),
            Pattern.compile("(?i)<\\s*iframe"),
            Pattern.compile("(?i)<\\s*svg[^>]*on\\w+\\s*="),
            Pattern.compile("(?i)document\\s*\\.\\s*cookie"));

    @Override
    public String ruleKey() {
        return "XSS";
    }

    @Override
    public Optional<DetectionResult> evaluate(SecurityEvent event,
                                              DetectionRuleEntity config,
                                              TrackerStats stats) {
        String haystack = safe(event.getEndpoint()) + " " + safe(event.getRawMessage());
        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(haystack);
            if (m.find()) {
                String matched = truncate(m.group());
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("matchedPattern", matched);
                evidence.put("field", "endpoint/raw_message");
                evidence.put("sourceIp", event.getSourceIp());
                evidence.put("endpoint", truncate(safe(event.getEndpoint())));
                evidence.put("httpMethod", event.getHttpMethod());
                evidence.put("status", event.getStatus());
                String explanation = "Request payload contains script markup or an event-handler payload ('"
                        + matched + "') consistent with Cross-Site Scripting.";
                return Optional.of(new DetectionResult(ruleKey(), AttackType.XSS,
                        config != null ? config.getSeverity() : Severity.HIGH,
                        0.9, 1, explanation, evidence));
            }
        }
        return Optional.empty();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s) {
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
