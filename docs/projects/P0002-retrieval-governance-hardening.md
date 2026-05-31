---
type: project
doc_id: P0002
title: retrieval-governance-hardening
status: done
release_version: 1.1.0
project_role: exception-branch
umbrella_initiative: local-rag-system-retrieval-governance
parent_umbrella_project: P0001-local-rag-system
completion_mode: functional
owner:
created: 2026-05-29
updated: 2026-05-31
current_focus: "Retrieval governance hardening complete and deployable without a separate local model reranker"
related_control_plane: docs/design/control-plane.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - docs/projects/P0001-local-rag-system.md
  - docs/tasks/T0011-retrieval-quality-hardening.md
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - docs/reports/2026-05-31-local-reranker-evaluation-decision.md
  - docs/design/retrieval-quality-improvement-design.md
  - source:conversation/2026-05-29-hybrid-rag-llm-wiki-governance-review
quality_axes:
  - WHOLE
  - SCOPE
  - HANDOFF
  - EVIDENCE
  - SECURITY
  - CONTRACT
tags:
  - docs/project
  - local-rag-system
  - retrieval-governance
  - llm-wiki
---

# P0002 retrieval-governance-hardening

- Type: project
- Document ID: P0002
- Status: done
- Release Version: 1.1.0
- Project Role: exception-branch
- Umbrella Initiative: local-rag-system-retrieval-governance
- Parent Umbrella Project: P0001-local-rag-system
- Completion Mode: functional
- Owner:
- Created: 2026-05-29
- Updated: 2026-05-31
- Current Focus: Retrieval governance hardening complete and deployable without a separate local model reranker
- Related Control Plane: docs/design/control-plane.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 project는 P0001의 local-only RAG functional baseline 이후, 문서가 많아져도 Local RAG가 최신성, 권위, 폐기 여부, 중복, citation 품질을 안정적으로 다루도록 검색 거버넌스를 고도화한다.

핵심 방향은 단순히 더 많은 문서를 indexing하는 것이 아니라, Markdown/LLM Wiki 문서의 frontmatter와 heading 구조를 chunk metadata로 보존하고, 검색 필터와 rerank, answer context packing, 평가 지표까지 같은 권위 모델을 사용하게 만드는 것이다.

## Umbrella Lineage

- 이 문서는 `P0001 local-rag-system`의 기능 baseline 완료 이후 발급된 exception branch project다.
- parent umbrella project는 `P0001-local-rag-system`이다.
- P0001은 local-only RAG runtime, indexing, hybrid search, Codex bridge의 functional baseline을 닫는다.
- P0002는 그 baseline 위에서 LLM Wiki/Governed Hybrid RAG 품질을 별도 delivery boundary로 소유한다.

## Project Issuance Check

- 사용자가 2026-05-29에 RAG/LLM Wiki 검색 거버넌스 개선사항을 우선순위별로 준비하라고 요청했고, 새 project `P0002` 발급을 명시적으로 제안했다.
- 이 작업은 단일 quick fix가 아니라 chunk schema, reindex, search filter, answer prompt, evaluation, audit migration을 포함하는 다단계 품질 hardening이다.
- 기존 P0001 안의 task로 계속 누적하면 functional baseline closeout과 후속 governed retrieval 품질 목표가 섞인다.
- human 입장에서는 P0001을 v1 functional baseline으로 읽고, P0002를 v1.x retrieval governance improvement track으로 읽는 편이 더 명확하다.

## Whole-System Anchor

이 project가 보존해야 하는 전체 목표는 private source content를 local-only boundary 안에 둔 채, 등록된 source truth를 더 정확한 우선순위와 근거로 Codex/CLI/future UI에 제공하는 것이다.

깨면 안 되는 invariant:

- registered source root 밖의 파일은 indexing/search 대상이 아니다.
- repo `docs/`는 project current truth이고 compiled wiki는 support context다.
- deprecated, superseded, draft, raw memo는 canonical/current 문서와 같은 권위로 취급하지 않는다.
- private source snippet은 hosted API 또는 hosted reranker로 전송하지 않는다.
- ranking, filtering, answer context 변경은 versioned evaluation fixture로 회귀 측정한다.
- public search modes는 `hybrid`, `keyword`, `vector`를 유지한다.
- Codex/MCP/REST response는 citation, source metadata, score/debug information을 backward-compatible하게 유지한다.

## Completion Mode Notes

