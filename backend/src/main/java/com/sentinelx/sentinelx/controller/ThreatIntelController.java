package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.ConfigDtos;
import com.sentinelx.sentinelx.service.ThreatIntelService;
import com.sentinelx.sentinelx.repository.ThreatIntelRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/threat-intel")
public class ThreatIntelController {

    private final ThreatIntelService threatIntelService;
    private final ThreatIntelRepository threatIntelRepository;

    public ThreatIntelController(ThreatIntelService threatIntelService,
                                 ThreatIntelRepository threatIntelRepository) {
        this.threatIntelService = threatIntelService;
        this.threatIntelRepository = threatIntelRepository;
    }

    @GetMapping("/ip/{ip}")
    public ResponseEntity<ConfigDtos.ThreatIntelDto> lookup(@PathVariable String ip) {
        var dto = threatIntelService.lookup(ip);
        return ResponseEntity.ok(new ConfigDtos.ThreatIntelDto(
                dto.ip(), dto.malicious(), dto.confidenceScore(), dto.countryCode(),
                dto.isp(), dto.usageType(), dto.totalReports(), dto.simulated(),
                dto.provider(), dto.queriedAt()));
    }

    @GetMapping("/recent")
    public ResponseEntity<Iterable<ConfigDtos.ThreatIntelDto>> recent() {
        return ResponseEntity.ok(threatIntelRepository.findTop20ByOrderByQueriedAtDesc().stream()
                .map(r -> new ConfigDtos.ThreatIntelDto(
                        r.getIp(), r.isMalicious(), r.getConfidenceScore(), r.getCountryCode(),
                        r.getIsp(), r.getUsageType(), r.getTotalReports(), r.isSimulated(),
                        r.getProvider(), r.getQueriedAt()))
                .toList());
    }
}
