package com.sentinelx.sentinelx.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "threat_intelligence")
public class ThreatIntelRecord {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(nullable = false)
    private boolean malicious;

    @Column(name = "confidence_score")
    private int confidenceScore;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(length = 128)
    private String isp;

    @Column(name = "usage_type", length = 64)
    private String usageType;

    @Column(name = "total_reports")
    private long totalReports;

    @Column(nullable = false)
    private boolean simulated;

    @Column(length = 32)
    private String provider;

    @Column(name = "raw_data_json", columnDefinition = "TEXT")
    private String rawDataJson;

    @Column(name = "queried_at", nullable = false)
    private Instant queriedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public boolean isMalicious() { return malicious; }
    public void setMalicious(boolean malicious) { this.malicious = malicious; }
    public int getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getIsp() { return isp; }
    public void setIsp(String isp) { this.isp = isp; }
    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }
    public long getTotalReports() { return totalReports; }
    public void setTotalReports(long totalReports) { this.totalReports = totalReports; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getRawDataJson() { return rawDataJson; }
    public void setRawDataJson(String rawDataJson) { this.rawDataJson = rawDataJson; }
    public Instant getQueriedAt() { return queriedAt; }
    public void setQueriedAt(Instant queriedAt) { this.queriedAt = queriedAt; }
}
