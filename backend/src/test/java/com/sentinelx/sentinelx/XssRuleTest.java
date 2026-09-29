package com.sentinelx.sentinelx;

import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import com.sentinelx.sentinelx.entity.SecurityEvent;
import com.sentinelx.sentinelx.service.engine.TrackerStats;
import com.sentinelx.sentinelx.service.engine.XssRule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XssRuleTest {

    private final XssRule rule = new XssRule();
    private final DetectionRuleEntity cfg = new DetectionRuleEntity();

    private SecurityEvent event(String endpoint, String raw) {
        SecurityEvent e = new SecurityEvent();
        e.setSourceIp("5.6.7.8");
        e.setEndpoint(endpoint);
        e.setRawMessage(raw);
        e.setTimestamp(Instant.now());
        return e;
    }

    @Test
    void detectsScriptTag() {
        Optional<?> r = rule.evaluate(event("/comments", "POST body=<script>alert(1)</script>"),
                cfg, TrackerStats.empty("5.6.7.8"));
        assertTrue(r.isPresent());
    }

    @Test
    void detectsEventHandler() {
        assertTrue(rule.evaluate(event("/profile", "<img src=x onerror=alert('xss')>"),
                cfg, TrackerStats.empty("5.6.7.8")).isPresent());
    }

    @Test
    void detectsJavascriptUri() {
        assertTrue(rule.evaluate(event("/link?url=javascript:void(0)", ""),
                cfg, TrackerStats.empty("5.6.7.8")).isPresent());
    }

    @Test
    void ignoresNormalText() {
        assertFalse(rule.evaluate(event("/comments", "Nice article!"),
                cfg, TrackerStats.empty("5.6.7.8")).isPresent());
        assertFalse(rule.evaluate(event("/search?q=script+writing+tips", ""),
                cfg, TrackerStats.empty("5.6.7.8")).isPresent());
        assertFalse(rule.evaluate(event("/about", "We build javascript applications"),
                cfg, TrackerStats.empty("5.6.7.8")).isPresent());
    }
}
