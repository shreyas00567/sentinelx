package com.sentinelx.sentinelx;

import com.sentinelx.sentinelx.config.RiskProperties;
import com.sentinelx.sentinelx.entity.Severity;
import com.sentinelx.sentinelx.service.RiskScoringService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskScoringServiceTest {

    private final RiskScoringService svc = new RiskScoringService(new RiskProperties());

    @Test
    void lowSeverityLowSignalsStaysLow() {
        int s = svc.score(Severity.LOW, 0.2, 0.0, 1, false);
        assertTrue(s >= 0 && s < 25, "expected LOW band, got " + s);
    }

    @Test
    void criticalWithConfidenceAndRepeatsIsCritical() {
        int s = svc.score(Severity.CRITICAL, 0.9, 0.5, 5, true);
        assertEquals(Severity.CRITICAL, svc.bandOf(s));
    }

    @Test
    void bruteForceStyleScoreLandsInHighOrCritical() {
        int s = svc.score(Severity.HIGH, 0.8, 0.3, 8, false);
        assertTrue(s >= 50, "expected HIGH band or above, got " + s);
    }

    @Test
    void anomalyOnlyMediumAnomalyIsMedium() {
        int s = svc.score(Severity.MEDIUM, 0.4, 0.6, 1, false);
        assertTrue(s >= 25 && s < 50, "expected MEDIUM band, got " + s);
    }

    @Test
    void scoreNeverExceedsBounds() {
        int max = svc.score(Severity.CRITICAL, 1.0, 1.0, 100, true);
        int min = svc.score(Severity.LOW, 0.0, 0.0, 0, false);
        assertTrue(max <= 100);
        assertTrue(min >= 0);
    }

    @Test
    void bandBoundariesMatchSpec() {
        assertEquals(Severity.CRITICAL, svc.bandOf(75));
        assertEquals(Severity.HIGH, svc.bandOf(50));
        assertEquals(Severity.MEDIUM, svc.bandOf(25));
        assertEquals(Severity.LOW, svc.bandOf(24));
    }
}
