import inspect
import os
import threading
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


DEFAULT_MODEL = "BAAI/bge-reranker-v2-m3"
MODEL_NAME = os.getenv("LOCAL_RAG_RERANKER_MODEL", DEFAULT_MODEL)
MAX_LENGTH = int(os.getenv("LOCAL_RAG_RERANKER_MAX_LENGTH", "1024"))
USE_FP16 = os.getenv("LOCAL_RAG_RERANKER_USE_FP16", "false").strip().lower() in {
    "1",
    "true",
    "yes",
    "on",
}

app = FastAPI(title="Local RAG reranker sidecar", version="0.1.0")
_model_lock = threading.Lock()
_reranker = None
_load_error: Optional[str] = None


class RerankCandidate(BaseModel):
    id: str = Field(min_length=1)
    text: str = ""


class RerankRequest(BaseModel):
    query: str = ""
    candidates: List[RerankCandidate] = Field(default_factory=list)
    normalize: bool = True
    batchSize: int = Field(default=8, ge=1, le=64)
    maxLength: Optional[int] = Field(default=None, ge=64, le=8192)


class RerankScore(BaseModel):
    id: str
    score: float


class RerankResponse(BaseModel):
    model: str
    normalized: bool
    scores: List[RerankScore]


def _load_reranker():
    global _reranker, _load_error
    if _reranker is not None:
        return _reranker
    with _model_lock:
        if _reranker is not None:
            return _reranker
        try:
            from FlagEmbedding import FlagReranker

            _reranker = FlagReranker(MODEL_NAME, use_fp16=USE_FP16)
            _load_error = None
            return _reranker
        except Exception as exc:  # pragma: no cover - exercised in container runtime
            _load_error = str(exc)
            raise


@app.get("/health")
def health():
    return {
        "status": "UP" if _load_error is None else "DEGRADED",
        "model": MODEL_NAME,
        "ready": _reranker is not None and _load_error is None,
        "loadError": _load_error,
    }


@app.post("/rerank", response_model=RerankResponse)
def rerank(request: RerankRequest):
    if not request.candidates:
        return RerankResponse(model=MODEL_NAME, normalized=request.normalize, scores=[])
    if not request.query.strip():
        raise HTTPException(status_code=400, detail="query must not be blank")

    pairs = [[request.query, candidate.text or ""] for candidate in request.candidates]
    try:
        reranker = _load_reranker()
        raw_scores = _compute_score(reranker, pairs, request)
    except Exception as exc:  # pragma: no cover - exercised in container runtime
        raise HTTPException(status_code=503, detail=f"reranker unavailable: {exc}") from exc

    if hasattr(raw_scores, "tolist"):
        raw_scores = raw_scores.tolist()
    if isinstance(raw_scores, tuple):
        raw_scores = list(raw_scores)
    if not isinstance(raw_scores, list):
        raw_scores = [raw_scores]
    if len(raw_scores) != len(request.candidates):
        raise HTTPException(status_code=502, detail="reranker returned mismatched score count")

    scores = [
        RerankScore(id=candidate.id, score=_score(raw_score, request.normalize))
        for candidate, raw_score in zip(request.candidates, raw_scores)
    ]
    return RerankResponse(model=MODEL_NAME, normalized=request.normalize, scores=scores)


def _score(raw_score, normalized: bool) -> float:
    value = float(raw_score)
    if normalized:
        return min(1.0, max(0.0, value))
    return value


def _compute_score(reranker, pairs, request: RerankRequest):
    signature = inspect.signature(reranker.compute_score)
    kwargs = {}
    if "normalize" in signature.parameters:
        kwargs["normalize"] = request.normalize
    if "batch_size" in signature.parameters:
        kwargs["batch_size"] = request.batchSize
    if "max_length" in signature.parameters:
        kwargs["max_length"] = request.maxLength or MAX_LENGTH
    return reranker.compute_score(pairs, **kwargs)
