package com.sentinelx.sentinelx.service;

import com.sentinelx.sentinelx.dto.ViewDtos;
import com.sentinelx.sentinelx.entity.SecurityEvent;
import com.sentinelx.sentinelx.repository.SecurityEventRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LogQueryService {

    private final SecurityEventRepository repository;

    public LogQueryService(SecurityEventRepository repository) {
        this.repository = repository;
    }

    public Page<ViewDtos.LogDto> list(String q, String eventType, int page, int size) {
        Specification<SecurityEvent> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (eventType != null && !eventType.isBlank()) {
                ps.add(cb.equal(cb.upper(root.get("eventType")), eventType.trim().toUpperCase()));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                Predicate ip = cb.like(cb.lower(cb.coalesce(root.get("sourceIp"), "")), like);
                Predicate user = cb.like(cb.lower(cb.coalesce(root.get("username"), "")), like);
                Predicate ep = cb.like(cb.lower(cb.coalesce(root.get("endpoint"), "")), like);
                Predicate evt = cb.like(cb.lower(cb.coalesce(root.get("eventType"), "")), like);
                ps.add(cb.or(ip, user, ep, evt));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return repository.findAll(spec, pageable).map(LogQueryService::toDto);
    }

    public static ViewDtos.LogDto toDto(SecurityEvent e) {
        return new ViewDtos.LogDto(
                e.getId(), e.getEventId(), e.getTimestamp(), e.getSourceIp(), e.getDestinationIp(),
                e.getUsername(), e.getSource(), e.getEndpoint(), e.getHttpMethod(), e.getStatus(),
                e.getProtocol(), e.getPort(), e.getBytes(), e.getFailedAttempts(), e.getEventType(),
                e.getRawMessage(), e.getAttackType(),
                e.getSeverity() == null ? null : e.getSeverity().name(),
                e.getAnomalyScore(), e.isProcessed());
    }
}
