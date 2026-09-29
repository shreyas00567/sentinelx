package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.DashboardDtos;
import com.sentinelx.sentinelx.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardDtos.SummaryDto> summary() {
        return ResponseEntity.ok(dashboardService.summary());
    }

    @GetMapping("/trends")
    public ResponseEntity<DashboardDtos.SummaryDto> trends() {
        return ResponseEntity.ok(dashboardService.summary());
    }
}
