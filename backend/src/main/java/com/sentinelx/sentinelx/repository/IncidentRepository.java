package com.sentinelx.sentinelx.repository;

import com.sentinelx.sentinelx.entity.Incident;
import com.sentinelx.sentinelx.entity.IncidentStatus;
import com.sentinelx.sentinelx.entity.AttackType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    long countByStatus(IncidentStatus status);

    long countByCreatedAtAfter(Instant since);

    @Query("select i from Incident i where i.sourceIp = :ip and i.attackType = :type and i.status in :statuses and i.createdAt >= :since")
    List<Incident> findOpenSimilar(@Param("ip") String ip,
                                   @Param("type") AttackType type,
                                   @Param("statuses") List<IncidentStatus> statuses,
                                   @Param("since") Instant since);

    @Query("select i.createdAt, i.resolvedAt from Incident i where i.resolvedAt is not null and i.resolvedAt >= :since")
    List<Object[]> resolvedSince(@Param("since") Instant since);

    @Query("select i.createdAt from Incident i where i.createdAt >= :since")
    List<Instant> createdTimestampsSince(@Param("since") Instant since);
}
