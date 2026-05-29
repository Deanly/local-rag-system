---
type: project
doc_id: P0002
title: retrieval-governance-hardening
status: active
project_role: exception-branch
umbrella_initiative: local-rag-system-retrieval-governance
parent_umbrella_project: P0001-local-rag-system
completion_mode: functional
owner:
created: 2026-05-29
updated: 2026-05-29
current_focus: "Prepare and execute governed Hybrid RAG improvements after the P0001 functional baseline"
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
- Status: active
- Project Role: exception-branch
- Umbrella Initiative: local-rag-system-retrieval-governance
- Parent Umbrella Project: P0001-local-rag-system
- Completion Mode: functional
- Owner:
- Created: 2026-05-29
- Updated: 2026-05-29
- Current Focus: Prepare and execute governed Hybrid RAG improvements after the P0001 functional baseline
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
| T0013 | Retrieval chunking and document authority hardening | Active | 0% | P0: frontmatter/title/status/authority metadata, schema, reindex, stale policy |

## Planned Task Candidates

- `T0014-search-filter-and-answer-context-governance`: P1 search filters, default stale/deprecated exclusion, answer context source priority cues.
- `T0015-answer-quality-and-staleness-evaluation`: P1 evaluation fixture and runner expansion for staleness error, must/must-not-use, answer faithfulness, citation usefulness.
- `T0016-retrieval-audit-observability-expansion`: P2 PostgreSQL audit migration and generated quality debugging reports.
- `T0017-local-reranker-evaluation`: P2 local cross-encoder or embedding-similarity reranker benchmark after metadata and deterministic ranking improvements plateau.

## Overall Progress

- 0%

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
- docs validators, compose config, and relevant Maven tests.

Evidence that is not sufficient alone:

- a single manual query that looks better,
- metadata fields documented but not present in indexed chunks,
- filtering fields accepted in request DTO but not enforced in Weaviate/PostgreSQL query,
- answer prompt wording without source priority metadata in the supplied context,
- hosted API benchmark.

## Outputs / Handoff

- P0002 owns the post-P0001 retrieval governance delivery boundary.
- `T0013` is the first critical-path task and should be worked before search filter or answer evaluation expansion.
- Later tasks should preserve `docs/evaluation/retrieval-quality-cases.yaml` and extend it rather than replacing the T0011 baseline.
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
| G1 | Pending | | Starts with `T0013`. |
| G2 | Pending | | Depends on authority metadata from `T0013`; likely continues in `T0014`. |
| G3 | Pending | | Planned for `T0014`. |
| G4 | Pending | | Planned for `T0015`. |
| G5 | Pending | | Planned for `T0016`. |
| G6 | Pending | | Verified at each task closeout and project closeout. |

## Completion Guardrails

- Do not close P0002 by documenting recommendations only; the metadata and retrieval behavior must be implemented and measured.
- Do not send private source content to hosted rerankers, hosted LLM judges, or external evaluation APIs.
- Do not remove support context merely to make metrics look better; weight and filter it deliberately.
- Do not make deprecated/superseded documents disappear entirely from historical queries; default behavior should exclude or demote them unless explicitly requested.
- Do not break public `hybrid`, `keyword`, and `vector` search modes.
- Do not rewrite P0001's functional baseline goals while moving quality hardening into P0002.

## Risks / Open Questions

- Full metadata schema changes may require destructive Weaviate reindexing; the rollback path must be explicit.
- Git tagging/pushing `v1.0.0` is a separate operator action if a repository tag is desired.
- Local cross-encoder rerank may add memory pressure and should wait until deterministic metadata improvements plateau.
- Evaluation fixtures can become stale as docs change; maintenance rules need to be part of the evaluation task.

## Status

- 2026-05-29: Project issued by explicit user request as the follow-on retrieval governance hardening track after P0001 functional baseline. First critical-path task is `T0013`.
