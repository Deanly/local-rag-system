---
type: task
doc_id: T0021
title: scoregate-offline-selector-experiment
status: done
owner: Codex
created: 2026-06-16
updated: 2026-06-16
current_focus: Pure ScoreGate selector substrate for offline evaluation; no runtime default behavior change.
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
  - services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateCandidateSelectorTests.java
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

# T0021 scoregate-offline-selector-experiment

- Type: task
- Document ID: T0021
- Status: done
- Completion Mode: functional
- Owner: Codex
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Pure ScoreGate selector substrate for offline evaluation; no runtime default behavior change.
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: docs/projects/P0001-local-rag-system.md
- Related Project: docs/projects/P0003-scoregate-adaptive-context-selection.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`

## Purpose

이 task는 ScoreGate를 runtime default로 켜기 전에 offline experiment에서 사용할 수 있는 순수 selector substrate를 구현한다. 입력은 normalized bi-encoder similarity `s_i`, normalized local cross-encoder reranker score `r_i`, candidate metadata이며, 출력은 retained candidates, bucket, fusion score, decision reason이다.

## Task Placement Check

- 사용자가 ScoreGate 도입을 "신규 프로젝트"로 요청했기 때문에 이 task는 P0003의 첫 implementation slice다.
- 별도 project가 아니라 P0003 아래 task인 이유는 selector substrate만으로는 ScoreGate 도입 project 전체 목표인 offline calibration, local cross-encoder score source, runtime rollout 판단을 닫을 수 없기 때문이다.

## Whole-System Anchor

이 task는 Local RAG의 `rag_search` public contract와 default behavior를 보존한다. 현재 deterministic `RetrievalRanker.rerankScore`는 source/path/metadata/governance composite이므로 논문의 cross-encoder `r_i`로 재사용하지 않는다.

## Completion Mode Notes

Completion mode는 `functional`이다. 완료 상태는 실제 Java selector와 focused tests가 존재하고, 후속 offline evaluation task가 사용할 수 있는 contract가 문서화된 상태다.

## Committed Outcome

- `retrieval-service` 내부에 package-private `ScoreGateCandidateSelector`가 추가된다.
- Selector는 normalized `s_i`/`r_i`, thresholds, fusion weight, B2/B3 fusion threshold, MAX-K cap을 적용한다.
- Tests는 B1/B2/B3/B4, B3 rescue, MAX-K cap, invalid score/config guard를 검증한다.
- Runtime search path는 변경하지 않는다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | ScoreGate bucket/fusion decision을 순수 함수로 구현한다. | `ScoreGateCandidateSelector.select`가 decision list와 retained list를 반환한다. |
| G2 | B3 rescue와 MAX-K cap을 focused tests로 고정한다. | Unit tests cover bucket decisions, B3 rescue, cap ordering, and validation guards. |
| G3 | Runtime default 미도입과 score-source 경계를 문서화한다. | P0003와 retrieval-quality design이 deterministic `rerankScore`를 `r_i`로 쓰지 않는다고 명시한다. |

## Scope

- Pure selector class in `services/retrieval-service`.
- Focused unit tests.
- P0003 project/task/design/index documentation updates.

## Out Of Scope

- Local cross-encoder model selection.
- Offline corpus fixture expansion.
- Runtime `rag_search` integration.
- Threshold calibration.

## References

- arXiv: [ScoreGate: Adaptive Chunk Selection for Retrieval-Augmented Generation via Dual-Score Statistical Fusion](https://arxiv.org/abs/2606.14269)
- `~/Workspace/personal-assistant-wiki/inbox/2026-06-15-agent-system-paper-scrap.md`
- `docs/reports/2026-05-25-retrieval-quality-baseline.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/projects/P0003-scoregate-adaptive-context-selection.md`

## Dependencies

- Existing `retrieval-service` Maven module.
- No local cross-encoder runtime is required for this task because it implements the selector contract only.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Issue P0003 and T0021 docs | Done | 100% | New project and task surfaces created. |
| W2 | Implement pure selector | Done | 100% | `ScoreGateCandidateSelector` added with bucket, fusion, reason, and MAX-K logic. |
| W3 | Add focused tests | Done | 100% | Tests cover bucket thresholds, B3 rescue, cap, and validation guards. |
| W4 | Verify and close | Done | 100% | Focused Maven test passed; docs validators passed. |

## Overall Progress

- 100%

## Completion Criteria

1. Selector input and output are explicit enough for offline experiment code to consume.
2. Selector rejects non-normalized scores so current deterministic composite scores cannot silently masquerade as `r_i`.
3. Focused tests pass.
4. Documentation records that runtime default behavior is unchanged.

## Completion Evidence

- `mvn -q -pl services/retrieval-service -am -Dtest=ScoreGateCandidateSelectorTests -Dsurefire.failIfNoSpecifiedTests=false test`: passed on 2026-06-16.
- `services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java`
- `services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateCandidateSelectorTests.java`

## Outputs / Handoff

- P0003 future offline evaluation can call the selector with candidate score snapshots.
- Runtime rollout remains blocked on local cross-encoder scoring and calibration evidence.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Selector must not weaken current retrieval behavior. | No runtime integration in this task. |
| GOAL | Task goal is substrate implementation, not unproven production rollout. | P0003 keeps offline calibration and runtime rollout pending. |
| EVIDENCE | Algorithm math must be test-backed. | Focused Maven test passes. |
| CONTRACT | Future experiments need stable outputs. | Decisions include bucket, fusion score, retained flag, and reason. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `ScoreGateCandidateSelector.select` returns `Result(decisions, retained)`. | Selector is package-private and pure. |
| G2 | Done | `ScoreGateCandidateSelectorTests` covers bucket decisions, B3 rescue, MAX-K cap, and validation guards. | B3 rescue uses low similarity and high reranker score. |
| G3 | Done | P0003 and retrieval-quality design state that deterministic `rerankScore` is not cross-encoder `r_i`. | Runtime default unchanged. |

## Completion Guardrails

- Do not treat T0021 as ScoreGate production rollout.
- Do not use paper thresholds as calibrated Local RAG thresholds.
- Do not reuse deterministic governance score as cross-encoder `r_i`.

## Risks / Open Questions

- Offline evaluation still needs real `s_i`/`r_i` sourcing.
- Multi-hop coverage can regress if MAX-K cap is tuned only for token reduction.

## Status

- 2026-06-16: Task issued and completed as the first P0003 implementation slice. Selector and focused tests passed; runtime default behavior was not changed.
