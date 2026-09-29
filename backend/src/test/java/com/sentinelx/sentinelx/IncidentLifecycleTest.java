package com.sentinelx.sentinelx;

import com.sentinelx.sentinelx.entity.IncidentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentLifecycleTest {

    @Test
    void openCanStartInvestigation() {
        assertTrue(IncidentStatus.OPEN.canTransitionTo(IncidentStatus.INVESTIGATING));
    }

    @Test
    void investigatingCanResolve() {
        assertTrue(IncidentStatus.INVESTIGATING.canTransitionTo(IncidentStatus.RESOLVED));
    }

    @Test
    void resolvedCanCloseOrReopen() {
        assertTrue(IncidentStatus.RESOLVED.canTransitionTo(IncidentStatus.CLOSED));
        assertTrue(IncidentStatus.RESOLVED.canTransitionTo(IncidentStatus.INVESTIGATING));
    }

    @Test
    void closedIsTerminal() {
        for (IncidentStatus s : IncidentStatus.values()) {
            assertFalse(IncidentStatus.CLOSED.canTransitionTo(s));
        }
    }

    @Test
    void openCannotJumpToResolved() {
        assertFalse(IncidentStatus.OPEN.canTransitionTo(IncidentStatus.RESOLVED));
    }
}
