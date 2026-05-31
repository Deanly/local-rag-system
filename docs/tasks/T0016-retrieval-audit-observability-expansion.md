---
type: task
doc_id: T0016
title: retrieval-audit-observability-expansion
status: done
owner:
created: 2026-05-30
updated: 2026-05-30
current_focus: "Completed search audit schema and runtime observability expansion for retrieval debugging"
completion_mode: migration
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0002-retrieval-governance-hardening
related_project: docs/projects/P0002-retrieval-governance-hardening.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - docs/projects/P0002-retrieval-governance-hardening.md
  - docs/design/retrieval-quality-improvement-design.md
  - docs/tasks/T0015-answer-quality-and-staleness-evaluation.md
  - database/postgres/ddl/001_core_schema.sql
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
  - audit
  - observability
---

# T0016 retrieval-audit-observability-expansion

- Type: task
- Document ID: T0016
- Status: done
- Completion Mode: migration
- Owner:
- Created: 2026-05-30
- Updated: 2026-05-30
- Current Focus: Completed search audit schema and runtime observability expansion for retrieval debugging
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0002-retrieval-governance-hardening
- Related Project: docs/projects/P0002-retrieval-governance-hardening.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 P0002의 P2 observability slice로, 검색 품질이 흔들렸을 때 운영자가 `search_audit`만 보고도 first-stage retrieval, deterministic ranking, source drift를 추적할 수 있게 한다.

목표는 기존 `search_audit`의 query/mode/result count/latency 기록을 candidate limit, raw/final candidate counts, phase latency, top result, source distribution, top score components까지 확장하는 것이다.

## Task Placement Check

- 이 작업은 P0002의 P2 단계이며, T0013 metadata substrate, T0014 governance ranking, T0015 evaluation checks가 모두 구현된 뒤 수행하는 audit migration이다.
- 별도 project가 아니라 P0002 아래 task가 맞는 이유는 retrieval governance 품질 디버깅을 위한 관측성 확장이기 때문이다.
- local reranker 모델 선택과 benchmark는 T0017로 분리한다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 Local RAG가 registered source boundary와 local-only model boundary를 유지하면서도 검색 품질 회귀를 운영자가 설명할 수 있게 만드는 것이다.

깨면 안 되는 invariant:

- audit persistence는 검색 실패를 만들면 안 된다.
- private source content 전체를 audit row에 저장하지 않는다.
- public Search API/MCP response shape는 backward-compatible하게 유지한다.
- schema migration은 additive `ADD COLUMN IF NOT EXISTS` 방식이어야 한다.
- hosted observability, hosted reranker, hosted judge를 도입하지 않는다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- `search_audit`가 `candidate_limit`, `raw_candidate_count`, `final_result_count`, phase latency, `source_distribution`, top result path/source, and top score JSON을 저장한다.
- retrieval-service가 existing DB에서도 audit schema를 additive하게 보강한 뒤 audit row를 쓴다.
- top result score JSON에는 `baseScore`, `sourceWeight`, `pathWeight`, `matchWeight`, `governanceWeight`, `rerankScore`, `rawCandidateCount`, `finalResultCount` 같은 debugging fields가 보존된다.
- runtime smoke가 실제 DB row에서 새 audit fields를 확인한다.
- docs/design/project/task surfaces가 T0016 완료 상태와 다음 T0017 handoff를 반영한다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Search audit schema is expanded additively. | DDL and runtime schema guard add candidate, count, latency, source distribution, top result, and top score columns. |
| G2 | Runtime writes observability fields. | Search path persists candidate limit, raw/final counts, phase latency, top source/path, source distribution, and score JSON. |
| G3 | Debug values are tested. | Focused tests cover source distribution and existing ranking score fields remain available. |
| G4 | Runtime evidence exists. | A live search writes an audit row with non-empty source distribution and top result fields. |
| G5 | Documentation and handoff are aligned. | P0002, design docs, task indexes, and T0016 agree on completed scope and T0017 next step. |

## Scope

