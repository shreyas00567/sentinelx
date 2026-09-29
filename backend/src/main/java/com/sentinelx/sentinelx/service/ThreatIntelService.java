package com.sentinelx.sentinelx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentinelx.sentinelx.entity.ThreatIntelRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ThreatIntelService {

    private static final Logger log = LoggerFactory.getLogger(ThreatIntelService.class);
    private static final String OFFLINE_PROVIDER = "offline-heuristic-v1";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String abuseIpdbKey;
    private final long cacheHours;
    private final com.sentinelx.sentinelx.repository.ThreatIntelRepository repository;

    public ThreatIntelService(@Value("${sentinelx.threat-intel.abuseipdb-key:}") String abuseIpdbKey,
                              @Value("${sentinelx.threat-intel.cache-hours}") long cacheHours,
                              com.sentinelx.sentinelx.repository.ThreatIntelRepository repository) {
        this.abuseIpdbKey = abuseIpdbKey == null ? "" : abuseIpdbKey.trim();
        this.cacheHours = cacheHours;
        this.repository = repository;
    }

    public ConfigDtosHolder lookup(String ip) {
        var cached = repository.findFirstByIpOrderByQueriedAtDesc(ip);
        if (cached.isPresent()
                && cached.get().getQueriedAt().isAfter(Instant.now().minusSeconds(cacheHours * 3600))) {
            return toDto(cached.get());
        }
        ThreatIntelRecord record = abuseIpdbKey.isEmpty() ? offlineLookup(ip) : abuseIpdbLookup(ip);
        record.setQueriedAt(Instant.now());
        repository.save(record);
        return toDto(record);
    }

    private ThreatIntelRecord abuseIpdbLookup(String ip) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Key", abuseIpdbKey);
            headers.set("Accept", "application/json");
            ResponseEntity<String> resp = restTemplate.exchange(
                    "https://api.abuseipdb.com/api/v2/check?ipAddress=" + ip + "&maxAgeInDays=90",
                    HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode data = JsonUtil.read(resp.getBody()).path("data");
            ThreatIntelRecord r = new ThreatIntelRecord();
            r.setIp(ip);
            r.setMalicious(data.path("abuseConfidenceScore").asInt(0) >= 50);
            r.setConfidenceScore(data.path("abuseConfidenceScore").asInt(0));
            r.setCountryCode(data.path("countryCode").asText(""));
            r.setIsp(data.path("isp").asText(""));
            r.setUsageType(data.path("usageType").asText(""));
            r.setTotalReports(data.path("totalReports").asLong(0));
            r.setSimulated(false);
            r.setProvider("AbuseIPDB");
            r.setRawDataJson(JsonUtil.write(data));
            return r;
        } catch (Exception ex) {
            log.info("AbuseIPDB lookup failed for {}, falling back to heuristic: {}", ip, ex.getMessage());
            return offlineLookup(ip);
        }
    }

    private ThreatIntelRecord offlineLookup(String ip) {
        int hash = Math.abs(ip.hashCode());
        int score = hash % 101;
        String[] isps = {"Comcast", "Deutsche Telekom", "DigitalOcean", "Hetzner", "OVH", "China Telecom"};
        String[] countries = {"US", "DE", "NL", "RU", "CN", "BR", "IN", "FR"};
        String[] usages = {"Data Center/Web Hosting/Transit", "Fixed Line ISP", "Commercial", "Mobile ISP"};

        ThreatIntelRecord r = new ThreatIntelRecord();
        r.setIp(ip);
        r.setMalicious(score >= 60);
        r.setConfidenceScore(score);
        r.setCountryCode(countries[hash % countries.length]);
        r.setIsp(isps[hash % isps.length]);
        r.setUsageType(usages[hash % usages.length]);
        r.setTotalReports(score >= 60 ? (hash % 400) + 10L : hash % 8L);
        r.setSimulated(true);
        r.setProvider(OFFLINE_PROVIDER);
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("note", "Deterministic offline heuristic. Set ABUSEIPDB_API_KEY for live enrichment.");
        raw.put("seedHash", hash);
        r.setRawDataJson(JsonUtil.write(raw));
        return r;
    }

    private ConfigDtosHolder toDto(ThreatIntelRecord r) {
        return new ConfigDtosHolder(
                r.getIp(), r.isMalicious(), r.getConfidenceScore(), r.getCountryCode(),
                r.getIsp(), r.getUsageType(), r.getTotalReports(), r.isSimulated(), r.getProvider(),
                r.getQueriedAt());
    }

    public record ConfigDtosHolder(
            String ip,
            boolean malicious,
            int confidenceScore,
            String countryCode,
            String isp,
            String usageType,
            long totalReports,
            boolean simulated,
            String provider,
            Instant queriedAt) {}
}
