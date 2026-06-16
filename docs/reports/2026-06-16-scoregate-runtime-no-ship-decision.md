---
type: report
title: scoregate-runtime-no-ship-decision
status: done
owner: Codex
created: 2026-06-16
updated: 2026-06-16
current_focus: "P0003 runtime rollout decision for ScoreGate after selector, fixture, and local score-source checks"
report_type: scoregate-runtime-decision
related_project: docs/projects/P0003-scoregate-adaptive-context-selection.md
related_task:
  - docs/tasks/T0021-scoregate-offline-selector-experiment.md
  - docs/tasks/T0022-scoregate-offline-evaluation-fixture.md
  - docs/tasks/T0023-local-cross-encoder-score-source.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - https://arxiv.org/abs/2606.14269
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - docs/reports/2026-05-31-local-reranker-evaluation-decision.md
  - docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md
  - docs/evaluation/scoregate-offline-cases.json
  - docs/bin/validate-scoregate-offline.sh
  - services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateOfflineEvaluationTests.java
  - .env.example
  - docker-compose.yml
tags:
  - docs/report
  - local-rag-system
  - retrieval-quality
  - scoregate
  - rerank
---

# scoregate-runtime-no-ship-decision

- Type: report
- Status: done
- Owner: Codex
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: P0003 runtime rollout decision for ScoreGate after selector, fixture, and local score-source checks
- Report Type: scoregate-runtime-decision
- Related Project: `docs/projects/P0003-scoregate-adaptive-context-selection.md`
- Related Task:
  - `docs/tasks/T0021-scoregate-offline-selector-experiment.md`
  - `docs/tasks/T0022-scoregate-offline-evaluation-fixture.md`
  - `docs/tasks/T0023-local-cross-encoder-score-source.md`
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`

## Summary

Do not ship ScoreGate into runtime in P0003.

What is complete:

- ScoreGate was reviewed against the arXiv paper, assistant wiki scrap, retrieval-quality design, and 2026-05-25 baseline.
- P0003 was issued as a new exception-branch project.
- `ScoreGateCandidateSelector` exists as a pure Java selector.
- Offline snapshot fixture, validator, and report exist.

What blocks runtime rollout:

- Current Local RAG still does not have a true local cross-encoder reranker score source for `r_i`.
- Current deterministic `rerankScore` is source/path/metadata/governance composite and must not be used as cross-encoder relevance.
- Current environment lacks the local Python/ONNX model runtime dependencies needed for a sidecar/library proof.

Therefore P0003 closes as an evidence-backed no-ship decision for runtime ScoreGate. The reusable artifacts remain in place for a future task that introduces a real local cross-encoder score source.

## Scope

Included:

- current environment score-source inspection,
- runtime rollout decision,
- handoff for future sidecar/library work.

Excluded:

- model download,
- sidecar implementation,
- threshold calibration,
- debug/audit runtime output,
- default `rag_search` behavior change.

## Inputs

Source-backed inputs:

- arXiv ScoreGate paper, v1 dated 2026-06-12,
- assistant wiki paper scrap,
- `docs/design/retrieval-quality-improvement-design.md`,
- `docs/reports/2026-05-25-retrieval-quality-baseline.md`,
- `docs/reports/2026-05-31-local-reranker-evaluation-decision.md`,
- `docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md`.

Environment checks:

```text
python3 --version -> Python 3.9.6
torch=False
transformers=False
sentence_transformers=False
onnxruntime=False
curl http://127.0.0.1:11434/api/tags -> connection failed in this shell
curl http://127.0.0.1:42120/api/health -> Local RAG gateway/services UP
.env.example -> LOCAL_RAG_OMLX_BASE_URL is empty
docker-compose.yml -> passes RAG_OMLX_BASE_URL but defines no cross-encoder sidecar
```

## Findings

### F1. The Selector Is Ready, But `r_i` Is Not

T0021 implemented the algorithmic selection layer and T0022 proved it against snapshot cases. This is enough to preserve the implementation contract, but not enough to run ScoreGate against real corpus traffic.

### F2. Current Deterministic `rerankScore` Is Not Cross-Encoder Relevance

The current score is intentionally built from first-stage score, source role, path, match, and governance weights. It is valuable for Local RAG ranking, but using it as `r_i` would invalidate the paper's premise.

### F3. No Current Local Cross-Encoder Runtime Surface Exists

The current repo/runtime has no Python ML packages, no ONNX runtime, no configured OMLX endpoint, and no compose sidecar for cross-encoder scoring. Adding one would be a real new runtime surface with model packaging, memory, startup, latency, timeout, and fallback policy.

### F4. Runtime ScoreGate Would Be Premature

Without true `r_i`, threshold calibration cannot be performed. Without calibration, debug/audit or opt-in runtime mode would either be misleading or would silently depend on paper thresholds.

## Decision

P0003 does not ship runtime ScoreGate.

Closed state:

- Keep `ScoreGateCandidateSelector`.
- Keep offline snapshot fixture and validator.
- Keep public `rag_search` behavior unchanged.
- Do not add debug/audit mode or opt-in runtime mode in this project.
- Reopen with a new task only when a local cross-encoder sidecar/library is explicitly accepted and measured.

## Follow-Up Promotion

Future task candidate:

- `T0024-local-cross-encoder-sidecar-proof`: introduce a local-only cross-encoder score service or library, define timeout/fallback/resource policy, collect real `r_i` snapshots, and re-run `validate-scoregate-offline.sh`.

Do not start runtime ScoreGate integration before that proof exists.

## Status

- 2026-06-16: Runtime no-ship decision recorded for P0003.
