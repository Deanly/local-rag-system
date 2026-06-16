---
type: task
doc_id: T0023
title: local-cross-encoder-score-source
status: done
owner: Codex
created: 2026-06-16
updated: 2026-06-16
current_focus: Decision on whether current Local RAG can source true local cross-encoder `r_i` scores for ScoreGate.
completion_mode: decision-lock
related_control_plane: docs/design/control-plane.md
related_umbrella_project: docs/projects/P0001-local-rag-system.md
related_project: docs/projects/P0003-scoregate-adaptive-context-selection.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - https://arxiv.org/abs/2606.14269
  - docs/reports/2026-05-31-local-reranker-evaluation-decision.md
  - docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md
  - docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md
  - docs/design/retrieval-quality-improvement-design.md
  - docs/projects/P0003-scoregate-adaptive-context-selection.md
  - docs/evaluation/scoregate-offline-cases.json
  - .env.example
  - docker-compose.yml
quality_axes:
  - WHOLE
  - GOAL
  - EVIDENCE
  - SECURITY
tags:
  - docs/task
  - local-rag-system
  - retrieval-quality
  - scoregate
  - rerank
---

# T0023 local-cross-encoder-score-source

- Type: task
- Document ID: T0023
- Status: done
- Completion Mode: decision-lock
- Owner: Codex
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Decision on whether current Local RAG can source true local cross-encoder `r_i` scores for ScoreGate.
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: docs/projects/P0001-local-rag-system.md
- Related Project: docs/projects/P0003-scoregate-adaptive-context-selection.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`

## Purpose

이 task는 P0003의 runtime rollout blocker인 true local cross-encoder `r_i` score source를 현재 배포 프로파일에서 만들 수 있는지 결정한다.

## Task Placement Check

- 이 작업은 P0003의 score-source gate다.
- 별도 project가 아닌 이유는 decision 결과가 P0003 runtime rollout/no-ship 판단의 하위 증거이기 때문이다.

## Whole-System Anchor

Local RAG는 private source snippets를 hosted API로 보내면 안 된다. ScoreGate의 `r_i`는 deterministic governance score가 아니라 query와 chunk를 함께 읽는 true local cross-encoder relevance score여야 한다.

## Completion Mode Notes

Completion mode는 `decision-lock`이다. 완료 상태는 새 모델을 도입하는 것이 아니라, 현재 환경에서 ScoreGate runtime score source를 진행할지 보류할지 evidence-backed decision을 남기는 것이다.

## Committed Outcome

- 현재 배포 프로파일에서 true local cross-encoder `r_i` source가 있는지 확인한다.
- sidecar/library 후보와 운영 비용을 정리한다.
- runtime ScoreGate opt-in/default 진행 여부를 결정한다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Current environment score-source availability is checked. | Python/Java/Ollama/config/runtime evidence is recorded. |
| G2 | Candidate runtime boundary is decided. | Sidecar/library/no-go options are compared. |
| G3 | Runtime rollout decision is recorded. | P0003 can either proceed to debug/opt-in or close with no-ship evidence. |

## Scope

- Local environment/package availability check.
- Existing config/runtime surface check.
- Decision report.
- P0003 closeout alignment.

## Out Of Scope

- Downloading a model.
- Adding Python/ONNX runtime dependencies.
- Adding a reranker sidecar service.
- Runtime `rag_search` integration.

## References

- `docs/reports/2026-05-31-local-reranker-evaluation-decision.md`
- `docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md`
- `docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/projects/P0003-scoregate-adaptive-context-selection.md`

## Dependencies

- T0021 pure selector.
- T0022 offline snapshot evaluator.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Inspect current local ML/runtime surface | Done | 100% | Python ML packages are absent; Ollama API is unavailable on localhost in this shell; OMLX URL is empty. |
| W2 | Compare score-source boundaries | Done | 100% | Java library, Python sidecar, and OMLX-style sidecar were reviewed. |
| W3 | Record rollout decision | Done | 100% | Runtime ScoreGate is no-ship for this project closeout. |
| W4 | Verify and close | Done | 100% | Docs validators and focused tests passed after closeout. |

## Overall Progress

- 100%

## Completion Criteria

1. Evidence distinguishes true cross-encoder `r_i` from current deterministic `rerankScore`.
2. The current environment is checked for local model/runtime availability.
3. A runtime rollout/no-ship decision is recorded.
4. P0003 status is aligned with the decision.

## Completion Evidence

- `python3 --version`: Python 3.9.6.
- Python import checks: `torch=False`, `transformers=False`, `sentence_transformers=False`, `onnxruntime=False`.
- `curl -fsS --max-time 3 http://127.0.0.1:11434/api/tags`: failed to connect in this shell.
- `curl -fsS --max-time 3 http://127.0.0.1:42120/api/health`: Local RAG gateway/service health returned `UP`.
- `.env.example` has `LOCAL_RAG_OMLX_BASE_URL=` empty.
- `docker-compose.yml` only passes `RAG_OMLX_BASE_URL`; it does not define a cross-encoder sidecar.
- `docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md`.

## Outputs / Handoff

- Runtime ScoreGate integration is not implemented in P0003.
- Future work should start from a new task that introduces a local cross-encoder sidecar/library with explicit resource, timeout, fallback, and privacy boundaries.
- T0021/T0022 remain reusable once a real score source exists.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Runtime rollout must not weaken Local RAG reliability. | No uncalibrated runtime ScoreGate integration. |
| GOAL | P0003 must close with a real decision, not indefinite analysis. | No-ship report and project closeout. |
| EVIDENCE | Score-source claims must be based on current environment. | Package/config/runtime command evidence. |
| SECURITY | Private snippets must remain local-only. | No hosted reranker or hosted judge path introduced. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | Environment commands listed in Completion Evidence | No true local cross-encoder runtime is available in the current profile. |
| G2 | Done | No-ship report compares sidecar/library/no-go options | Sidecar remains future work, not a P0003 runtime patch. |
| G3 | Done | P0003 no-ship closeout | Runtime ScoreGate is not shipped. |

## Completion Guardrails

- Do not substitute deterministic governance score for `r_i`.
- Do not add hosted reranker calls.
- Do not add debug/opt-in runtime mode without real local score sourcing and calibration.

## Risks / Open Questions

- A future sidecar may be viable if the operator accepts model download, memory, startup, timeout, and maintenance cost.
- Threshold calibration remains unperformed because true `r_i` is unavailable.

## Status

- 2026-06-16: Task issued and completed. Current environment does not provide true local cross-encoder `r_i`; P0003 closes as runtime no-ship with reusable selector/evaluator artifacts.
