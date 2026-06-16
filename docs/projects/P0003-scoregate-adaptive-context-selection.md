---
type: project
doc_id: P0003
title: scoregate-adaptive-context-selection
status: done
project_role: exception-branch
umbrella_initiative: Local RAG ScoreGate Adaptive Context Selection
parent_umbrella_project: docs/projects/P0001-local-rag-system.md
release_version: 1.2.0
completion_mode: functional
owner: Codex
created: 2026-06-16
updated: 2026-06-16
current_focus: Runtime no-ship decision recorded; selector and offline evaluator remain as reusable artifacts.
related_control_plane: docs/design/control-plane.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - https://arxiv.org/abs/2606.14269
  - ~/Workspace/personal-assistant-wiki/inbox/2026-06-15-agent-system-paper-scrap.md
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - docs/design/retrieval-quality-improvement-design.md
  - docs/tasks/T0021-scoregate-offline-selector-experiment.md
  - docs/tasks/T0022-scoregate-offline-evaluation-fixture.md
  - docs/tasks/T0023-local-cross-encoder-score-source.md
  - docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md
  - docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md
  - docs/evaluation/scoregate-offline-cases.json
  - docs/bin/validate-scoregate-offline.sh
  - services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateCandidateSelectorTests.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateOfflineEvaluationTests.java
quality_axes:
  - WHOLE
  - SCOPE
  - EVIDENCE
  - HANDOFF
tags:
  - docs/project
  - local-rag-system
  - retrieval-quality
  - scoregate
  - rerank
---

# P0003 scoregate-adaptive-context-selection

