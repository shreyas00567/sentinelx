package com.sentinelx.sentinelx.service;

import com.sentinelx.sentinelx.config.RiskProperties;
import com.sentinelx.sentinelx.entity.Severity;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {

    private final RiskProperties props;

    public RiskScoringService(RiskProperties props) {
        this.props = props;
    }

    public int score(Severity severity, double confidence, double anomalyScore, int repeatCount) {
        return score(severity, confidence, anomalyScore, repeatCount, false);
    }

    public int score(Severity severity, double confidence, double anomalyScore,
                     int repeatCount, boolean intelMalicious) {
        double base = switch (severity) {
            case CRITICAL -> 70;
            case HIGH -> 50;
            case MEDIUM -> 30;
            case LOW -> 15;
        };
        RiskProperties.Weights w = props.getWeights();
        double repeats = Math.min(Math.max(repeatCount, 0), w.getRepeatCap()) * w.getRepeatPerEvent();
        double anomaly = clamp01(anomalyScore) * w.getAnomaly();
        double conf = clamp01(confidence) * w.getConfidence();
        double intel = intelMalicious ? w.getIntelBoost() : 0;

        int total = (int) Math.round(base + conf + anomaly + repeats + intel);
        return Math.max(0, Math.min(100, total));
    }

    public Severity bandOf(int score) {
        if (score >= 75) {
            return Severity.CRITICAL;
        }
        if (score >= 50) {
            return Severity.HIGH;
        }
        if (score >= 25) {
            return Severity.MEDIUM;
        }
        return Severity.LOW;
    }

    public int incidentThreshold() {
        return props.getIncidentThreshold();
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) {
            return 0;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }
}
