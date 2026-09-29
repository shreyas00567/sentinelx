package com.sentinelx.sentinelx.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {}

    public record DistributionItem(String type, long count) {}

    public record TopIp(String ip, long count, int maxRisk) {}

    public record Point(String label, long count) {}

    public record SummaryDto(
            Map<String, Long> alertsBySeverity,
            long alertsTotal,
            long alertsToday,
            long aiAnomaliesToday,
            Map<String, Long> incidentsByStatus,
            long openIncidents,
            List<DistributionItem> attackDistribution,
            List<TopIp> topSourceIps,
            List<Point> alertsHourlyTrend,
            List<Point> incidentsDailyTrend,
            List<ViewDtos.AlertDto> recentAlerts,
            Map<String, Long> eventsBySeverityToday) {}

    public record ReportDto(
            int days,
            Instant generatedAt,
            long totalAlerts,
            Map<String, Long> alertsBySeverity,
            List<DistributionItem> alertsByType,
            long incidentsOpened,
            long incidentsResolved,
            long incidentsClosed,
            double avgResolutionHours,
            List<TopIp> topSourceIps,
            long aiAnomalies,
            List<String> recommendations,
            String rendered) {}
}
