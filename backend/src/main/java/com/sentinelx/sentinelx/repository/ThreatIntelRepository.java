package com.sentinelx.sentinelx.repository;

import com.sentinelx.sentinelx.entity.ThreatIntelRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThreatIntelRepository extends JpaRepository<ThreatIntelRecord, UUID> {

    Optional<ThreatIntelRecord> findFirstByIpOrderByQueriedAtDesc(String ip);

    List<ThreatIntelRecord> findTop20ByOrderByQueriedAtDesc();
}
