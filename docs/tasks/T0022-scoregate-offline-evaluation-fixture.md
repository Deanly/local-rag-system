---
type: task
doc_id: T0022
title: scoregate-offline-evaluation-fixture
status: done
owner: Codex
created: 2026-06-16
updated: 2026-06-16
current_focus: Offline ScoreGate snapshot fixture and validator for selector-level evaluation metrics.
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: docs/projects/P0001-local-rag-system.md
related_project: docs/projects/P0003-scoregate-adaptive-context-selection.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - https://arxiv.org/abs/2606.14269
  - ~/Workspace/personal-assistant-wiki/inbox/2026-06-15-agent-system-paper-scrap.md
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - docs/design/retrieval-quality-improvement-design.md
  - docs/projects/P0003-scoregate-adaptive-context-selection.md
  - docs/tasks/T0021-scoregate-offline-selector-experiment.md
  - docs/evaluation/scoregate-offline-cases.json
  - docs/bin/validate-scoregate-offline.sh
  - services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateOfflineEvaluationTests.java
  - docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md
quality_axes:
  - WHOLE
  - GOAL
  - EVIDENCE
  - CONTRACT
tags:
  - docs/task
  - local-rag-system
  - retrieval-quality
  - scoregate
---

# T0022 scoregate-offline-evaluation-fixture

