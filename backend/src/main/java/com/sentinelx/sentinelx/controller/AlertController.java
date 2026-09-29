package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.ViewDtos;
import com.sentinelx.sentinelx.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<Page<ViewDtos.AlertDto>> list(
            @RequestParam(required = false) String attackType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertService.list(attackType, severity, status, q, page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViewDtos.AlertDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(alertService.get(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ViewDtos.AlertDto> updateStatus(@PathVariable UUID id,
                                                          @Valid @RequestBody ViewDtos.UpdateAlertRequest req) {
        return ResponseEntity.ok(alertService.updateStatus(id, req));
    }
}
