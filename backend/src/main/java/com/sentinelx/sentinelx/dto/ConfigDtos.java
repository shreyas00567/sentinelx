package com.sentinelx.sentinelx.dto;

import java.time.Instant;
import java.util.Map;

public final class ConfigDtos {

    private ConfigDtos() {}

    public record RuleDto(
            Long id,
            String ruleKey,
            String name,
            String description,
            boolean enabled,
            int threshold,
            int windowSeconds,
            String severity) {}

    public record UpdateRuleRequest(Boolean enabled, Integer threshold, Integer windowSeconds, String severity) {}

    public record UserDto(
            Long id,
            String username,
            String email,
            String fullName,
            String role,
            boolean enabled,
            Instant createdAt) {}

    public record CreateUserRequest(
            String username,
            String email,
            String password,
            String fullName,
            String role) {}

    public record UpdateUserRequest(Boolean enabled, String password, String role, String fullName) {}

    public record ThreatIntelDto(
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

    public record SystemInfoDto(
            String appVersion,
            boolean dbOk,
            Map<String, Object> mlHealth,
            Instant serverTime,
            long uptimeSeconds) {}
}