Completion mode는 `functional`이다. 이 project가 닫히려면 문서 권위 metadata가 실제 indexing, retrieval, answer, evaluation surface에서 동작하고, 운영자가 P0001 대비 검색 거버넌스 개선을 재현 가능한 증거로 확인할 수 있어야 한다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- Markdown frontmatter/title/status/authority/updated/supersession metadata가 chunk metadata로 indexing된다.
- heading hierarchy, stable heading id, chunk overlap, table/code block 보존 기준이 구현되거나 명확히 검증된다.
- Search API filters가 `ssotRole`, `sourceType`, `status`, `authority`, `sensitivity`, document type 같은 retrieval metadata에 실제로 적용된다.
- deprecated/superseded document는 기본 검색에서 강하게 제외 또는 demote되고, historical query에서만 의도적으로 사용된다.
- Answer context packer가 source title, status, authority, updated, supersededBy 같은 priority cues를 LLM에게 제공한다.
- Evaluation fixture가 hit/MRR뿐 아니라 staleness error, must/must-not-use source, citation usefulness, answer faithfulness를 점검한다.
- Search audit가 first-stage candidate, rerank, source distribution, top result drift를 field debugging 가능한 수준으로 기록한다.
- 모든 변경은 local-only model/runtime boundary를 유지하고 docs validators, service tests, compose config, retrieval evaluation으로 검증된다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Chunking and document authority metadata를 retrieval substrate에 반영한다. | frontmatter/title/status/authority/updated/supersession/heading metadata가 index schema와 search result에 나타나고 reindex/evaluation evidence가 있다. |
| G2 | Search filtering and ranking이 문서 권위, 최신성, 폐기 상태를 실제로 사용한다. | deprecated/superseded/raw support 문서가 current/canonical 문서를 밀어내지 않는 regression cases가 통과한다. |
| G3 | Answer generation context가 source priority와 citation cues를 포함한다. | `/api/answer`가 context source metadata를 포함하고 stale/conflicting sources를 우선순위에 맞게 다루는 smoke/evaluation이 있다. |
| G4 | Retrieval evaluation을 answer-quality and staleness governance까지 확장한다. | fixture와 runner가 hit/MRR 외 staleness error, mustContain/mustNotUse, citation usefulness 또는 faithfulness checks를 산출한다. |
| G5 | Audit and observability가 retrieval 품질 디버깅을 지원한다. | search audit 또는 generated report가 candidate limit, raw/final counts, phase latency, top source/path, source distribution을 제공한다. |
| G6 | P0001 functional baseline과 public contract를 보존한다. | existing docs validators, compose config, service tests, retrieval quality baseline checks가 no-regression으로 통과한다. |

## Scope

- Markdown AST/frontmatter-aware chunking
- document authority/status/supersession metadata schema
- Weaviate/PostgreSQL schema migration and reindex plan
- metadata filters and stale/deprecated default policy
- answer context packing and source priority prompt rules
- retrieval evaluation fixture and runner expansion
- search audit schema/report expansion
- local-only deterministic or local-model reranker evaluation after metadata improvements plateau

## Out Of Scope

- hosted reranker or hosted LLM judge
- multi-user SaaS permissions
- replacing Weaviate as the first-stage retrieval engine
- broad attachment parsing beyond Markdown/plain text
- changing Codex tool names or public search mode names
- indexing unregistered folders
- making P0001 functional baseline closeout depend on P0002 completion

## References

- `docs/projects/P0001-local-rag-system.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/tasks/T0011-retrieval-quality-hardening.md`
- `docs/reports/2026-05-25-retrieval-quality-baseline.md`
- `services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java`
- `services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalRanker.java`
- `database/weaviate/local-rag-chunk.schema.json`
- `database/postgres/ddl/001_core_schema.sql`

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| T0013 | Retrieval chunking and document authority hardening | Done | 100% | P0 complete: frontmatter/title/status/authority metadata, schema, reindex, and search result exposure |
| T0014 | Search filter and answer context governance | Done | 100% | P1 complete: metadata filters, stale-source demotion, historical opt-in, answer context source priority cues |
| T0015 | Answer quality and staleness evaluation | Done | 100% | P1 complete: portable fixture cleanup, must-use/must-not-use checks, citation usefulness, staleness errors, optional unknown-project skips, Korean task-id suffix regression |
| T0016 | Retrieval audit observability expansion | Done | 100% | P2 complete: search audit candidate counts, phase latency, source distribution, top result, and score JSON |
| T0017 | Local reranker evaluation | Done | 100% | P2 decision complete: default hybrid/vector quality is sufficient for deployment; separate local model reranker deferred |

## Planned Task Candidates

- _none_

## Execution Plan

Work must proceed in dependency order. Do not start model reranker work before metadata, filtering, answer context, and evaluation gaps are closed.

