package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.IngestDtos;
import com.sentinelx.sentinelx.service.AuditService;
import com.sentinelx.sentinelx.service.SimulatorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final SimulatorService simulatorService;
    private final AuditService auditService;

    public SimulatorController(SimulatorService simulatorService, AuditService auditService) {
        this.simulatorService = simulatorService;
        this.auditService = auditService;
    }

    @PostMapping("/{scenario}")
    public ResponseEntity<IngestDtos.SimulatorResult> run(@PathVariable String scenario,
                                                          Authentication auth,
                                                          HttpServletRequest http) {
        IngestDtos.SimulatorResult result = simulatorService.run(scenario);
        auditService.log(auth != null ? auth.getName() : "unknown", "SIMULATION_RUN",
                "scenario=" + scenario + " events=" + result.eventsGenerated(), http);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/scenarios")
    public ResponseEntity<Iterable<String>> scenarios() {
        return ResponseEntity.ok(simulatorService.scenarioNames());
    }
}
