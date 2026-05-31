---
type: task
doc_id: T0017
title: local-reranker-evaluation
status: done
owner:
created: 2026-05-31
updated: 2026-05-31
current_focus: "Closed P0002 local reranker deployment decision; separate model reranker is deferred"
completion_mode: decision-lock
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0002-retrieval-governance-hardening
related_project: docs/projects/P0002-retrieval-governance-hardening.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - docs/projects/P0002-retrieval-governance-hardening.md
  - docs/design/retrieval-quality-improvement-design.md
  - docs/evaluation/retrieval-quality-cases.yaml
  - docs/bin/validate-retrieval-quality.sh
  - docs/reports/2026-05-31-local-reranker-evaluation-decision.md
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalRanker.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
quality_axes:
  - WHOLE
  - EVIDENCE
  - SECURITY
  - CONTRACT
tags:
  - docs/task
  - local-rag-system
  - retrieval-governance
  - reranker
  - decision
---

# T0017 local-reranker-evaluation

- Type: task
- Document ID: T0017
- Status: done
- Completion Mode: decision-lock
- Owner:
- Created: 2026-05-31
- Updated: 2026-05-31
- Current Focus: Closed P0002 local reranker deployment decision; separate model reranker is deferred
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0002-retrieval-governance-hardening
- Related Project: docs/projects/P0002-retrieval-governance-hardening.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 P0002의 마지막 P2 slice로, metadata-aware chunking, governance ranking, answer context, staleness evaluation, and search audit hardening 이후 별도 local model reranker를 배포 전에 추가해야 하는지 결정한다.

## Task Placement Check

- 이 작업은 P0002의 release gate decision이며, 별도 project가 아니라 P0002 아래의 `decision-lock` task가 맞다.
- T0013-T0016 결과가 나온 뒤에만 판단할 수 있으므로 P0002 마지막 task로 둔다.
- 새 모델 도입 자체는 아직 검증된 필요가 아니므로 이 task에서 모델 runtime을 추가하지 않는다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 Local RAG가 local-only, source-registry-boundary, and Spring Boot MSA runtime을 유지하면서 배포 가능한 retrieval governance 상태인지 확인하는 것이다.

깨면 안 되는 invariant:

- private source snippet은 hosted reranker, hosted judge, external API로 보내지 않는다.
- 모델 reranker는 품질 이득과 운영 비용이 증명될 때만 별도 task로 도입한다.
- public `hybrid`, `keyword`, `vector` modes and response shape remain backward-compatible.
- P0002 closeout은 deterministic ranking, evaluation, and audit evidence로 재현 가능해야 한다.

## Completion Mode Notes

Completion mode는 `decision-lock`이다. 완료 상태는 새 모델을 도입하는 것이 아니라, P0002 배포 범위에서 local model reranker를 넣을지 말지에 대한 authoritative decision과 evidence를 남기는 것이다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- 현재 P0002 retrieval quality metrics로 local reranker 필요성을 판단한다.
- top1 miss class가 operator에게 보이도록 evaluation output을 보강한다.
- local model reranker를 P0002 배포 blocker로 둘지 여부가 문서와 project closeout에 반영된다.
- future reranker trigger가 명확하다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Current post-T0016 retrieval quality is measured. | Portable fixture reports hit/MRR/source/staleness/citation metrics and top1 miss classes. |
| G2 | Reranker value versus operational cost is decided. | The task records whether local model reranker is required for P0002 deployment, with latency and miss-class evidence. |
| G3 | Evaluation output supports future reranker decisions. | `validate-retrieval-quality.sh` exposes top1 misses, not only complete misses. |
| G4 | P0002 closeout surfaces are aligned. | Project/design/index docs reference the decision and no unresolved P0002 reranker gate remains. |

## Scope

- Re-run portable retrieval quality evaluation.
- Add top1 miss visibility to the evaluation runner.
- Inspect audit latency and deterministic weighting cost.
- Decide P0002 local reranker deployment scope.
- Update P0002, design, task, report, and index surfaces.

## Out Of Scope

- Downloading or selecting a cross-encoder model.
- Adding a sixth service or sidecar model runtime.
- Hosted reranker, hosted LLM judge, or external benchmark API.
- Expanding the fixture with machine-local/private source projects.
- UI changes.

## References

- `docs/projects/P0002-retrieval-governance-hardening.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/reports/2026-05-31-local-reranker-evaluation-decision.md`
- `docs/evaluation/retrieval-quality-cases.yaml`
- `docs/bin/validate-retrieval-quality.sh`

## Dependencies

