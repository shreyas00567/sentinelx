package com.sentinelx.sentinelx.controller;

import com.sentinelx.sentinelx.dto.ViewDtos;
import com.sentinelx.sentinelx.entity.Incident;
import com.sentinelx.sentinelx.entity.IncidentStatus;
import com.sentinelx.sentinelx.exception.ApiException;
import com.sentinelx.sentinelx.repository.IncidentRepository;
import com.sentinelx.sentinelx.service.AuditService;
import com.sentinelx.sentinelx.service.IncidentService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;
    private final AuditService auditService;

    public IncidentController(IncidentRepository incidentRepository,
                              IncidentService incidentService,
                              AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.incidentService = incidentService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<Page<ViewDtos.IncidentDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String attackType,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Specification<Incident> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                try {
                    ps.add(cb.equal(root.get("status"), IncidentStatus.valueOf(status.trim().toUpperCase())));
                } catch (IllegalArgumentException ex) {
                    throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                            "Invalid status: " + status);
                }
            }
            if (attackType != null && !attackType.isBlank()) {
                try {
                    ps.add(cb.equal(root.get("attackType"),
                            com.sentinelx.sentinelx.entity.AttackType.valueOf(attackType.trim().toUpperCase())));
                } catch (IllegalArgumentException ex) {
                    throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                            "Invalid attack type: " + attackType);
                }
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("title"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("sourceIp"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("assignedTo"), "")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(incidentRepository.findAll(spec, pageable).map(incidentService::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViewDtos.IncidentDto> get(@PathVariable UUID id) {
        Incident inc = incidentRepository.findById(id)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "Incident not found"));
        return ResponseEntity.ok(incidentService.toDto(inc));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ViewDtos.IncidentDto> update(@PathVariable UUID id,
                                                       @Valid @RequestBody ViewDtos.UpdateIncidentRequest req,
                                                       Authentication auth,
                                                       HttpServletRequest http) {
        String actor = auth != null ? auth.getName() : "unknown";
        ViewDtos.IncidentDto updated = incidentService.toDto(incidentService.update(id, req, actor));
        auditService.log(actor, "INCIDENT_UPDATED",
                "id=" + id + " status=" + updated.status() + " assignedTo=" + updated.assignedTo(), http);
        return ResponseEntity.ok(updated);
    }
}
