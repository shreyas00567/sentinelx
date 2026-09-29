package com.sentinelx.sentinelx;

import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import com.sentinelx.sentinelx.entity.SecurityEvent;
import com.sentinelx.sentinelx.service.engine.SqlInjectionRule;
import com.sentinelx.sentinelx.service.engine.TrackerStats;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlInjectionRuleTest {

    private final SqlInjectionRule rule = new SqlInjectionRule();
    private final DetectionRuleEntity cfg = new DetectionRuleEntity();

    private SecurityEvent event(String endpoint, String raw) {
        SecurityEvent e = new SecurityEvent();
        e.setSourceIp("1.2.3.4");
        e.setEndpoint(endpoint);
        e.setRawMessage(raw);
        e.setTimestamp(Instant.now());
        return e;
    }

    @Test
    void detectsClassicOrTautology() {
        assertTrue(rule.evaluate(event("/products?id=105 OR 1=1", ""),
                cfg, TrackerStats.empty("1.2.3.4")).isPresent());
        assertTrue(rule.evaluate(event("/login", "user=' OR '1'='1"),
                cfg, TrackerStats.empty("1.2.3.4")).isPresent());
    }

    @Test
    void detectsUnionSelect() {
        assertTrue(rule.evaluate(event("/search?q=a UNION SELECT username,password FROM users--", ""),
                cfg, TrackerStats.empty("1.2.3.4")).isPresent());
    }

    @Test
    void detectsTimeBasedPayload() {
        assertTrue(rule.evaluate(event("/item/1", "'; WAITFOR DELAY '0:0:5'--"),
                cfg, TrackerStats.empty("1.2.3.4")).isPresent());
    }

    @Test
    void ignoresNormalRequests() {
        assertFalse(rule.evaluate(event("/products?id=105", "normal page view"),
                cfg, TrackerStats.empty("1.2.3.4")).isPresent());
        assertFalse(rule.evaluate(event("/search?q=union+station+hotel", ""),
                cfg, TrackerStats.empty("1.2.3.4")).isPresent());
        assertFalse(rule.evaluate(event("/api/orders", "order and payment details"),
                cfg, TrackerStats.empty("1.2.3.4")).isPresent());
    }
}