- `T0013` metadata-aware chunking and authority metadata.
- `T0014` governance filters, stale-source demotion, historical opt-in, and answer context cues.
- `T0015` answer-quality and staleness evaluation.
- `T0016` search audit observability.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Re-run portable quality evaluation | Done | 100% | Overall hit@1 91.7%, hit@5 100%, MRR 0.958, source accuracy 100%, staleness errors 0 |
| W2 | Expose top1 miss classes | Done | 100% | Runner now prints and serializes `top1_misses` |
| W3 | Patch deterministic Korean retrieval-quality ranking gap | Done | 100% | Added Korean aliases for search/quality/evaluation/harness and a retrieval-quality path boost with focused ranker test |
| W4 | Review latency and model-reranker cost | Done | 100% | Audit shows deterministic weighting below 1 ms average in the sampled runtime window |
| W5 | Lock reranker deployment decision | Done | 100% | P0002 will not ship a separate local model reranker; deterministic governance ranking remains the release path |
| W6 | Align closeout docs | Done | 100% | P0002, design, report, task, and indexes updated |

## Overall Progress

- 100%

## Completion Criteria

1. Portable retrieval quality evaluation passes after T0016.
2. Top1 miss classes are visible in console output and JSON report.
3. Reranker decision is backed by fixture metrics and audit latency evidence.
4. No hosted reranker or hosted judge path is introduced.
5. P0002 has no unresolved reranker blocker.

## Completion Evidence

- `LOCAL_RAG_EVAL_OUTPUT=/tmp/local-rag-t0017-eval.json ./docs/bin/validate-retrieval-quality.sh` passed on 2026-05-31.
- Evaluation summary: 24 observations, overall hit@1 91.7%, hit@5 100%, MRR 0.958, source accuracy@1 100%, must-use 100%, must-not-use 100%, citation usefulness 100%, staleness errors 0.
- `hybrid` and `vector` modes reached 100% hit@1 and 100% hit@5 for the portable fixture.
- The only top1 misses were Korean natural-language `keyword` cases where the expected document was rank 2.
- A post-reindex Korean default-mode drift in `rq-ko-002` was fixed by deterministic query expansion/path weighting, then verified by `RetrievalRankerTests.koreanRetrievalQualityQueryPrefersDomainDesignOverTermGlossary`.
- `search_audit` sample showed deterministic weighting averaging below 1 ms, while embedding dominated hybrid/vector latency.
- `docs/reports/2026-05-31-local-reranker-evaluation-decision.md` records the release decision.

Evidence that is not sufficient alone:

- a single manual query that looks good,
- a model preference without local runtime proof,
- hosted reranker benchmark,
- improving `keyword` top1 while regressing default `hybrid`.

## Outputs / Handoff

- P0002 deployment proceeds without a separate local model reranker.
- Future reranker work should be re-opened only if expanded portable fixtures show repeated `hybrid` or `vector` top1/source/staleness regressions.
- `validate-retrieval-quality.sh` now gives future sessions a concise top1 drift list.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Reranker should improve the actual Local RAG runtime, not create a parallel model stack by default. | Decision keeps retrieval-service deterministic ranking as the release path. |
| EVIDENCE | Model reranker cost must be justified by measurable default-mode gaps. | Fixture metrics, top1 miss classes, and audit latency evidence. |
| SECURITY | Reranking must not leak private snippets. | No hosted reranker or hosted judge path introduced. |
| CONTRACT | Clients should not receive a different API shape for P0002. | Runner/report/doc changes are additive; Search API modes remain unchanged. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `validate-retrieval-quality.sh` passed with JSON output at `/tmp/local-rag-t0017-eval.json` | Portable fixture only; no private project ids |
| G2 | Done | Reranker decision report records "do not ship separate local model reranker in P0002" | Future trigger is expanded default-mode regression evidence |
| G3 | Done | Runner now prints and serializes `top1_misses` | Complete misses remain separately reported |
| G4 | Done | P0002 and retrieval-quality design updated for T0017 closeout | P0002 can now close after final validation |

## Completion Guardrails

- Do not treat Korean `keyword` lexical mismatch as a default `hybrid` deployment blocker.
- Do not introduce a model sidecar without a measured fixture gap and local resource budget.
- Do not remove `keyword`; it remains useful for identifiers and exact titles.
- Do not send source snippets to hosted quality services.

## Risks / Open Questions

- A future larger fixture may reveal default-mode ranking gaps not present in the portable P0002 fixture.
- Local model reranker selection remains a future task if the operator accepts additional memory and latency cost.

## Status

- 2026-05-31: task issued and completed as the P0002 local reranker deployment decision. Evaluation and audit evidence support deferring a separate local model reranker from the P0002 release path.
