---
type: report
title: scoregate-sidecar-proof-smoke
status: done
created: 2026-06-16
updated: 2026-06-16
current_focus: "Local reranker sidecar build and direct rerank smoke for T0024"
report_type: scoregate-sidecar-proof-smoke
related_project: docs/projects/P0003-scoregate-adaptive-context-selection.md
related_tasks:
  - docs/tasks/T0024-local-cross-encoder-sidecar-proof.md
source_refs:
  - services/reranker-sidecar/README.md
  - services/reranker-sidecar/app/main.py
  - services/reranker-sidecar/requirements.txt
  - docker-compose.yml
  - docs/evaluation/scoregate-runtime-probes.json
  - docs/bin/collect-scoregate-runtime-snapshot.py
tags:
  - docs/report
  - local-rag-system
  - retrieval-quality
  - scoregate
  - rerank
---

# scoregate-sidecar-proof-smoke

- Type: report
- Status: done
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Local reranker sidecar build and direct rerank smoke for T0024
- Report Type: scoregate-sidecar-proof-smoke
- Related Project: `docs/projects/P0003-scoregate-adaptive-context-selection.md`
- Related Tasks:
  - `docs/tasks/T0024-local-cross-encoder-sidecar-proof.md`

## Summary

The optional T0024 reranker sidecar builds and can return normalized local cross-encoder scores from `BAAI/bge-reranker-v2-m3`.

This is sidecar-level proof only. It does not prove live Local RAG `rag_search` improvement yet because the deployed retrieval-service container was not restarted onto this development build during the smoke. Runtime ScoreGate snapshots still require a controlled deployment or dev stack with `LOCAL_RAG_RERANKER_ENABLED=true`.

## Evidence

Commands run:

```bash
docker compose --profile scoregate build reranker-sidecar
docker compose --profile scoregate up -d reranker-sidecar
curl -fsS http://127.0.0.1:42145/health
curl -fsS -X POST http://127.0.0.1:42145/rerank ...
docker compose --profile scoregate stop reranker-sidecar
```

Observed health before model load:

```json
{"status":"UP","model":"BAAI/bge-reranker-v2-m3","ready":false,"loadError":null}
```

Observed rerank smoke:

```json
{
  "model": "BAAI/bge-reranker-v2-m3",
  "normalized": true,
  "scores": [
    {"id": "release", "score": 1.0},
    {"id": "unrelated", "score": 0.0}
  ]
}
```

Observed health after model load:

```json
{"status":"UP","model":"BAAI/bge-reranker-v2-m3","ready":true,"loadError":null}
```

Warm rerank latency for the two-candidate smoke was about `5.983s` total through `curl` on CPU. Docker stats after warm load showed about `1.921GiB` memory usage and about `1.57GB` network receive during image/model setup.

## Interpretation

- The true local `r_i` score-source blocker is now technically addressable.
- CPU-only latency is too high to promote ScoreGate to default without batching, candidate-window tuning, or a faster runtime.
- Debug/opt-in mode is still the right rollout posture.
- Runtime quality reporting should next use `docs/bin/collect-scoregate-runtime-snapshot.py` against a restarted retrieval-service build configured with `LOCAL_RAG_RERANKER_ENABLED=true`.

## Limitations

- No source data was reindexed.
- No deployed retrieval-service container was restarted for this smoke.
- No Local RAG runtime before/after snapshot was collected from live `rag_search`.
- The smoke used two synthetic candidate strings, not full retrieved chunks.

## Status

- 2026-06-16: Sidecar image build, health, direct rerank, warm latency, and stop completed. Runtime `rag_search` proof remains pending controlled deployment or dev-stack restart.
