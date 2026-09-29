package com.sentinelx.sentinelx.service.engine;

import com.sentinelx.sentinelx.entity.DetectionRuleEntity;
import com.sentinelx.sentinelx.repository.DetectionRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);
    private static final long CONFIG_CACHE_MS = 15_000;

    private final DetectionRuleRepository ruleRepository;
    private final EventTracker tracker;
    private final Map<String, DetectionRuleHandler> handlers = new HashMap<>();
    private volatile List<DetectionRuleEntity> cachedRules = List.of();
    private volatile Instant cacheLoadedAt = Instant.EPOCH;

    public RuleEngineService(DetectionRuleRepository ruleRepository,
                             EventTracker tracker,
                             List<DetectionRuleHandler> handlerList) {
        this.ruleRepository = ruleRepository;
        this.tracker = tracker;
        for (DetectionRuleHandler h : handlerList) {
            handlers.put(h.ruleKey(), h);
        }
    }

    public List<DetectionResult> evaluate(com.sentinelx.sentinelx.entity.SecurityEvent event) {
        List<DetectionResult> results = new ArrayList<>();
        for (DetectionRuleEntity cfg : activeRules()) {
            DetectionRuleHandler handler = handlers.get(cfg.getRuleKey());
            if (handler == null || !cfg.isEnabled()) {
                continue;
            }
            try {
                TrackerStats stats = tracker.stats(event.getSourceIp());
                handler.evaluate(event, cfg, stats).ifPresent(results::add);
            } catch (Exception ex) {
                log.warn("Rule {} failed: {}", cfg.getRuleKey(), ex.getMessage());
            }
        }
        return results;
    }

    public void record(com.sentinelx.sentinelx.entity.SecurityEvent event) {
        tracker.record(event);
    }

    public void clearCaches() {
        cacheLoadedAt = Instant.EPOCH;
        tracker.clear();
    }

    private List<DetectionRuleEntity> activeRules() {
        if (Duration.between(cacheLoadedAt, Instant.now()).toMillis() > CONFIG_CACHE_MS) {
            cachedRules = ruleRepository.findAllByOrderByIdAsc();
            cacheLoadedAt = Instant.now();
        }
        return cachedRules;
    }
}