- Type: task
- Document ID: T0022
- Status: done
- Completion Mode: functional
- Owner: Codex
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Offline ScoreGate snapshot fixture and validator for selector-level evaluation metrics.
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: docs/projects/P0001-local-rag-system.md
- Related Project: docs/projects/P0003-scoregate-adaptive-context-selection.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`

## Purpose

이 task는 T0021의 순수 `ScoreGateCandidateSelector`를 실제 offline evaluation input 형식과 연결한다. Runtime `rag_search` path를 바꾸지 않고, normalized `s_i`/`r_i` score snapshot을 넣으면 retained candidates, B3 rescue, fixed top-K miss rescue, retained token estimate, multi-hop coverage를 검증할 수 있는 fixture와 validator를 만든다.

## Task Placement Check

- 사용자가 ScoreGate 도입을 신규 P0003 project로 요청했으므로 이 task는 P0003의 두 번째 implementation slice다.
- 별도 project가 아닌 이유는 local cross-encoder score source, threshold calibration, runtime opt-in 판단이 여전히 P0003 안에 남아 있기 때문이다.

## Whole-System Anchor

이 task는 Local RAG의 local-only invariant와 stable public `rag_search` contract를 보존한다.

깨면 안 되는 경계:

- private source content를 hosted reranker나 hosted LLM으로 보내지 않는다.
- deterministic governance `rerankScore`를 cross-encoder `r_i`로 취급하지 않는다.
- snapshot fixture 통과를 production calibration으로 과장하지 않는다.
- multi-hop coverage를 token reduction보다 우선 검증한다.

## Completion Mode Notes

Completion mode는 `functional`이다. 완료 상태는 offline ScoreGate fixture와 validator가 존재하고, focused Maven test가 selector contract를 통해 fixture metrics를 검증하는 상태다.

## Committed Outcome

- `docs/evaluation/scoregate-offline-cases.json`이 한국어 lexical mismatch, 영어 identifier, primary-source preference, stale demotion, broad/multi-hop coverage case를 가진다.
- `docs/bin/validate-scoregate-offline.sh`가 fixture를 검증하는 focused Maven test를 실행한다.
- `ScoreGateOfflineEvaluationTests`가 actual `ScoreGateCandidateSelector`를 사용해 retained candidates, B3 rescue, fixed top-K miss rescue, retained token estimate, source accuracy, and multi-hop coverage를 산출한다.
- Runtime search path는 변경하지 않는다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Offline ScoreGate snapshot fixture를 만든다. | `docs/evaluation/scoregate-offline-cases.json`이 candidateWindowSize 40 계열 snapshot과 required case classes를 담는다. |
| G2 | Selector contract 기반 validator를 만든다. | `./docs/bin/validate-scoregate-offline.sh`가 actual Java selector를 사용해 fixture를 검증한다. |
| G3 | Snapshot evaluation과 production calibration 경계를 문서화한다. | T0022와 P0003가 local cross-encoder `r_i` sourcing/calibration은 아직 pending이라고 남긴다. |

## Scope

- Offline snapshot fixture.
- Focused Java test that consumes `ScoreGateCandidateSelector`.
- Shell validator.
- Snapshot evaluation report.
- Project/design/index documentation updates.

## Out Of Scope

- Local cross-encoder model selection or sidecar.
- Runtime `rag_search` opt-in mode.
- Production threshold calibration.
- Hosted reranker integration.

## References

- arXiv: [ScoreGate: Adaptive Chunk Selection for Retrieval-Augmented Generation via Dual-Score Statistical Fusion](https://arxiv.org/abs/2606.14269)
- `~/Workspace/personal-assistant-wiki/inbox/2026-06-15-agent-system-paper-scrap.md`
- `docs/reports/2026-05-25-retrieval-quality-baseline.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/projects/P0003-scoregate-adaptive-context-selection.md`
- `docs/tasks/T0021-scoregate-offline-selector-experiment.md`

## Dependencies

- T0021 `ScoreGateCandidateSelector`.
- Existing retrieval-service Maven module and Java 17 test runtime.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Define offline fixture schema | Done | 100% | JSON fixture includes normalized scores, candidate rank, source/path, estimated tokens, and case expectations. |
| W2 | Implement validator | Done | 100% | `ScoreGateOfflineEvaluationTests` calls the actual selector and writes optional JSON output. |
| W3 | Add shell entrypoint | Done | 100% | `docs/bin/validate-scoregate-offline.sh` runs the focused Maven test. |
| W4 | Verify and close | Done | 100% | Focused tests and ScoreGate offline validator passed. |

## Overall Progress

- 100%

## Completion Criteria

1. The fixture covers Korean lexical mismatch, English identifier lookup, primary-source preference, stale demotion, and broad/multi-hop coverage.
2. The validator uses the actual Java selector, not a duplicate implementation.
3. Output metrics include hit@1, hit@5, MRR, source accuracy@1, fixed top-K miss rescue, retained token estimate, and B3 rescue count.
4. Documentation states that snapshot validation is not production calibration.

## Completion Evidence

- `./docs/bin/validate-scoregate-offline.sh`: passed on 2026-06-16.
- `mvn -q -pl services/retrieval-service -am -Dtest=ScoreGateCandidateSelectorTests,ScoreGateOfflineEvaluationTests -Dsurefire.failIfNoSpecifiedTests=false test`: passed on 2026-06-16.
- `LOCAL_RAG_SCOREGATE_OUTPUT=/tmp/local-rag-scoregate-offline-report.json ./docs/bin/validate-scoregate-offline.sh`: passed and generated snapshot metrics on 2026-06-16.
- `docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md`

## Outputs / Handoff

- T0023 can use the fixture schema to replace manual normalized `r_i` snapshots with a real local cross-encoder score source.
- T0024 can consume the report fields for debug/audit output design after calibration exists.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | ScoreGate work must preserve Local RAG source truth and local-only constraints. | No runtime integration or hosted call is introduced. |
| GOAL | The task advances P0003 without pretending calibration is complete. | T0023 later closes runtime rollout as no-ship because real `r_i` is unavailable. |
| EVIDENCE | Future rollout needs repeatable metrics, not anecdotal examples. | Validator output and report record fixture metrics. |
| CONTRACT | Future score sources need a stable fixture/report shape. | JSON fixture and focused test define the contract. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `docs/evaluation/scoregate-offline-cases.json` | Candidate windows are marked as 40 and cover the required case classes. |
| G2 | Done | `docs/bin/validate-scoregate-offline.sh`; `ScoreGateOfflineEvaluationTests` | Test calls `ScoreGateCandidateSelector` directly. |
| G3 | Done | This task, P0003, and report scope statements | Runtime calibration remains blocked on local `r_i` sourcing. |

## Completion Guardrails

- Do not treat snapshot score validation as production calibration.
- Do not close P0003 while local cross-encoder score source and runtime rollout decision remain pending.
- Do not replace the Java selector with a second script-only implementation.

## Risks / Open Questions

- Real local cross-encoder scores may shift bucket distribution and thresholds.
- Current fixture is intentionally small and contract-focused; it does not prove domain-level quality.

## Status

- 2026-06-16: Task issued and completed. Offline fixture, validator, and snapshot report are present; runtime default behavior remains unchanged.