- PostgreSQL `search_audit` additive schema migration.
- retrieval-service phase timing for embedding, Weaviate request, deterministic weighting/ranking, and total latency.
- candidate limit, raw candidate count, final result count, source distribution, top result source/path, and top score component persistence.
- focused unit tests and live DB smoke.
- docs/design/project/task closeout updates.

## Out Of Scope

- hosted telemetry or hosted observability.
- storing full retrieved source content in audit rows.
- adding a UI.
- local reranker model selection or benchmark.
- full tracing system with request propagation across all services.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Add search audit schema fields | Done | 100% | DDL adds candidate/count/latency/source/top-result/top-score fields |
| W2 | Add runtime schema guard | Done | 100% | retrieval-service runs additive audit schema guard before first audit write |
| W3 | Persist observability fields | Done | 100% | search path records phase latency, candidate counts, source distribution, top result, and score JSON |
| W4 | Add focused tests | Done | 100% | retrieval-service tests cover final-result source distribution |
| W5 | Runtime smoke and closeout | Done | 100% | live search/audit row, retrieval evaluation, Maven tests, docs validators, compose config, and diff check recorded |

## Overall Progress

- 100%

## Completion Criteria

1. `search_audit` schema is backward-compatible and additive.
2. Existing DBs are migrated automatically before audit insertion.
3. Live search audit rows include candidate limit, raw/final counts, phase latency, top result source/path, source distribution, and top score JSON.
4. Focused tests and relevant Maven test suites pass.
5. Docs validators, compose config, retrieval quality fixture, and `git diff --check` pass.

## Completion Evidence

- `database/postgres/ddl/001_core_schema.sql` adds audit fields and additive `ALTER TABLE search_audit ADD COLUMN IF NOT EXISTS ...`.
- `RetrievalService` measures embedding, Weaviate, weighting, and total latency and persists audit JSON fields with `CAST(? AS jsonb)`.
- `RetrievalService.sourceDistribution` has a focused unit test.
- `mvn -pl services/retrieval-service -am test` passed with 12 retrieval-service tests.
- Runtime retrieval-service rebuild and audit DB smoke were executed for closeout.
- `./docs/bin/validate-retrieval-quality.sh` passed against the portable default fixture.

## Outputs / Handoff

- `search_audit` is now the field-debugging surface for P0002 retrieval quality.
- T0017 can use `top_result_score`, `source_distribution`, and phase latency to decide whether a local reranker is worth the cost.
- Future UI/reporting can read the audit fields without changing Search API response shape.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Audit must describe the actual retrieval path, not a side-channel approximation. | Runtime search persists fields produced by the search/rank path. |
| EVIDENCE | Quality regressions need concrete debugging facts. | DB row shows counts, latency, top source/path, distribution, and score JSON. |
| SECURITY | Audit must not store full private snippets. | Only metadata, counts, timing, source ids, paths, and score components are stored. |
| CONTRACT | Clients should not break because audit got richer. | Search API response remains additive/backward-compatible. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `001_core_schema.sql` and retrieval-service runtime guard add all T0016 audit columns with `IF NOT EXISTS` | Existing DBs migrate on first audit write |
| G2 | Done | `RetrievalService.audit` writes candidate counts, phase latency, source distribution, top result fields, and top score JSON | `rerank_latency_ms` remains `0` until a separate model reranker exists |
| G3 | Done | `RetrievalServiceTests.sourceDistributionCountsFinalResultsBySourceId` plus existing score component tests | |
| G4 | Done | Runtime DB smoke queried the latest `search_audit` row after a live search and found populated T0016 fields | |
| G5 | Done | P0002, retrieval quality design, active docs index, and task README were updated | T0017 later completed the local reranker deployment decision |

## Completion Guardrails

- Do not store full retrieved source text in `search_audit`.
- Do not make audit schema migration destructive.
- Do not block search results if audit columns already exist or are added concurrently.
- Do not introduce hosted telemetry or hosted quality judging.
- Do not start local reranker implementation inside this task.

## Status

- 2026-05-30: task issued and completed as P0002 P2 observability slice after T0015 portable evaluation checks passed.
