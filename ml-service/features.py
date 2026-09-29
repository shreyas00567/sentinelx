FEATURES = [
    "hour_sin",
    "hour_cos",
    "is_off_hours",
    "failed_attempts",
    "request_rate_per_min",
    "bytes_sent",
    "distinct_endpoints_1h",
    "session_duration_min",
]

MODEL_VERSION = "1.0"


def vectorize(features: dict) -> list:
    missing = [k for k in FEATURES if k not in features]
    if missing:
        raise ValueError(f"Missing features: {missing}")
    return [float(features[k]) for k in FEATURES]


LOG_COLUMNS = {c: i for i, c in enumerate(FEATURES)}