| Priority | Task | Purpose | Exit Gate |
| --- | --- | --- | --- |
| P0 | `T0013-retrieval-chunking-and-document-authority-hardening` | Build the metadata substrate: frontmatter-aware chunking, document authority fields, schema/reindex path, and search result exposure. | New metadata is present in indexed chunks, search results expose it backward-compatibly, reindex is documented, and T0011 retrieval cases show no unacceptable regression. |
| P1 | `T0014-search-filter-and-answer-context-governance` | Make retrieval and answer generation use document status, authority, freshness, supersession, and source role. | Search filters are enforced, deprecated/superseded docs are excluded or demoted by default, historical queries can opt in, and `/api/answer` receives source priority cues. |
| P1 | `T0015-answer-quality-and-staleness-evaluation` | Extend quality checks beyond document discovery. | Evaluation reports staleness error, must-use/must-not-use source checks, citation usefulness, and answer faithfulness or an explicitly bounded local-only substitute. |
| P2 | `T0016-retrieval-audit-observability-expansion` | Make field debugging possible when ranking or answer quality regresses. | Search audit or generated reports include candidate limits, raw/final counts, phase latency, top source/path, source distribution, and rerank score components. |
| P2 | `T0017-local-reranker-evaluation` | Decide whether a local reranker is worth the operational cost. | Done: current default `hybrid`/`vector` fixture quality does not justify shipping a separate local model reranker in P0002. |

## Release Gates

- `P0 gate`: metadata schema and reindex path are stable enough that later filters and answer context do not need to guess from path-only heuristics.
- `P1 gate`: Codex-facing answers prefer canonical/current evidence and avoid stale source contamination in repeatable evaluation cases.
- `P2 gate`: operators can explain and debug retrieval drift without reproducing the whole request manually.
- `Project closeout gate`: all issued tasks are done, no hosted model path is introduced, and the T0011 baseline plus P0002 staleness cases pass.

## Overall Progress

- 100%

## Milestones

- M1: Document metadata schema and reindex path locked.
- M2: Chunk metadata and authority policy implemented.
- M3: Search filters and answer context use the authority policy.
- M4: Evaluation suite catches stale-source and answer-grounding regressions.
- M5: Audit/reporting supports retrieval debugging.

## Exit Criteria

1. All issued P0002 task docs are `done`, or residual work is explicitly split into follow-up tasks without shrinking this project purpose.
2. `docs/design/retrieval-quality-improvement-design.md`, P0002, and completed task docs agree on the active authority/status metadata contract.
3. Reindex/migration evidence proves the new metadata is present in the retrieval index.
4. Evaluation proves no regression against the T0011 baseline and adds stale/deprecated misuse checks.
5. Answer generation uses source priority metadata and cites evidence-bearing results.
6. Local-only security boundary is verified for every rerank/evaluation/answer path.
7. Required docs validators, service tests, compose config, and retrieval quality checks pass or any skipped runtime checks are explicitly explained.

## Completion Evidence

Required evidence:

- schema migration or schema compatibility note,
- indexer/chunker tests for frontmatter, heading hierarchy, overlap, code/table preservation where implemented,
- reindex command and result summary,
- search API filter/ranking tests,
- answer context smoke with visible source priority cues,
- retrieval evaluation before/after metrics including staleness cases,
- audit/report output sample,
- local reranker deployment decision report,
- docs validators, compose config, and relevant Maven tests.

Evidence that is not sufficient alone:

- a single manual query that looks better,
- metadata fields documented but not present in indexed chunks,
- filtering fields accepted in request DTO but not enforced in Weaviate/PostgreSQL query,
- answer prompt wording without source priority metadata in the supplied context,
- hosted API benchmark.

## Outputs / Handoff

