package com.sentinelx.sentinelx.service.engine;

import com.sentinelx.sentinelx.entity.SecurityEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventTracker {

    private record Hit(Instant time, Integer port, String endpoint, boolean failed, String username) {}

    private static final Duration MAX_WINDOW = Duration.ofMinutes(30);
    private static final int MAX_EVENTS_PER_IP = 2000;

    private final Map<String, List<Hit>> byIp = new ConcurrentHashMap<>();

    public void record(SecurityEvent event) {
        if (event.getSourceIp() == null || event.getSourceIp().isBlank()) {
            return;
        }
        Instant now = Instant.now();
        byIp.compute(event.getSourceIp(), (ip, hits) -> {
            List<Hit> list = (hits == null) ? new ArrayList<>() : hits;
            synchronized (list) {
                list.add(new Hit(now, event.getPort(), event.getEndpoint(),
                        "LOGIN_FAILED".equalsIgnoreCase(safe(event.getEventType())),
                        safe(event.getUsername())));
                if (list.size() > MAX_EVENTS_PER_IP) {
                    list.subList(0, list.size() - MAX_EVENTS_PER_IP).clear();
                }
            }
            return list;
        });
        pruneAll(now);
    }

    public TrackerStats stats(String ip) {
        List<Hit> hits = byIp.get(ip);
        if (hits == null) {
            return TrackerStats.empty(ip);
        }
        Instant now = Instant.now();
        List<Hit> snapshot;
        synchronized (hits) {
            snapshot = new ArrayList<>(hits);
        }
        Set<Integer> ports = new HashSet<>();
        Set<String> endpoints = new HashSet<>();
        List<String> usernames = new ArrayList<>();
        int failures = 0;
        for (Hit h : snapshot) {
            Duration age = Duration.between(h.time(), now);
            if (age.isNegative() || age.compareTo(MAX_WINDOW) > 0) {
                continue;
            }
            if (h.port() != null) {
                ports.add(h.port());
            }
            if (h.endpoint() != null && age.toMinutes() <= 60) {
                endpoints.add(h.endpoint());
            }
            if (h.failed()) {
                failures++;
                if (h.username() != null && !usernames.contains(h.username())) {
                    usernames.add(h.username());
                }
            }
        }
        int recentRequests = 0;
        for (Hit h : snapshot) {
            if (!Duration.between(h.time(), now).isNegative()
                    && Duration.between(h.time(), now).getSeconds() <= 60) {
                recentRequests++;
            }
        }
        return new TrackerStats(ip, usernames, failures, recentRequests,
                ports.size(), endpoints.size());
    }

    public void clear() {
        byIp.clear();
    }

    private void pruneAll(Instant now) {
        for (Map.Entry<String, List<Hit>> e : byIp.entrySet()) {
            List<Hit> list = e.getValue();
            synchronized (list) {
                list.removeIf(h -> Duration.between(h.time(), now).compareTo(MAX_WINDOW) > 0);
            }
            if (list.isEmpty()) {
                byIp.remove(e.getKey());
            }
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
