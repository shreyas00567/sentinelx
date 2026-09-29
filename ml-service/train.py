import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import numpy as np

from ae_numpy import NumpyAutoencoder, keras_to_numpy
from features import FEATURES, MODEL_VERSION

MODELS_DIR = Path(__file__).parent / "models"
SIZES = (8, 64, 32, 8, 32, 64, 8)


def gen_synthetic(n_normal=20000, n_anom=2500, seed=42):
    rng = np.random.default_rng(seed)

    def hour_features(hour):
        frac = (hour * 60) / 1440.0
        return np.sin(2 * np.pi * frac), np.cos(2 * np.pi * frac)

    normals = []
    for _ in range(n_normal):
        hour = int(np.clip(rng.normal(13.5, 3.2), 7, 19))
        hs, hc = hour_features(hour)
        off = 1 if rng.random() < 0.04 else 0
        if off:
            hs, hc = hour_features(int(rng.integers(20, 24)))
        normals.append([
            hs, hc, off,
            float(rng.choice([0, 0, 0, 0, 1])),
            max(0.3, rng.normal(6.0, 2.5)),
            float(np.exp(rng.normal(7.3, 0.6))),
            float(rng.integers(1, 6)),
            float(np.clip(rng.normal(18, 9), 1, 60)),
        ])

    anomalies = []
    profiles = ["night_owl", "stuffing", "scanner", "exfil"]
    for i in range(n_anom):
        p = profiles[i % len(profiles)]
        if p == "night_owl":
            hs, hc = hour_features(int(rng.integers(0, 5)))
            anomalies.append([hs, hc, 1, 0, rng.uniform(1, 4), np.exp(rng.normal(7.5, 0.5)),
                              rng.integers(1, 5), rng.uniform(10, 40)])
        elif p == "stuffing":
            hour = int(rng.choice([2, 3, 4, 14]))
            hs, hc = hour_features(hour)
            anomalies.append([hs, hc, 1 if hour < 6 else 0, rng.uniform(6, 30),
                              rng.uniform(8, 25), np.exp(rng.normal(6.8, 0.5)),
                              rng.integers(1, 4), rng.uniform(1, 15)])
        elif p == "scanner":
            anomalies.append([0, 1, 0, 0, rng.uniform(80, 300), rng.normal(400, 100),
                              rng.integers(25, 90), rng.uniform(30, 120)])
        else:
            hs, hc = hour_features(int(rng.integers(1, 5)))
            anomalies.append([hs, hc, 1, 0, rng.uniform(2, 8), rng.uniform(500000, 6000000),
                              rng.integers(2, 8), rng.uniform(60, 240)])

    Xn = np.array(normals, dtype=np.float64)
    Xa = np.array(anomalies, dtype=np.float64)
    y = np.concatenate([np.zeros(n_normal), np.ones(n_anom)])
    return Xn, Xa, y


def maybe_load_csv(path):
    import pandas as pd
    df = pd.read_csv(path)
    missing = [c for c in FEATURES if c not in df.columns]
    if missing:
        raise SystemExit(f"CSV is missing feature columns: {missing}. Expected columns: {FEATURES} (+ optional 'label')")
    label_col = "label" if "label" in df.columns else None
    normal = df[df[label_col] == 0][FEATURES].to_numpy(dtype=np.float64) if label_col else df[FEATURES].to_numpy(dtype=np.float64)
    anom = df[df[label_col] == 1][FEATURES].to_numpy(dtype=np.float64) if label_col else np.empty((0, len(FEATURES)))
    return normal, anom


