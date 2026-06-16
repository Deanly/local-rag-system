---
type: task
doc_id: T0024
title: local-cross-encoder-sidecar-proof
status: active
owner: Codex
created: 2026-06-16
updated: 2026-06-16
current_focus: "Add a local-only cross-encoder score source and ScoreGate opt-in proof path"
completion_mode: proof
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0003-scoregate-adaptive-context-selection
related_project: docs/projects/P0003-scoregate-adaptive-context-selection.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - https://huggingface.co/BAAI/bge-reranker-v2-m3
  - docs/projects/P0003-scoregate-adaptive-context-selection.md
  - docs/reports/2026-06-16-scoregate-before-after-comparison.md
  - docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md
  - docs/reports/2026-06-16-scoregate-sidecar-proof-smoke.md
  - docs/evaluation/scoregate-offline-cases.json
  - docs/evaluation/scoregate-runtime-probes.json
  - docs/bin/collect-scoregate-runtime-snapshot.py
  - services/reranker-sidecar/README.md
  - services/reranker-sidecar/app/main.py
  - services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/LocalRerankerClient.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/HttpLocalRerankerClient.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
quality_axes:
  - SECURITY
  - EVIDENCE
  - CONTRACT
  - WHOLE
tags:
  - docs/task
  - local-rag-system
  - retrieval-quality
  - scoregate
  - rerank
---

# T0024 local-cross-encoder-sidecar-proof