- Type: project
- Document ID: P0003
- Status: done
- Project Role: exception-branch
- Umbrella Initiative: Local RAG ScoreGate Adaptive Context Selection
- Parent Umbrella Project: docs/projects/P0001-local-rag-system.md
- Release Version: 1.2.0
- Completion Mode: functional
- Owner: Codex
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Runtime no-ship decision recorded; selector and offline evaluator remain as reusable artifacts.
- Related Control Plane: docs/design/control-plane.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`

## Purpose

이 project는 ScoreGate를 Local RAG의 retrieval 자체가 아니라 final context selection 품질을 개선하는 후보로 도입 검토하고, calibration evidence가 생기기 전에는 default runtime behavior를 바꾸지 않는 경로를 소유한다.

ScoreGate의 핵심은 fixed top-K selection 대신 normalized bi-encoder similarity `s_i`와 local cross-encoder relevance `r_i`를 함께 보고, 특히 low-vector/high-reranker bucket인 B3 chunk를 구제하는 것이다. Local RAG에서는 한국어 자연어 query와 영어 문서/운영 용어 사이의 lexical mismatch가 이 project의 직접 동기다.

## Umbrella Lineage

- 이 문서는 `P0001-local-rag-system` functional baseline 이후의 exception-branch project다.
- `P0002-retrieval-governance-hardening`은 deterministic governance ranking과 metadata-aware retrieval을 닫았고, separate model reranker를 release path에서 제외했다.
- P0003는 그 다음 단계인 adaptive context selection experiment, local cross-encoder score sourcing, calibration, opt-in rollout 판단을 별도 delivery boundary로 소유한다.

## Project Issuance Check

- 2026-06-16 사용자 요청으로 "신규 프로젝트"로 작업하도록 명시되었다.
- 기존 P0001/P0002 task로만 처리하면 ScoreGate 도입 검토, local cross-encoder scoring, offline calibration, runtime opt-in 판단이 섞여 human-facing 상태를 읽기 어렵다.
- 별도 project로 두면 "selector substrate는 구현됨", "cross-encoder score source는 미정", "runtime default는 아직 아님"이라는 상태를 명확히 유지할 수 있다.

## Whole-System Anchor

이 project는 Local RAG의 local-only invariant, source registry boundary, stable public `rag_search` contract를 보존해야 한다.

깨면 안 되는 경계:

- private source content를 hosted API reranker나 hosted LLM으로 보내지 않는다.
- 현재 `RetrievalRanker`의 deterministic `rerankScore`를 논문이 말하는 cross-encoder `r_i`로 간주하지 않는다.
- ScoreGate threshold는 paper reference value를 그대로 production default로 승격하지 않는다.
- ranking/context selection 변경은 versioned fixture와 before/after metric으로 검증한다.

## Completion Mode Notes

Completion mode는 `functional`이다. 이 project가 닫히려면 단순 분석 문서가 아니라 Local RAG가 사용할 수 있는 calibrated context-selection path 또는 명시적 no-ship decision이 남아야 한다.

## Committed Outcome

P0003가 `done`이면 다음 중 하나가 성립해야 한다.

- Offline evaluation에서 ScoreGate가 current default보다 context token count를 줄이면서 hit@1/hit@5/MRR/source accuracy/staleness/multi-hop coverage를 보존하거나 개선하고, opt-in runtime/debug mode로 사용할 수 있다.
- 또는 local-only cross-encoder score source, threshold calibration, latency/resource budget 중 하나가 충족되지 않아 ScoreGate runtime 도입을 보류한다는 evidence-backed no-ship decision이 남는다.

P0003 is the official `1.2.0` release line. It is backward-compatible with P0002 `1.1.0`, keeps runtime `rag_search` behavior unchanged, and advances runtime artifact versions to `1.2.0` so the ScoreGate selector/evaluator and no-ship decision are preserved as a tagged release.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | ScoreGate selector substrate를 Local RAG codebase에 runtime-default 변경 없이 추가한다. | Normalized `s_i`/`r_i` input, bucket, fusion, decision reason, MAX-K cap을 가진 순수 selector와 focused tests가 있다. |
| G2 | Local RAG corpus에 맞는 offline calibration experiment를 만든다. | candidateLimit 30/40, Korean natural language, English filename/task-id, primary source, stale demotion, broad/multi-hop cases를 비교하는 report가 있다. |
| G3 | `r_i` score source를 local-only로 결정한다. | local cross-encoder candidate, sidecar/library boundary, latency/resource budget, fallback/no-go decision이 문서화된다. |
| G4 | Runtime rollout 판단을 evidence로 닫는다. | debug/audit 또는 opt-in runtime mode가 검증되거나, default 미도입 결정이 source-backed report로 남는다. |

## Scope

- ScoreGate algorithm contract and pure selector implementation.
- Offline retrieval quality fixture extension and metric comparison.
- Local cross-encoder reranker score source evaluation.
- Debug/audit output contract for bucket/fusion/decision reason.
- Opt-in runtime mode decision after calibration.

## Out Of Scope

- Hosted reranker or hosted LLM API integration.
- Immediate default `rag_search` behavior change.
- Replacing Weaviate first-stage retrieval.
- Source registry registration policy changes.
- UI work.

## References

- arXiv: [ScoreGate: Adaptive Chunk Selection for Retrieval-Augmented Generation via Dual-Score Statistical Fusion](https://arxiv.org/abs/2606.14269)
- `~/Workspace/personal-assistant-wiki/inbox/2026-06-15-agent-system-paper-scrap.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/reports/2026-05-25-retrieval-quality-baseline.md`
- `docs/projects/P0002-retrieval-governance-hardening.md`
- `docs/tasks/T0017-local-reranker-evaluation.md`
- `docs/tasks/T0021-scoregate-offline-selector-experiment.md`
- `docs/tasks/T0022-scoregate-offline-evaluation-fixture.md`
- `docs/tasks/T0023-local-cross-encoder-score-source.md`
- `docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md`
- `docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md`

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| T0021 | ScoreGate offline selector experiment substrate | Done | 100% | Pure selector and focused tests are present; runtime default behavior unchanged. |
| T0022 | ScoreGate offline evaluation fixture | Done | 100% | Snapshot fixture, validator, and report are present; scores are not yet real local cross-encoder calibration evidence. |
| T0023 | Local cross-encoder score source decision | Done | 100% | Current profile has no true local cross-encoder `r_i` source; runtime ScoreGate is no-ship. |

## Future Task Candidates

- `T0024-local-cross-encoder-sidecar-proof`: Introduce a local-only cross-encoder score service or library, collect real `r_i` snapshots, and re-run ScoreGate offline evaluation before any runtime opt-in.

## Overall Progress

- 100%

## Milestones

- M1: Selector substrate completed without runtime default behavior change.
- M2: Offline snapshot fixture completed; corpus calibration remains pending local `r_i` sourcing.
- M3: Local cross-encoder score source decision completed as no-go for current profile.
- M4: Runtime no-ship decision completed.

## Exit Criteria

1. ScoreGate selector behavior is implemented, tested, and documented.
2. Local RAG evaluation evidence covers retained count, estimated context tokens, latency, hit@1, hit@5, MRR, source accuracy@1, Korean false-negative behavior, stale-source behavior, and multi-hop coverage.
3. `r_i` is sourced from a real local cross-encoder score or the project records why that is not yet viable.
4. Default runtime behavior is changed only after calibration evidence; otherwise a no-ship decision is recorded.

## Completion Evidence

- Sufficient evidence includes focused unit tests, retrieval-quality before/after metrics, cross-encoder runtime proof or no-go evidence, and docs validators.
- Insufficient evidence: paper thresholds copied into runtime, deterministic `rerankScore` reused as `r_i`, or synthetic tests without corpus evaluation.

## Outputs / Handoff

- `ScoreGateCandidateSelector` pure selector under `retrieval-service`.
- Future offline evaluation task consumes selector decisions and candidate score snapshots.
- Runtime rollout work must consume P0003 evidence and keep `rag_search` backward compatible.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Context selection must improve Local RAG without weakening local-only or source-registry truth. | No hosted API dependency; source/path governance remains separate from `r_i`. |
| SCOPE | ScoreGate must not be silently promoted to default before calibration. | Project WBS and design explicitly keep runtime default unchanged until evidence. |
| EVIDENCE | Paper results are not corpus-specific proof for this system. | Local fixture metrics and calibration report. |
| HANDOFF | Future runtime work needs a clear selector contract. | Bucket, fusion, reason, and MAX-K output fields are tested. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `ScoreGateCandidateSelector` and `ScoreGateCandidateSelectorTests`; focused Maven test passed on 2026-06-16. | Runtime default behavior is unchanged. |
| G2 | Done | `T0022` added `docs/evaluation/scoregate-offline-cases.json`, `docs/bin/validate-scoregate-offline.sh`, `ScoreGateOfflineEvaluationTests`, and `docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md`. | Snapshot fixture and evaluator exist. Corpus calibration was not performed because G3 found no true local `r_i` source in the current profile. |
| G3 | Done | `T0023` and `docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md` | Current profile has no true local cross-encoder score source. |
| G4 | Done | Runtime no-ship decision report | Default/runtime behavior remains unchanged; future runtime work requires a new sidecar/library proof. |

## Completion Guardrails

- P0003 is closed as no-ship; do not reopen runtime integration without a new score-source proof task.
- Do not substitute deterministic governance `rerankScore` for cross-encoder `r_i`.
- Do not copy paper thresholds into default runtime without Local RAG calibration.
- Do not reduce multi-hop coverage only to save tokens.

## Status

- 2026-06-16: Project issued by explicit user request as a new ScoreGate exception branch. T0021 completed the pure selector substrate and left runtime default behavior unchanged.
- 2026-06-16: T0022 completed offline snapshot fixture and validator. P0003 remains active because real local cross-encoder score sourcing, threshold calibration, and runtime rollout decision remain pending.
- 2026-06-16: T0023 found no true local cross-encoder `r_i` source in the current profile. P0003 closed as runtime no-ship while keeping selector and offline evaluator artifacts.
- 2026-06-16: Formal P0003 release version set to `1.2.0`; Maven parent/module versions, service Dockerfile jar paths, and artifact docs were advanced from `1.1.0` to `1.2.0` before tagging.