def main():
    ap = argparse.ArgumentParser(description="Train the SentinelX autoencoder anomaly detector")
    ap.add_argument("--epochs", type=int, default=100)
    ap.add_argument("--dataset", type=str, default=None,
                    help="Optional CSV with FEATURE columns and a 0/1 'label' column (e.g., a CIC-IDS2017 subset)")
    ap.add_argument("--no-keras", action="store_true", help="Force the pure-NumPy trainer")
    args = ap.parse_args()

    print(f"SentinelX Autoencoder training | features={len(FEATURES)}")

    if args.dataset:
        X_normal_all, X_anom_all = maybe_load_csv(args.dataset)
        print(f"Loaded CSV: {len(X_normal_all)} normal, {len(X_anom_all)} anomalous rows")
    else:
        X_normal_all, X_anom_all, _ = gen_synthetic()
        print("Generated synthetic behavioral dataset "
              f"({len(X_normal_all)} normal / {len(X_anom_all)} anomalous)")

    rng = np.random.default_rng(123)
    perm = rng.permutation(len(X_normal_all))
    X_normal_all = X_normal_all[perm]
    n_train = int(0.8 * len(X_normal_all))
    x_train_raw = X_normal_all[:n_train]
    x_val_raw = X_normal_all[n_train:]

    mean = x_train_raw.mean(axis=0)
    scale = x_train_raw.std(axis=0)
    scale[scale < 1e-8] = 1.0

    def apply_scale(x):
        return (x - mean) / scale

    x_train = apply_scale(x_train_raw)
    x_val = apply_scale(x_val_raw)

    backend = "numpy"
    keras_model = None
    if not args.no_keras:
        try:
            from ae_keras import train_keras
            keras_model = train_keras(x_train, x_val, input_dim=len(FEATURES), epochs=args.epochs)
            backend = "tensorflow/keras"
        except Exception as ex:
            print(f"TensorFlow unavailable ({type(ex).__name__}: {ex}); using pure-NumPy trainer")

    if keras_model is not None:
        arrays, sizes = keras_to_numpy(keras_model)
        val_err = np.mean((keras_model.predict(x_val, verbose=0) - x_val) ** 2, axis=1)
    else:
        model = NumpyAutoencoder(layer_sizes=SIZES)
        model.fit(x_train, x_val, epochs=args.epochs, batch_size=256, lr=1e-3, patience=10)
        arrays, sizes = model.export(), list(model.sizes)
        val_err = model.reconstruction_error(x_val)

    thr_mean = float(val_err.mean() + 3 * val_err.std())
    thr_p99 = float(np.percentile(val_err, 99))
    threshold = thr_mean
    print(f"\nthreshold (mean+3std) = {thr_mean:.6f}   (p99 = {thr_p99:.6f})")

    metrics = {}
    if len(X_anom_all) > 0:
        from sklearn.metrics import confusion_matrix, precision_score, recall_score, f1_score, accuracy_score
        test_norm = apply_scale(x_val_raw[:2000]) if len(x_val_raw) > 2000 else x_val
        test_anom = apply_scale(X_anom_all)
        errs_n = _errors(arrays, sizes, test_norm)
        errs_a = _errors(arrays, sizes, test_anom)
        y_true = np.concatenate([np.zeros(len(errs_n)), np.ones(len(errs_a))])
        errs = np.concatenate([errs_n, errs_a])
        y_pred = (errs > threshold).astype(int)
        cm = confusion_matrix(y_true, y_pred).tolist()
        metrics = {
            "precision": round(float(precision_score(y_true, y_pred, zero_division=0)), 4),
            "recall": round(float(recall_score(y_true, y_pred)), 4),
            "f1_score": round(float(f1_score(y_true, y_pred)), 4),
            "accuracy": round(float(accuracy_score(y_true, y_pred)), 4),
            "false_positive_rate": round(float(cm[0][1] / max(1, cm[0][0] + cm[0][1])), 4),
            "confusion_matrix": {"tn": cm[0][0], "fp": cm[0][1], "fn": cm[1][0], "tp": cm[1][1]},
            "eval_set": {"normal": int(len(errs_n)), "anomalous": int(len(errs_a))},
        }
        print("\nEvaluation on held-out mixed set:")
        for k, v in metrics.items():
            print(f"  {k}: {v}")

    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    artifact = {**arrays,
                "scaler_mean": mean,
                "scaler_scale": scale,
                "threshold": np.array(threshold),
                "sizes": np.array(sizes)}
    np.savez(MODELS_DIR / "autoencoder.npz", **artifact)

    if keras_model is not None:
        try:
            keras_model.save(MODELS_DIR / "autoencoder.keras")
        except Exception as ex:
            print(f"Skipping .keras export: {ex}")

    metadata = {
        "model_version": MODEL_VERSION,
        "backend": backend,
        "features": FEATURES,
        "layer_sizes": sizes,
        "threshold": threshold,
        "threshold_strategy": "mean + 3*std of validation reconstruction error",
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "metrics": metrics,
    }
    (MODELS_DIR / "metadata.json").write_text(json.dumps(metadata, indent=2))

    print(f"\nSaved artifacts to {MODELS_DIR}/")
    print("Done.")


def _errors(arrays, sizes, x):
    model = NumpyAutoencoder.load(arrays, sizes)
    return model.reconstruction_error(x)


if __name__ == "__main__":
    main()