- Type: task
- Document ID: T0024
- Status: active
- Completion Mode: proof
- Owner: Codex
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Add a local-only cross-encoder score source and ScoreGate opt-in proof path
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0003-scoregate-adaptive-context-selection
- Related Project: docs/projects/P0003-scoregate-adaptive-context-selection.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`

## Purpose

This task turns the P0003 no-ship blocker into an executable proof path by adding a local-only cross-encoder `r_i` score source and wiring it to ScoreGate only behind an explicit opt-in/debug mode.

The goal is not to make ScoreGate the default. The goal is to create enough runtime substrate to collect real `s_i`/`r_i` snapshots, measure latency and quality, calibrate thresholds, and decide whether ScoreGate should later become an opt-in or default context-selection mode.

## Task Placement Check

- This is a follow-up to `P0003-scoregate-adaptive-context-selection`, not a new umbrella project.
- P0003 already shipped the pure selector and offline evaluator but closed runtime rollout as no-ship because true local cross-encoder `r_i` did not exist.
- T0024 owns only the score-source proof and opt-in measurement path needed before any runtime rollout decision.

## Whole-System Anchor

This task must preserve Local RAG's local-only and source-registry boundaries.

Invariants:

- Private source snippets must not be sent to hosted APIs.
- The cross-encoder must be local or LAN-local operator-owned infrastructure.
- Deterministic source/path/governance scores must stay separate from `r_i`.
- Default `rag_search` behavior must remain unchanged unless a later task explicitly promotes ScoreGate.
- If the reranker sidecar is unavailable, timeout, or unhealthy, the system must fall back to the current deterministic ranking path.

## Completion Mode Notes

Completion mode is `proof`. A successful closeout requires a working local-only score-source path and evidence. It does not require default runtime rollout.

## Committed Outcome

When this task is done:

- A local reranker sidecar contract exists for `POST /rerank`.
- `retrieval-service` can call that sidecar with top-N candidate snippets only when explicitly enabled.
- Real `r_i` scores are stored in result score maps or snapshot output separately from deterministic governance scores.
- ScoreGate can run in debug/opt-in mode without changing default public `rag_search` behavior.
- A before/after report compares fixed top-K, current deterministic ranking, and ScoreGate-with-real-`r_i`.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Provide a local-only cross-encoder score-source contract. | Optional reranker sidecar scaffold exposes health and rerank APIs, and the model/runtime is configurable. |
| G2 | Add a safe retrieval-service client with timeout and fallback. | Tests prove disabled/unavailable reranker leaves existing ranking behavior unchanged. |
| G3 | Add ScoreGate debug/opt-in path without default behavior change. | Request filters can enable debug/opt-in behavior, and score maps expose bucket/fusion/reason when used. |
| G4 | Produce real-candidate snapshots and calibration-ready evidence. | A report or generated JSON includes real `s_i`, real `r_i`, deterministic score components, retained count, latency, and quality metrics. |

## Scope

- Optional `reranker-sidecar` scaffold and API contract.
- Retrieval-service reranker client.
- ScoreGate opt-in/debug path inside retrieval-service.
- Tests for disabled, fallback, and opt-in scoring behavior.
- Evaluation/reporting hooks for real `s_i`/`r_i` snapshots.

## Out Of Scope

- Hosted reranker or hosted LLM judge integration.
- Default ScoreGate rollout.
- Reindexing source data.
- Replacing Weaviate first-stage retrieval.
- Full UI or dashboard.

## References

- `docs/projects/P0003-scoregate-adaptive-context-selection.md`
- `docs/reports/2026-06-16-scoregate-before-after-comparison.md`
- `docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md`
- `docs/reports/2026-06-16-scoregate-sidecar-proof-smoke.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/evaluation/scoregate-runtime-probes.json`
- `docs/bin/collect-scoregate-runtime-snapshot.py`
- `services/reranker-sidecar/README.md`
- `services/reranker-sidecar/app/main.py`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/LocalRerankerClient.java`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/HttpLocalRerankerClient.java`
- Hugging Face model card: `BAAI/bge-reranker-v2-m3`

## Dependencies

- Current Local RAG Docker Compose runtime.
- Current `retrieval-service` candidate window and audit fields.
- Local or LAN-local model runtime capacity.
- Operator acceptance of sidecar model download/startup/latency cost.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Document score-source proof plan | Done | 100% | Task document and active indexes define the proof boundary. |
| W2 | Add optional reranker sidecar scaffold | Done | 100% | `services/reranker-sidecar` exposes `/health` and `/rerank`; `BAAI/bge-reranker-v2-m3` is configurable. |
| W3 | Add retrieval-service reranker client | Done | 100% | `LocalRerankerClient` uses strict timeout/fallback and is disabled unless configured. |
| W4 | Add ScoreGate opt-in/debug path | Done | 100% | `filters.scoreGate=debug|on` can request ScoreGate; default search remains unchanged. |
| W5 | Add tests and validation | Done | 100% | Focused Maven tests, docs validators, compose config, profile config, py_compile, and diff check pass. |
| W6 | Collect proof evidence or record blocker | In Progress | 70% | Sidecar build/health/direct rerank smoke passed; live `rag_search` runtime snapshot still requires controlled retrieval-service restart. |

## Overall Progress

- 85%

## Completion Criteria

1. `personal-deploy`/default runtime behavior remains unchanged unless opt-in flags are used.
2. Reranker failures do not fail search requests.
3. Score maps distinguish `baseScore`, deterministic ranking score, cross-encoder `r_i`, ScoreGate bucket, fusion score, and decision reason.
4. A real local/LAN score source is measured or a blocker report records why it could not be run.
5. Validators and focused tests pass.

## Completion Evidence

Sufficient evidence:

- Focused Java tests for disabled/fallback/opt-in paths.
- Sidecar contract smoke or explicit runtime blocker report.
- Before/after report with real `r_i` if sidecar can run on this machine.

Insufficient evidence:

- Reusing deterministic `rerankScore` as `r_i`.
- Synthetic fixture-only metrics.
- Enabling ScoreGate by default before calibration.

## Outputs / Handoff

- Optional reranker sidecar scaffold.
- Retrieval-service client and opt-in ScoreGate integration.
- Calibration-ready snapshot/report artifacts.
- A clear next decision: keep disabled, ship debug/opt-in, or consider default rollout in a later task.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| SECURITY | Private snippets must stay local-only. | Sidecar endpoint is local/LAN-only and no hosted API is used. |
| EVIDENCE | ScoreGate rollout depends on real `r_i`. | Snapshot/report includes real cross-encoder scores. |
| CONTRACT | `rag_search` clients must not break. | Default request behavior and response compatibility are preserved. |
| WHOLE | Context selection must preserve source governance and multi-hop coverage. | Metrics include retained count, source accuracy, stale/noise suppression, and multi-hop coverage. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `services/reranker-sidecar/README.md`, `services/reranker-sidecar/app/main.py`, optional compose profile. | Model is configurable and local-only. |
| G2 | Done | `LocalRerankerClient`, `HttpLocalRerankerClient`, `mvn -q -pl services/retrieval-service -am test`. | Failure returns fallback metadata instead of failing search. |
| G3 | Done | `RetrievalServiceTests` covers debug no-reorder and `on` B3 rescue behavior. | Default requests still skip ScoreGate. |
| G4 | In Progress | `docs/reports/2026-06-16-scoregate-sidecar-proof-smoke.md`, `docs/evaluation/scoregate-runtime-probes.json`, `docs/bin/collect-scoregate-runtime-snapshot.py`. | Direct real `r_i` sidecar smoke passed; live `rag_search` snapshots still require retrieval-service restart/deploy with reranker enabled. |

## Completion Guardrails

- Do not ship default ScoreGate in this task.
- Do not use hosted APIs.
- Do not blend deterministic governance scores into `r_i`.
- Do not make source indexing or source registry changes for this proof.
- Do not require a sidecar for normal search.

## Risks / Open Questions

- `BAAI/bge-reranker-v2-m3` model download and first-start time may be large.
- CPU-only inference may be too slow for interactive use; LAN-local or Mac-optimized runtime may be needed.
- Need to choose a stable normalized `s_i` source across `hybrid`, `vector`, and `keyword`.
- Threshold calibration may show that ScoreGate should remain debug-only.

## Status

- 2026-06-16: Task issued after P0003 before/after review. User approved moving from planning into development, with documentation first.
- 2026-06-16: Added optional local reranker sidecar scaffold, retrieval-service timeout/fallback client, `filters.scoreGate=debug|on` opt-in integration, runtime probe definitions, and snapshot collector. Focused retrieval-service tests pass; real sidecar snapshot collection remains the next proof step before any default rollout.
- 2026-06-16: Built the CPU sidecar image after changing torch to `2.5.1+cpu`; direct sidecar health and rerank smoke passed. Warm two-candidate CPU rerank was about 5.983s and memory settled near 1.9GiB, so default rollout remains inappropriate pending runtime snapshots and tuning.
