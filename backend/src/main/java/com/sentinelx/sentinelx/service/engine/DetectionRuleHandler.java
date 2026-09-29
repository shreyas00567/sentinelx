package com.sentinelx.sentinelx.service.engine;

import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import com.sentinelx.sentinelx.entity.SecurityEvent;

import java.util.List;
import java.util.Optional;

public interface DetectionRuleHandler {

    String ruleKey();

    Optional<DetectionResult> evaluate(SecurityEvent event,
                                       DetectionRuleEntity config,
                                       TrackerStats stats);
}
