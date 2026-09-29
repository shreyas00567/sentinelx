package com.sentinelx.sentinelx.service.engine;

import java.util.List;

public record TrackerStats(
        String sourceIp,
        List<String> recentUsernames,
        int failuresRecent,
        int requestsRecent,
        int distinctPortsRecent,
        int distinctEndpointsHour) {

    public static TrackerStats empty(String ip) {
        return new TrackerStats(ip, List.of(), 0, 0, 0, 0);
    }
}
