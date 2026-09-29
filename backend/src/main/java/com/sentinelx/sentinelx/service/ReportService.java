package com.sentinelx.sentinelx.service;

import com.sentinelx.sentinelx.dto.DashboardDtos;
import com.sentinelx.sentinelx.repository.AlertRepository;
import com.sentinelx.sentinelx.repository.IncidentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final DashboardService dashboardService;

    public ReportService(AlertRepository alertRepository,
                         IncidentRepository incidentRepository,
                         DashboardService dashboardService) {
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
        this.dashboardService = dashboardService;
    }

    public DashboardDtos.ReportDto generate(int days) {
        Instant since = Instant.now().minus(Duration.ofDays(days));

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Object[] row : alertRepository.countGroupBySeverity()) {
            bySeverity.put(String.valueOf(row[0]), (Long) row[1]);
        }
        long total = bySeverity.values().stream().mapToLong(Long::longValue).sum();

        List<DashboardDtos.DistributionItem> byType = new ArrayList<>();
        for (Object[] row : alertRepository.countGroupByAttackType()) {
            byType.add(new DashboardDtos.DistributionItem(String.valueOf(row[0]), (Long) row[1]));
        }

        List<DashboardDtos.TopIp> topIps = new ArrayList<>();
        for (Object[] row : alertRepository.topSourceIps(PageRequest.of(0, 5))) {
            topIps.add(new DashboardDtos.TopIp(String.valueOf(row[0]), (Long) row[1],
                    row[2] instanceof Number n ? n.intValue() : 0));
        }

        List<Object[]> resolvedRows = incidentRepository.resolvedSince(since);
        long resolvedCount = resolvedRows.size();
        double avgHours = resolvedRows.stream()
                .filter(r -> r[0] instanceof java.time.Instant && r[1] instanceof java.time.Instant)
                .mapToDouble(r -> Duration.between((java.time.Instant) r[0], (java.time.Instant) r[1]).toMillis() / 3_600_000.0)
                .average().orElse(0);

        long aiAnomalies = alertRepository
                .countByAnomalyScoreIsNotNullAndDetectionMethodNotAndCreatedAtAfter(
                        com.sentinelx.sentinelx.entity.DetectionMethod.RULE, since);

        List<String> recommendations = buildRecommendations(byType, aiAnomalies);
        String rendered = renderText(days, total, bySeverity, byType, resolvedCount, avgHours, topIps, aiAnomalies, recommendations);

        return new DashboardDtos.ReportDto(days, Instant.now(), total, bySeverity, byType,
                incidentRepository.countByCreatedAtAfter(since), resolvedCount,
                incidentRepository.countByStatus(com.sentinelx.sentinelx.entity.IncidentStatus.CLOSED),
                Math.round(avgHours * 10.0) / 10.0, topIps, aiAnomalies, recommendations, rendered);
    }

    private List<String> buildRecommendations(List<DashboardDtos.DistributionItem> byType, long aiAnomalies) {
        List<String> recs = new ArrayList<>();
        for (DashboardDtos.DistributionItem item : byType) {
            switch (item.type()) {
                case "BRUTE_FORCE" -> recs.add("Brute-force activity detected: enforce MFA and stricter lockout policies.");
                case "SQL_INJECTION" -> recs.add("SQL injection attempts observed: audit endpoints for parameterized queries.");
                case "XSS" -> recs.add("XSS attempts observed: deploy CSP headers and output encoding.");
                case "DDOS" -> recs.add("High-rate traffic observed: review rate limiting at edge.");
                case "PORT_SCAN" -> recs.add("Reconnaissance observed: minimize exposed services and close unused ports.");
                case "ANOMALY" -> { }
                default -> { }
            }
        }
        if (aiAnomalies > 0) {
            recs.add(aiAnomalies + " AI-flagged behavioral anomalies: verify affected user accounts.");
        }
        if (recs.isEmpty()) {
            recs.add("No significant risk patterns detected in this period. Continue routine monitoring.");
        }
        return recs;
    }

    private String renderText(int days, long total, Map<String, Long> bySeverity,
                              List<DashboardDtos.DistributionItem> byType, long resolved,
                              double avgHours, List<DashboardDtos.TopIp> topIps,
                              long aiAnomalies, List<String> recs) {
        StringBuilder sb = new StringBuilder();
        sb.append("==============================================\n");
        sb.append("        SENTINELX SECURITY REPORT\n");
        sb.append("==============================================\n");
        sb.append("Generated: ").append(Instant.now()).append("\n");
        sb.append("Period: last ").append(days).append(" day(s)\n\n");
        sb.append("--- ALERTS ---\n");
        sb.append("Total alerts: ").append(total).append("\n");
        bySeverity.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append('\n'));
        sb.append("\n--- ATTACK DISTRIBUTION ---\n");
        byType.forEach(t -> sb.append("  ").append(t.type()).append(": ").append(t.count()).append('\n'));
        sb.append("\n--- INCIDENTS ---\n");
        sb.append("Resolved: ").append(resolved).append('\n');
        sb.append("Avg resolution time (h): ").append(Math.round(avgHours * 10.0) / 10.0).append('\n');
        sb.append("\n--- TOP SOURCE IPs ---\n");
        topIps.forEach(ip -> sb.append("  ").append(ip.ip())
                .append("  alerts=").append(ip.count())
                .append(" maxRisk=").append(ip.maxRisk()).append('\n'));
        sb.append("\n--- AI ANOMALIES ---\n").append(aiAnomalies).append('\n');
        sb.append("\n--- RECOMMENDATIONS ---\n");
        recs.forEach(r -> sb.append("  - ").append(r).append('\n'));
        sb.append("\nEnd of report.\n");
        return sb.toString();
    }
}
