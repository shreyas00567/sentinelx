package com.sentinelx.sentinelx.repository;

import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DetectionRuleRepository extends JpaRepository<DetectionRuleEntity, Long> {

    Optional<DetectionRuleEntity> findByRuleKey(String ruleKey);

    List<DetectionRuleEntity> findAllByOrderByIdAsc();
}
