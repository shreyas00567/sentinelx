import json
import math
from contextlib import asynccontextmanager
from pathlib import Path

import numpy as np
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from ae_numpy import NumpyAutoencoder
from features import FEATURES, MODEL_VERSION

MODELS_DIR = Path(__file__).parent / "models"


class PredictRequest(BaseModel):
    features: dict = Field(..., description=f"Feature map with keys: {FEATURES}")


class Model:
    def __init__(self):
        self.ready = False
        self.error = None
        self.model = None
        self.mean = None
        self.scale = None
        self.threshold = 0.0
        self.sizes = None
        self.metadata = {}
        self.load()

    def load(self):
        try:
            artifact = np.load(MODELS_DIR / "autoencoder.npz")
            sizes = [int(s) for s in artifact["sizes"]]
            self.sizes = sizes
            self.mean = artifact["scaler_mean"]
            self.scale = artifact["scaler_scale"]
            self.threshold = float(artifact["threshold"])
            arrays = {k: artifact[k] for k in artifact.files if k.startswith(("W", "b"))}
            self.model = NumpyAutoencoder.load(arrays, sizes)
            meta_path = MODELS_DIR / "metadata.json"
            if meta_path.exists():
                self.metadata = json.loads(meta_path.read_text())
            self.ready = True
        except Exception as ex:
            self.error = f"{type(ex).__name__}: {ex}"


state = {"model": None}


@asynccontextmanager
async def lifespan(app: FastAPI):
    state["model"] = Model()
    if state["model"].ready:
        print("Model loaded. threshold =", state["model"].threshold)
    else:
        print("Model NOT loaded:", state["model"].error)
    yield


app = FastAPI(title="SentinelX ML Service", version=MODEL_VERSION, lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


def anomaly_score(err: float, threshold: float) -> float:
    ratio = err / threshold if threshold > 0 else 0.0
    return round(1.0 / (1.0 + math.exp(-5.0 * (ratio - 1.0))), 4)


@app.post("/predict")
def predict(req: PredictRequest):
    m: Model = state["model"]
    if not m.ready:
        raise HTTPException(status_code=503, detail="Model not loaded. Run train.py first.")
    missing = [k for k in FEATURES if k not in req.features]
    if missing:
        raise HTTPException(status_code=400, detail=f"Missing features: {missing}")
    x = np.array([[float(req.features[k]) for k in FEATURES]])
    xs = (x - m.mean) / m.scale
    err = float(m.model.reconstruction_error(xs)[0])
    score = anomaly_score(err, m.threshold)
    return {
        "anomaly": bool(score >= 0.5),
        "anomaly_score": score,
        "raw_error": round(err, 6),
        "threshold": round(m.threshold, 6),
        "model_version": MODEL_VERSION,
    }


@app.get("/health")
def health():
    m: Model = state["model"]
    return {
        "status": "ok" if m.ready else "degraded",
        "model_loaded": m.ready,
        "model_version": MODEL_VERSION,
        "detail": m.error,
    }


@app.get("/model/info")
def model_info():
    m: Model = state["model"]
    return {
        "model_version": MODEL_VERSION,
        "features": FEATURES,
        "layer_sizes": m.sizes,
        "threshold": m.threshold,
        "metrics": m.metadata.get("metrics", {}),
        "backend": m.metadata.get("backend"),
        "trained_at": m.metadata.get("trained_at"),
    }
