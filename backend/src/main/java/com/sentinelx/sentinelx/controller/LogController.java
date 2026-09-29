package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.IngestDtos;
import com.sentinelx.sentinelx.dto.ViewDtos;
import com.sentinelx.sentinelx.service.LogIngestionService;
import com.sentinelx.sentinelx.service.LogQueryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogIngestionService ingestionService;
    private final LogQueryService logQueryService;

    public LogController(LogIngestionService ingestionService, LogQueryService logQueryService) {
        this.ingestionService = ingestionService;
        this.logQueryService = logQueryService;
    }

    @PostMapping
    public ResponseEntity<IngestDtos.ProcessingOutcome> ingest(@Valid @RequestBody IngestDtos.IngestRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingestionService.ingest(req));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<IngestDtos.ProcessingOutcome>> ingestBatch(@Valid @RequestBody IngestDtos.BatchIngestRequest req) {
        List<IngestDtos.ProcessingOutcome> outcomes = new ArrayList<>();
        for (IngestDtos.IngestRequest e : req.events()) {
            outcomes.add(ingestionService.ingest(e));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(outcomes);
    }

    @GetMapping
    public ResponseEntity<Page<ViewDtos.LogDto>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(logQueryService.list(q, eventType, page, Math.min(size, 200)));
    }
}
