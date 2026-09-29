package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.ConfigDtos;
import com.sentinelx.sentinelx.repository.UserRepository;
import com.sentinelx.sentinelx.service.AuditService;
import com.sentinelx.sentinelx.service.MlClientService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

    private final MlClientService mlClientService;
    private final UserRepository userRepository;
    private final String appVersion;

    public SystemInfoController(MlClientService mlClientService,
                                UserRepository userRepository,
                                @Value("${spring.application.name}") String appName) {
        this.mlClientService = mlClientService;
        this.userRepository = userRepository;
        this.appVersion = "SentinelX 1.0.0 (" + appName + ")";
    }

    @GetMapping("/info")
    public ResponseEntity<ConfigDtos.SystemInfoDto> info() {
        boolean dbOk;
        try {
            dbOk = userRepository.count() >= 0;
        } catch (Exception ex) {
            dbOk = false;
        }
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return ResponseEntity.ok(new ConfigDtos.SystemInfoDto(
                appVersion, dbOk, mlClientService.health(), Instant.now(), uptimeSeconds));
    }

    @GetMapping("/ml-health")
    public ResponseEntity<Map<String, Object>> mlHealth() {
        return ResponseEntity.ok(new LinkedHashMap<>(mlClientService.health()));
    }
}
