package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.ConfigDtos;
import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import com.sentinelx.sentinelx.entity.Severity;
import com.sentinelx.sentinelx.exception.ApiException;
import com.sentinelx.sentinelx.repository.DetectionRuleRepository;
import com.sentinelx.sentinelx.service.engine.RuleEngineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final DetectionRuleRepository ruleRepository;
    private final RuleEngineService ruleEngineService;

    public RuleController(DetectionRuleRepository ruleRepository, RuleEngineService ruleEngineService) {
        this.ruleRepository = ruleRepository;
        this.ruleEngineService = ruleEngineService;
    }

    @GetMapping
    public ResponseEntity<Iterable<ConfigDtos.RuleDto>> list() {
        return ResponseEntity.ok(ruleRepository.findAllByOrderByIdAsc().stream()
                .map(RuleController::toDto).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfigDtos.RuleDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody ConfigDtos.UpdateRuleRequest req) {
        DetectionRuleEntity rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rule not found"));
        if (req.enabled() != null) {
            rule.setEnabled(req.enabled());
        }
        if (req.threshold() != null) {
            if (req.threshold() < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Threshold must be >= 1");
            }
            rule.setThreshold(req.threshold());
        }
        if (req.windowSeconds() != null) {
            if (req.windowSeconds() < 10 || req.windowSeconds() > 3600) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Window must be between 10 and 3600 seconds");
            }
            rule.setWindowSeconds(req.windowSeconds());
        }
        if (req.severity() != null && !req.severity().isBlank()) {
            try {
                rule.setSeverity(Severity.valueOf(req.severity().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid severity: " + req.severity());
            }
        }
        rule = ruleRepository.save(rule);
        ruleEngineService.clearCaches();
        return ResponseEntity.ok(toDto(rule));
    }

    private static ConfigDtos.RuleDto toDto(DetectionRuleEntity r) {
        return new ConfigDtos.RuleDto(r.getId(), r.getRuleKey(), r.getName(), r.getDescription(),
                r.isEnabled(), r.getThreshold(), r.getWindowSeconds(), r.getSeverity().name());
    }
}
