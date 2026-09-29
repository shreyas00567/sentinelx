package com.sentinelx.sentinelx.repository;

import com.sentinelx.sentinelx.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, UUID>, JpaSpecificationExecutor<SecurityEvent> {

    long countByTimestampAfter(Instant since);

    @Query("select s.timestamp from SecurityEvent s where s.timestamp >= :since")
    List<Instant> findTimestampsSince(@Param("since") Instant since);
}
