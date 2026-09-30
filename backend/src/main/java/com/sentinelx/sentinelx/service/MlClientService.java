package com.sentinelx.sentinelx.service;

import com.sentinelx.sentinelx.service.engine.TrackerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MlClientService {

    private static final Logger log = LoggerFactory.getLogger(MlClientService.class);

    public static final List<String> FEATURES = List.of(
            "hour_sin", "hour_cos", "is_off_hours", "failed_attempts",
            "request_rate_per_min", "bytes_sent", "distinct_endpoints_1h",
            "session_duration_min");

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final boolean enabled;

    private static String normalizeBaseUrl(String url) {
        String trimmed = url == null ? "" : url.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            if (!trimmed.contains(".")) {
                trimmed = trimmed + ".onrender.com";
            }
            return "https://" + trimmed;
        }
        return trimmed;
    }

    public MlClientService(@Value("${sentinelx.ml.base-url}") String baseUrl,
                           @Value("${sentinelx.ml.enabled}") boolean enabled,
                           @Value("${sentinelx.ml.connect-timeout-ms}") int connectTimeoutMs,
                           @Value("${sentinelx.ml.read-timeout-ms}") int readTimeoutMs) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.enabled = enabled;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(connectTimeoutMs);
        f.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(f);
    }

    public Map<String, Object> buildFeatures(Instant timestamp, int failedAttempts, long bytes,
                                             int requestRatePerMin, int distinctEndpoints1h,
                                             double sessionDurationMin) {
        ZoneId zone = ZoneId.systemDefault();
        int hour = timestamp.atZone(zone).getHour();
        int minute = timestamp.atZone(zone).getMinute();
        double hourFraction = (hour * 60 + minute) / 1440.0;
        boolean offHours = hour < 6 || hour >= 20;

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("hour_sin", round4(Math.sin(2 * Math.PI * hourFraction)));
        features.put("hour_cos", round4(Math.cos(2 * Math.PI * hourFraction)));
        features.put("is_off_hours", offHours ? 1 : 0);
        features.put("failed_attempts", Math.max(0, failedAttempts));
        features.put("request_rate_per_min", Math.max(0, requestRatePerMin));
        features.put("bytes_sent", Math.max(0, bytes));
        features.put("distinct_endpoints_1h", Math.max(0, distinctEndpoints1h));
        features.put("session_duration_min", round4(Math.max(0, sessionDurationMin)));
        return features;
    }

    public MlPrediction predict(Map<String, Object> features) {
        if (!enabled) {
            return null;
        }
        try {
            Map<String, Object> body = Map.of("features", features);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(baseUrl + "/predict", body, Map.class);
            if (resp == null) {
                return null;
            }
            return new MlPrediction(
                    Boolean.TRUE.equals(resp.get("anomaly")),
                    toDouble(resp.get("anomaly_score")),
                    toDouble(resp.getOrDefault("raw_error", 0)),
                    toDouble(resp.getOrDefault("threshold", 0)),
                    String.valueOf(resp.getOrDefault("model_version", "unknown")));
        } catch (Exception ex) {
            log.debug("ML service unavailable: {}", ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> health() {
        try {
            Map<String, Object> resp = restTemplate.getForObject(baseUrl + "/health", Map.class);
            Map<String, Object> out = new LinkedHashMap<>();
            out.putAll(resp == null ? Map.of() : resp);
            out.put("status", resp != null && "ok".equalsIgnoreCase(String.valueOf(resp.get("status"))) ? "UP" : "DEGRADED");
            return out;
        } catch (Exception ex) {
            return Map.of("status", "DOWN", "detail", ex.getMessage() == null ? "unreachable" : ex.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static double toDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0d;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