- P0002 owns the post-P0001 retrieval governance delivery boundary.
- The completed tasks preserve `docs/evaluation/retrieval-quality-cases.yaml` and extend it rather than replacing the T0011 baseline.
- P0002 ships with deterministic governance ranking; future model reranker work needs a new task and expanded default-mode regression evidence.
- P0002 is the official `1.1.0` release line. It is backward-compatible with the P0001 `1.0.0` functional baseline and advances runtime artifact versions to `1.1.0`.
- P0001 remains the official `1.0.0` functional runtime baseline.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Retrieval governance must improve the existing Local RAG system, not fork a parallel search product. | Same Spring Boot MSA, Weaviate, Ollama, registry, and public API boundaries remain in use. |
| SCOPE | P0002 should own metadata/ranking/answer/evaluation quality, not attachment parsing or cloud integrations. | Scope/out-of-scope plus task WBS separation. |
| HANDOFF | Future sessions need a clear priority order and schema/reindex path. | P0002 WBS, T0013 task, design references, migration/reindex notes. |
| EVIDENCE | Search quality changes must be measured and reproducible. | Versioned evaluation fixture, before/after metrics, stale-source regression cases. |
| SECURITY | Source snippets and private docs must remain local-only. | No hosted reranker/LLM path, local endpoint checks, compose/runtime evidence. |
| CONTRACT | Existing clients should not break while internals improve. | Backward-compatible DTO/API/MCP behavior and smoke tests. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `T0013` completed metadata-aware chunking, document authority indexing, additive schema migration, force-scan reindex, and search result metadata exposure | Full stale/default filter policy was completed in `T0014`. |
| G2 | Done | `T0014` implemented metadata filters, governance rerank weight, default stale-source demotion, and historical opt-in | `T0015` added deterministic stale-source misuse checks. |
| G3 | Done | `T0014` added source priority cues to answer context and verified `/api/answer` runtime smoke | `T0015` added bounded local-only source-use/citation usefulness checks. |
| G4 | Done | `T0015` extended the evaluation runner and fixture with portable local-rag-system default cases, must-use, must-not-use, citation usefulness, staleness error metrics, optional unknown-project skip reporting, and Korean task-id suffix coverage | `local-rag-governance` evaluation cases reported 100% source-use/citation checks and 0 staleness errors. |
| G5 | Done | `T0016` expanded `search_audit` and retrieval-service audit writes with candidate limit, raw/final counts, phase latency, top result source/path, source distribution, and top score JSON | Runtime DB smoke verified populated audit fields after live search. |
| G6 | Done | `T0017` reranker decision, retrieval evaluation, Maven tests, docs validators, compose config, runtime health/search/index smoke, and `git diff --check` passed during project closeout | No hosted reranker/LLM judge path introduced; public modes remain `hybrid`, `keyword`, and `vector`. |

## Completion Guardrails

- Do not close P0002 by documenting recommendations only; the metadata and retrieval behavior must be implemented and measured.
- Do not send private source content to hosted rerankers, hosted LLM judges, or external evaluation APIs.
- Do not remove support context merely to make metrics look better; weight and filter it deliberately.
- Do not make deprecated/superseded documents disappear entirely from historical queries; default behavior should exclude or demote them unless explicitly requested.
- Do not break public `hybrid`, `keyword`, and `vector` search modes.
- Do not rewrite P0001's functional baseline goals while moving quality hardening into P0002.

## Risks / Open Questions

- The formal P0002 release should be marked by the `v1.1.0` Git tag.
- Evaluation fixtures can become stale as docs change; maintenance rules need to be part of the evaluation task.
- A future larger fixture may justify local model reranker work, but it is not a P0002 deployment blocker.

## Status

- 2026-05-29: Project issued by explicit user request as the follow-on retrieval governance hardening track after P0001 functional baseline. First critical-path task is `T0013`.
- 2026-05-30: Existing plan reviewed and supplemented with explicit priority order, release gates, and dependency gates. `T0013` remains the first implementation task.
- 2026-05-30: P0 `T0013` completed. The retrieval substrate now carries document title/type/status/authority/updated/supersession and full heading metadata through chunking, indexing, reindex, and search results. Next project slice is P1 `T0014` search filter and answer context governance.
- 2026-05-30: `T0014` issued to make the T0013 metadata contract affect search filters, stale-source ranking, historical opt-in, and answer context cues.
- 2026-05-30: `T0014` completed metadata filter enforcement, governance rerank weights, historical opt-in, and answer context priority cues.
- 2026-05-30: `T0015` completed deterministic answer-quality/staleness evaluation expansion and made the default retrieval fixture portable by removing machine-local project dependencies. P0002 now moves to `T0016` audit and observability expansion.
- 2026-05-30: `T0016` completed search audit and runtime observability expansion. P0002 now moves to `T0017` local reranker evaluation decision.
- 2026-05-31: `T0017` completed the local reranker deployment decision. P0002 is closed as deployable with deterministic governance ranking, metadata-aware retrieval, source-priority answer context, staleness/citation evaluation, audit observability, and no separate local model reranker in the release path.
- 2026-05-31: Test deployment verification completed for `local-rag-system`. The operation registry now excludes `docs/_templates/**` from the project docs source; a forced project scan detected 55 active documents and removed 6 template documents. The deployed retrieval fixture passed with overall hit@1 95.8%, hit@5 100.0%, MRR 0.979, must-use 100.0%, must-not-use 100.0%, citation usefulness 100.0%, and staleness errors 0.
- 2026-05-31: P0002 formal release version set to `1.1.0`; Maven parent/module versions, service Dockerfile jar paths, and runtime artifact docs were advanced from `1.0.0` to `1.1.0` before tagging.
