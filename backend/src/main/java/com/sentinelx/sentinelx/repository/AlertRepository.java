package com.sentinelx.sentinelx.repository;

import com.sentinelx.sentinelx.entity.Alert;
import com.sentinelx.sentinelx.entity.DetectionMethod;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {

    long countByCreatedAtAfter(Instant since);

    long countBySeverity(com.sentinelx.sentinelx.entity.Severity severity);

    @Query("select a.severity, count(a) from Alert a group by a.severity")
    List<Object[]> countGroupBySeverity();

    @Query("select a.attackType, count(a) from Alert a group by a.attackType order by count(a) desc")
    List<Object[]> countGroupByAttackType();

    @Query("select a.sourceIp, count(a), max(a.riskScore) from Alert a where a.sourceIp is not null group by a.sourceIp order by count(a) desc")
    List<Object[]> topSourceIps(Pageable pageable);

    List<Alert> findTop10ByOrderByCreatedAtDesc();

    long countByAnomalyScoreIsNotNullAndDetectionMethodNotAndCreatedAtAfter(DetectionMethod method, Instant since);

    @Query("select a.createdAt from Alert a where a.createdAt >= :since")
    List<Instant> timestampsSince(@Param("since") Instant since);
}
