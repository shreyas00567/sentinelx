package com.sentinelx.sentinelx.entity;

public enum IncidentStatus {
    OPEN, INVESTIGATING, RESOLVED, CLOSED;

    public boolean canTransitionTo(IncidentStatus target) {
        return switch (this) {
            case OPEN -> target == INVESTIGATING || target == CLOSED;
            case INVESTIGATING -> target == RESOLVED;
            case RESOLVED -> target == CLOSED || target == INVESTIGATING;
            case CLOSED -> false;
        };
    }
}
