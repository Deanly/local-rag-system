---
type: task
doc_id: T0015
title: answer-quality-and-staleness-evaluation
status: done
owner:
created: 2026-05-30
updated: 2026-05-30
current_focus: "Completed deterministic staleness, must-use, must-not-use, citation usefulness, and Korean task-id regression checks"
completion_mode: implementation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0002-retrieval-governance-hardening
related_project: docs/projects/P0002-retrieval-governance-hardening.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - docs/projects/P0002-retrieval-governance-hardening.md
  - docs/tasks/T0014-search-filter-and-answer-context-governance.md
  - docs/evaluation/retrieval-quality-cases.yaml
  - docs/bin/validate-retrieval-quality.sh
quality_axes:
  - WHOLE
  - EVIDENCE
  - SECURITY
  - CONTRACT
tags:
  - docs/task
  - local-rag-system
  - retrieval-governance
  - evaluation
  - staleness
---

# T0015 answer-quality-and-staleness-evaluation

- Type: task
- Document ID: T0015
- Status: done
- Completion Mode: implementation
- Owner:
- Created: 2026-05-30
- Updated: 2026-05-30
- Current Focus: Completed deterministic staleness, must-use, must-not-use, citation usefulness, and Korean task-id regression checks
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0002-retrieval-governance-hardening
- Related Project: docs/projects/P0002-retrieval-governance-hardening.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`

## Purpose

이 task는 `T0011`의 hit/MRR 중심 retrieval evaluation을 `T0013`/`T0014`의 governed retrieval metadata에 맞게 확장한다.

목표는 단순히 정답 문서가 top K에 들어왔는지뿐 아니라, 반드시 사용해야 하는 문서가 포함됐는지, 쓰면 안 되는 stale/draft/deprecated 문서가 섞였는지, citation이 실제 문서 경로를 가리키는지 확인하는 로컬 deterministic 평가를 추가하는 것이다.

## Task Placement Check

- 이 작업은 P0002의 P1 평가 slice이며, T0013 metadata와 T0014 filter/ranking/answer context behavior를 회귀 검증한다.
- 별도 project가 아니라 P0002 아래 task가 맞는 이유는 evaluation fixture가 P0002 검색 거버넌스 품질 gate를 소유하기 때문이다.
- PostgreSQL audit migration and local reranker benchmarking은 후속 P2 task로 분리한다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 검색 거버넌스 변경을 감으로 닫지 않고, local-only deterministic evaluation으로 반복 확인할 수 있게 만드는 것이다.

깨면 안 되는 invariant:

- evaluation은 registered project/source scope를 우회하지 않는다.
- 현재 장비에 등록되지 않은 fixture project는 skip으로 기록하고 전체 실행을 중단하지 않는다.
- private source content는 hosted judge나 hosted evaluation API로 전송하지 않는다.
- T0011 hit/MRR baseline은 보존하고, P0002 staleness/source-use checks를 additive하게 확장한다.
- citation checks는 실제 retrieval result path/citation metadata를 사용한다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- retrieval quality runner가 hit/MRR 외 must-use, must-not-use, citation usefulness, and staleness error metrics를 산출한다.
- unknown project fixture rows가 per-mode skip으로 보고된다.
- Local RAG governance fixture가 T0014 source use, stale template avoidance, and citation usefulness를 검증한다.
- `T0014의` 같은 Korean task-id suffix query가 first-stage retrieval에서 T0014를 찾도록 regression coverage가 있다.
- closeout evidence가 Maven tests, runtime smoke, retrieval evaluation, docs validators, compose config, and diff check를 포함한다.

## Scope

- `docs/bin/validate-retrieval-quality.sh`에 must-use, must-not-use, staleness error, citation usefulness checks 추가.
- optional fixture에서 현재 registry에 없는 project는 unknown-project로 skip하여 실행 가능한 subset을 평가한다.
- `docs/evaluation/retrieval-quality-cases.yaml`을 portable default로 정리하고 Local RAG governance cases 추가.
- Korean task-id suffix query normalization regression case 추가.
- Hosted LLM judge 없이 local-only deterministic substitute로 answer/source faithfulness의 하한선을 검증한다.

## Out Of Scope

- Hosted judge, hosted reranker, or hosted evaluation API.
- Full natural-language answer grading.
- PostgreSQL audit expansion.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Evaluation runner reports staleness and source-use checks. | Output includes must-use, must-not-use, citation usefulness, and staleness error metrics. |
| G2 | Fixture can run on partial local registries. | Unknown project fixture rows are skipped and reported instead of aborting the run. |
| G3 | Governance cases cover P0002 behavior. | Fixture includes local-rag governance cases for T0014 and stale template avoidance. |
| G4 | Korean task-id suffix regression is covered. | Fixture catches `T0014의` style query behavior. |
| G5 | Evidence is recorded. | Evaluation output, Maven tests, docs validators, compose config, and runtime smoke pass or caveats are explained. |

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Extend evaluation runner | Done | 100% | Added must-use, must-not-use, citation usefulness, staleness error metrics, and optional unknown-project skip reporting |
| W2 | Extend governance fixture | Done | 100% | Replaced machine-local fixture dependencies with portable local-rag-system cases and added governance cases for T0014 source use, stale template avoidance, and citation usefulness |
| W3 | Fix task-id Korean suffix retrieval regression | Done | 100% | Normalized `T0014의` style query before first-stage retrieval and added a focused unit test |
| W4 | Run evaluation and tests | Done | 100% | Maven tests, retrieval quality run, runtime rebuild, health, force scan, search smoke, and answer smoke passed |
| W5 | Verify and close | Done | 100% | Closeout docs updated; full validator ladder executed in task closeout |

## Completion Criteria

1. Evaluation runner prints and reports must-use, must-not-use, citation usefulness, and staleness error metrics.
2. Unknown project cases are skipped and listed rather than failing the entire evaluation.
3. Local RAG governance cases pass for `hybrid`/`keyword` where configured.
4. Korean task-id suffix query retrieves `T0014` instead of generic templates.
5. Required validators and relevant service tests pass.

## Completion Evidence

- `mvn -pl services/retrieval-service -am test` passed with 11 retrieval-service tests, including Korean task-id suffix normalization.
- `mvn -pl services/indexer-service -am test` passed with 12 indexer-service tests, preserving T0013 chunking behavior.
- `docker compose --env-file .env.example up -d --build retrieval-service` rebuilt the runtime service and `/api/health` returned UP for gateway, registry, indexer, retrieval, and MCP bridge.
- `POST /api/index/force?projectId=local-rag-system` scanned the registered project source with 58 documents detected and no errors.
- Runtime `/api/search` for `T0014의 목적은 무엇인가?` returned `tasks/T0014-search-filter-and-answer-context-governance.md` as the top result.
- Runtime `/api/answer` for the same query returned T0014 citations and result metadata.
- `./docs/bin/validate-retrieval-quality.sh` completed against the portable default fixture with no machine-local project dependency: `cases=24`, `overall hit@1=91.7%`, `hit@5=100.0%`, `source_accuracy@1=100.0%`, `must_use=100.0%`, `must_not_use=100.0%`, `citation_usefulness=100.0%`, and `staleness_errors=0`.

## Outputs / Handoff

- Evaluation runner and fixture become the P0002 P1 regression gate.
- Staleness/source-use/citation metrics feed `T0016-retrieval-audit-observability-expansion`.
- Optional machine-local fixtures should live outside the committed default fixture or be generated locally.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Evaluation must measure the actual Local RAG runtime, not a parallel harness. | Runner calls the gateway/MCP search path and uses registered project ids. |
| EVIDENCE | P0002 governance should close with repeatable checks. | Source-use, citation, staleness, and task-id regression cases are versioned. |
| SECURITY | Answer quality checks must remain local-only. | No hosted LLM judge or hosted reranker is introduced. |
| CONTRACT | Existing T0011 hit/MRR output should remain readable. | New metrics are additive columns and JSON fields. |
| SCOPE | Audit/reranker work should stay separate. | Handoff keeps P2 observability and local reranker evaluation out of T0015. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `./docs/bin/validate-retrieval-quality.sh` now reports `must_use`, `must_not_use`, `citation_usefulness`, and `staleness_errors` | Portable default run reported 100% for all deterministic source-use/citation checks and 0 staleness errors |
| G2 | Done | Runner lists registered projects before search and skips optional fixture rows for projects absent from the active registry | The committed default fixture now avoids private/machine-local project ids, so ordinary runs should not depend on this skip path |
| G3 | Done | `local-rag-governance/hybrid` and `local-rag-governance/keyword` both reported 100% hit/source/source-use/citation checks and 0 staleness errors | Fixture covers T0014 and stale template avoidance |
| G4 | Done | `RetrievalServiceTests.retrievalQuerySeparatesTaskIdFromKoreanSuffix` plus runtime `/api/search` smoke for `T0014의 목적은 무엇인가?` | Top result was `tasks/T0014-search-filter-and-answer-context-governance.md` |
| G5 | Done | Runtime rebuild, health, force scan, evaluation run, Maven tests, docs validators, compose config, and `git diff --check` were executed for closeout | Portable fixture cleanup removed machine-local project dependencies from the default evaluation set |

## Completion Guardrails

- Do not hide retrieval misses; report them separately from deterministic governance failures.
- Do not make unknown fixture projects look successful; skip and report them explicitly.
- Do not introduce hosted answer judging for private local sources.
- Do not replace the T0011 fixture; extend it additively.
- Do not treat citation presence alone as full natural-language faithfulness.

## Status

- 2026-05-30: task issued after `T0014` completed search filter and answer context governance.
- 2026-05-30: implementation completed. Evaluation runner now reports source-use, citation, and staleness metrics; unregistered fixture projects are skipped; Local RAG governance cases pass; and Korean task-id suffix retrieval is covered by test and runtime smoke.
- 2026-05-31: test deployment regression found `_templates/task.md` appearing in the T0014 governance result set even though the expected source remained rank 1. The portable registry examples and operation registry now exclude `**/_templates/**`; after forced re-scan, template documents were removed and the deployed fixture returned must-not-use 100.0% with staleness errors 0.
