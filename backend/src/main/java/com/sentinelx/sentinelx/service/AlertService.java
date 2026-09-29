package com.sentinelx.sentinelx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentinelx.sentinelx.dto.ViewDtos;
import com.sentinelx.sentinelx.entity.Alert;
import com.sentinelx.sentinelx.entity.AttackType;
import com.sentinelx.sentinelx.entity.AlertStatus;
import com.sentinelx.sentinelx.entity.Severity;
import com.sentinelx.sentinelx.exception.ApiException;
import com.sentinelx.sentinelx.repository.AlertRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public Page<ViewDtos.AlertDto> list(String attackType, String severity, String status,
                                        String q, int page, int size) {
        Specification<Alert> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (attackType != null && !attackType.isBlank()) {
                ps.add(cb.equal(root.get("attackType"), parseEnum(AttackType.class, attackType, "attack type")));
            }
            if (severity != null && !severity.isBlank()) {
                ps.add(cb.equal(root.get("severity"), parseEnum(Severity.class, severity, "severity")));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(root.get("status"), parseEnum(AlertStatus.class, status, "alert status")));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("sourceIp"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("target"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("explanation"), "")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return alertRepository.findAll(spec, pageable).map(this::toDto);
    }

    public ViewDtos.AlertDto get(UUID id) {
        return alertRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found"));
    }

    public ViewDtos.AlertDto updateStatus(UUID id, ViewDtos.UpdateAlertRequest req) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found"));
        try {
            alert.setStatus(AlertStatus.valueOf(req.status().trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown alert status: " + req.status());
        }
        return toDto(alertRepository.save(alert));
    }

    public ViewDtos.AlertDto toDto(Alert a) {
        return new ViewDtos.AlertDto(
                a.getId(), a.getCreatedAt(), a.getAttackType().name(), a.getDetectionMethod().name(),
                a.getSeverity().name(), a.getRiskScore(), a.getConfidence(), a.getSourceIp(),
                a.getTarget(), safeParse(a.getEvidence()), a.getExplanation(), a.getStatus().name(),
                a.getAnomalyScore(), a.getEventId());
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value, String label) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid " + label + ": " + value);
        }
    }

    private static Object safeParse(String json) {
        JsonNode n = JsonUtil.read(json == null ? "{}" : json);
        return n.isMissingNode() ? json : JsonUtil.convert(n, Object.class);
    }
}
