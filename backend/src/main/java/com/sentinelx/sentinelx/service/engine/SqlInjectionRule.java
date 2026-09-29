package com.sentinelx.sentinelx.service.engine;

import com.sentinelx.sentinelx.entity.AttackType;
import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import com.sentinelx.sentinelx.entity.SecurityEvent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SqlInjectionRule implements DetectionRuleHandler {

    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("(?i)(\\b|')\\s*(or|and)\\s+'?[\\w\\s]+'?\\s*=\\s*'?[\\w\\s]+"),
            Pattern.compile("(?i)'\\s*or\\s*'?1'?\\s*=\\s*'?1"),
            Pattern.compile("(?i)union\\s+(all\\s+)?select"),
            Pattern.compile("(?i)drop\\s+table"),
            Pattern.compile("(?i)insert\\s+into"),
            Pattern.compile("(?i);\\s*--"),
            Pattern.compile("(?i)\\bsleep\\s*\\("),
            Pattern.compile("(?i)\\bbenchmark\\s*\\("),
            Pattern.compile("(?i)waitfor\\s+delay"),
            Pattern.compile("(?i)information_schema"));

    @Override
    public String ruleKey() {
        return "SQL_INJECTION";
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
                String explanation = "Request content matched SQL injection signature '" + matched
                        + "'. The request attempted to manipulate a database query.";
                return Optional.of(new DetectionResult(ruleKey(), AttackType.SQL_INJECTION,
                        config != null ? config.getSeverity() : com.sentinelx.sentinelx.entity.Severity.HIGH,
                        0.95, 1, explanation, evidence));
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
