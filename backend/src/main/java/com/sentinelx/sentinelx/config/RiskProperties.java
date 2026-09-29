package com.sentinelx.sentinelx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sentinelx.risk")
public class RiskProperties {

    private int incidentThreshold = 55;
    private int incidentDedupeHours = 2;
    private final Weights weights = new Weights();

    public int getIncidentThreshold() { return incidentThreshold; }
    public void setIncidentThreshold(int incidentThreshold) { this.incidentThreshold = incidentThreshold; }
    public int getIncidentDedupeHours() { return incidentDedupeHours; }
    public void setIncidentDedupeHours(int incidentDedupeHours) { this.incidentDedupeHours = incidentDedupeHours; }
    public Weights getWeights() { return weights; }

    public static class Weights {
        private double confidence = 15;
        private double anomaly = 15;
        private double repeatPerEvent = 2;
        private int repeatCap = 5;
        private double intelBoost = 8;

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public double getAnomaly() { return anomaly; }
        public void setAnomaly(double anomaly) { this.anomaly = anomaly; }
        public double getRepeatPerEvent() { return repeatPerEvent; }
        public void setRepeatPerEvent(double repeatPerEvent) { this.repeatPerEvent = repeatPerEvent; }
        public int getRepeatCap() { return repeatCap; }
        public void setRepeatCap(int repeatCap) { this.repeatCap = repeatCap; }
        public double getIntelBoost() { return intelBoost; }
        public void setIntelBoost(double intelBoost) { this.intelBoost = intelBoost; }
    }
}
