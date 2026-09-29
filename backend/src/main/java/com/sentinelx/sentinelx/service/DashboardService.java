package com.sentinelx.sentinelx.service;

import com.sentinelx.sentinelx.dto.DashboardDtos;
import com.sentinelx.sentinelx.entity.DetectionMethod;
import com.sentinelx.sentinelx.repository.AlertRepository;
import com.sentinelx.sentinelx.repository.IncidentRepository;
import com.sentinelx.sentinelx.repository.SecurityEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DashboardService {

    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("MM-dd HH:00");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final SecurityEventRepository eventRepository;
    private final AlertService alertService;

    public DashboardService(AlertRepository alertRepository,
                            IncidentRepository incidentRepository,
                            SecurityEventRepository eventRepository,
                            AlertService alertService) {
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
        this.eventRepository = eventRepository;
        this.alertService = alertService;
    }

    public DashboardDtos.SummaryDto summary() {
        Instant dayAgo = Instant.now().minus(Duration.ofHours(24));
        Instant weekAgo = Instant.now().minus(Duration.ofDays(7));

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Object[] row : alertRepository.countGroupBySeverity()) {
            bySeverity.put(String.valueOf(row[0]), (Long) row[1]);
        }

        Map<String, Long> incidentsByStatus = new LinkedHashMap<>();
        for (com.sentinelx.sentinelx.entity.IncidentStatus s : com.sentinelx.sentinelx.entity.IncidentStatus.values()) {
            incidentsByStatus.put(s.name(), incidentRepository.countByStatus(s));
        }

        List<DashboardDtos.DistributionItem> distribution = new ArrayList<>();
        for (Object[] row : alertRepository.countGroupByAttackType()) {
            distribution.add(new DashboardDtos.DistributionItem(String.valueOf(row[0]), (Long) row[1]));
        }

        List<DashboardDtos.TopIp> topIps = new ArrayList<>();
        for (Object[] row : alertRepository.topSourceIps(PageRequest.of(0, 5))) {
            long count = (Long) row[1];
            int maxRisk = row[2] instanceof Number n ? n.intValue() : 0;
            topIps.add(new DashboardDtos.TopIp(String.valueOf(row[0]), count, maxRisk));
        }

        List<DashboardDtos.Point> hourly = bucketByHour(alertRepository.timestampsSince(dayAgo));
        List<DashboardDtos.Point> dailyIncidents = bucketByDay(incidentRepository.createdTimestampsSince(weekAgo));

        return new DashboardDtos.SummaryDto(
                bySeverity,
                bySeverity.values().stream().mapToLong(Long::longValue).sum(),
                alertRepository.countByCreatedAtAfter(dayAgo),
                alertRepository.countByAnomalyScoreIsNotNullAndDetectionMethodNotAndCreatedAtAfter(
                        DetectionMethod.RULE, dayAgo),
                incidentsByStatus,
                incidentsByStatus.getOrDefault("OPEN", 0L) + incidentsByStatus.getOrDefault("INVESTIGATING", 0L),
                distribution,
                topIps,
                hourly,
                dailyIncidents,
                alertRepository.findTop10ByOrderByCreatedAtDesc().stream()
                        .map(alertService::toDto).toList(),
                Map.of());
    }

    public List<DashboardDtos.Point> bucketByHour(List<Instant> timestamps) {
        ZoneId zone = ZoneId.systemDefault();
        Map<String, Long> buckets = new TreeMap<>();
        Instant start = Instant.now().minus(Duration.ofHours(12));
        for (int i = 0; i <= 12; i++) {
            buckets.put(start.atZone(zone).plusHours(i).format(HOUR_FMT), 0L);
        }
        for (Instant t : timestamps) {
            String key = t.atZone(zone).format(HOUR_FMT);
            buckets.merge(key, 1L, Long::sum);
        }
        return buckets.entrySet().stream()
                .map(e -> new DashboardDtos.Point(e.getKey(), e.getValue()))
                .toList();
    }

    public List<DashboardDtos.Point> bucketByDay(List<Instant> timestamps) {
        ZoneId zone = ZoneId.systemDefault();
        Map<String, Long> buckets = new TreeMap<>();
        Instant start = Instant.now().minus(Duration.ofDays(6));
        for (int i = 0; i <= 6; i++) {
            buckets.put(start.atZone(zone).plusDays(i).format(DAY_FMT), 0L);
        }
        for (Instant t : timestamps) {
            String key = t.atZone(zone).format(DAY_FMT);
            buckets.merge(key, 1L, Long::sum);
        }
        return buckets.entrySet().stream()
                .map(e -> new DashboardDtos.Point(e.getKey(), e.getValue()))
                .toList();
    }
}
