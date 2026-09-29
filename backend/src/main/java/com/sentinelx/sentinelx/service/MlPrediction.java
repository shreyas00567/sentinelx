package com.sentinelx.sentinelx.service;

public record MlPrediction(
        boolean anomaly,
        double anomalyScore,
        double rawError,
        double threshold,
        String modelVersion) {
}
