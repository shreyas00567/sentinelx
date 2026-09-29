package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.DashboardDtos;
import com.sentinelx.sentinelx.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardDtos.ReportDto> summary(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(reportService.generate(Math.max(1, Math.min(days, 90))));
    }

    @GetMapping("/summary/download")
    public ResponseEntity<byte[]> download(@RequestParam(defaultValue = "7") int days) {
        String rendered = reportService.generate(Math.max(1, Math.min(days, 90))).rendered();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=sentinelx-report-" + days + "d.txt")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(rendered.getBytes(StandardCharsets.UTF_8));
    }
}
