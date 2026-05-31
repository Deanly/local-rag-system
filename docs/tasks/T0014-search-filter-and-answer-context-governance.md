---
type: task
doc_id: T0014
title: search-filter-and-answer-context-governance
status: done
owner:
created: 2026-05-30
updated: 2026-05-30
current_focus: "Completed metadata filters, stale-source demotion, historical opt-in, and answer source priority cues"
completion_mode: implementation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0002-retrieval-governance-hardening
related_project: docs/projects/P0002-retrieval-governance-hardening.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - docs/projects/P0002-retrieval-governance-hardening.md
  - docs/tasks/T0013-retrieval-chunking-and-document-authority-hardening.md
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalRanker.java
quality_axes:
  - WHOLE
  - SCOPE
  - EVIDENCE
  - SECURITY
  - CONTRACT
tags:
  - docs/task
  - local-rag-system
  - retrieval-governance
  - search-filter
  - answer-context
---

# T0014 search-filter-and-answer-context-governance

- Type: task
- Document ID: T0014
- Status: done
- Completion Mode: implementation
- Owner:
- Created: 2026-05-30
- Updated: 2026-05-30
- Current Focus: Completed metadata filters, stale-source demotion, historical opt-in, and answer source priority cues
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0002-retrieval-governance-hardening
- Related Project: docs/projects/P0002-retrieval-governance-hardening.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`
  - `docs/design/source-registry-and-project-ssot.md`

## Purpose

이 task는 `T0013`에서 만든 document authority metadata를 실제 검색과 답변 context에서 사용하게 만든다.

목표는 사용자가 metadata filter를 지정하면 Weaviate 검색 단계에서 적용하고, 기본 ranking에서는 deprecated/superseded/raw evidence가 current/canonical evidence를 밀어내지 않게 하며, `/api/answer`가 LLM에게 source priority cue를 함께 제공하게 만드는 것이다.

## Task Placement Check

- 이 작업은 P0002의 P1 단계이며, T0013의 metadata substrate를 소비하는 retrieval behavior slice다.
- 별도 project가 아니라 P0002 아래 task가 맞는 이유는 filter/rank/answer context policy가 P0002 검색 거버넌스 목표의 일부이기 때문이다.
- staleness evaluation, audit migration, and optional local reranker evaluation은 후속 task로 분리한다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 registered source boundary와 local-only answer boundary를 유지하면서, retrieval/answer path가 문서 상태와 권위를 실제 판단에 사용하게 만드는 것이다.

깨면 안 되는 invariant:

- 등록되지 않은 source root는 검색 범위에 포함하지 않는다.
- public search modes `hybrid`, `keyword`, `vector`는 유지한다.
- metadata filter와 ranking 변경은 Search API/MCP response를 backward-compatible하게 확장한다.
- deprecated/superseded evidence는 historical query에서 접근 가능해야 한다.
- private source content는 hosted reranker나 hosted judge로 전송하지 않는다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- supported metadata filters가 Weaviate `where` clause로 적용된다.
- current/canonical/accepted evidence가 기본 ranking에서 우선되고 deprecated/superseded/raw/draft evidence는 demote된다.
- historical query 또는 `includeHistorical=true` request는 stale-source penalty를 의도적으로 해제할 수 있다.
- `/api/answer` context가 title/source/status/authority/freshness/supersession cues를 포함한다.
- focused unit tests와 runtime smoke가 filter, ranking, and answer context behavior를 검증한다.

## Scope

- Search API `filters` enforcement for `ssotRole`, `sourceType`, `status`/`frontmatterStatus`, `authority`, `sensitivity`, and `docType`.
- Default governance rerank weight for canonical/current/accepted/reference/raw/deprecated/superseded sources.
- Historical opt-in through request filters or explicit historical query intent.
- Answer context source priority cues: title, source id, ssot role, document type, status, authority, updated, supersededBy.
- Focused service/ranker tests and runtime smoke on registered sources.

## Out Of Scope

- New hosted reranker or hosted judge.
- Full answer faithfulness/staleness evaluation suite; this is planned for `T0015`.
- PostgreSQL audit expansion; this is planned for `T0016`.
- Changing public search modes or Codex tool names.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Metadata filters are enforced. | Supported filter keys produce Weaviate `where` clauses and focused tests verify the query shape. |
| G2 | Stale/low-authority evidence is demoted by default. | Ranker tests show deprecated/superseded/raw candidates lose to current/canonical candidates unless historical use is intended. |
| G3 | Historical queries can opt in. | Request filters or explicit historical query terms disable the stale-source penalty while preserving normal source weighting. |
| G4 | Answer context carries source priority cues. | Prompt/context tests show title/status/authority/updated/supersession metadata is supplied to the chat model. |
| G5 | Public contract remains backward-compatible. | Existing result fields and modes stay unchanged; new behavior is additive. |
| G6 | Evidence is recorded. | Maven tests, docs validators, compose config, and a scoped runtime smoke pass or caveats are explained. |

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Issue task and lock filter contract | Done | 100% | Supported filter keys and historical opt-in rules |
| W2 | Implement filter where clauses | Done | 100% | Supported metadata filters are converted into Weaviate `where` operands |
| W3 | Implement governance rerank weights | Done | 100% | Deprecated/superseded/raw/draft evidence is demoted unless historical use is intended |
| W4 | Implement answer context priority cues | Done | 100% | Answer prompt includes metadata source priority line per context item |
| W5 | Add tests and runtime smoke | Done | 100% | Focused unit tests plus local registered-source smoke passed |
| W6 | Verify and close | Done | 100% | Maven tests, docs validators, compose config, and runtime smoke completed |

## Overall Progress

- 100%

## Implementation Evidence

- Filter enforcement: `SearchRequest.filters` now supports `ssotRole`, `sourceType`, `status`/`frontmatterStatus`, `authority`, `sensitivity`, and `docType` as Weaviate `where` operands.
- Historical opt-in: `includeHistorical=true` or `historical=true` disables stale-source penalty; explicit historical query terms also opt in.
- Governance ranking: `RetrievalRanker` adds `governanceWeight` and demotes `deprecated`, `superseded`, `raw`, `reference`, and `draft` evidence by default while boosting `canonical`, `accepted`, `current`, `active`, and `done` evidence.
- Answer context: `/api/answer` prompt context now includes source priority cues: title, sourceId, ssotRole, docType, status, authority, updated, and supersededBy.
- Runtime filter smoke: `/api/search` with `filters.docType=task` and `filters.status=done` returned only `docType=task`, `status=done` results for `local-rag-system`.
- Runtime answer smoke: `/api/answer` for `T0014 search filter answer context governance purpose` returned `T0014` citations and result metadata.

## Filter Contract

Supported `SearchRequest.filters` keys:

| Key | Weaviate Field | Behavior |
| --- | --- | --- |
| `ssotRole` | `ssotRole` | Include matching source truth roles. |
| `sourceType` | `sourceType` | Include matching registered source types. |
| `status` | `frontmatterStatus` | Alias for document status. |
| `frontmatterStatus` | `frontmatterStatus` | Include matching document statuses. |
| `authority` | `authority` | Include matching authority classes. |
| `sensitivity` | `sensitivity` | Include matching sensitivity classes. |
| `docType` | `docType` | Include matching document types. |
| `includeHistorical` | n/a | `true` disables stale/deprecated/superseded penalty for intentional historical queries. |
| `historical` | n/a | Alias for `includeHistorical`. |

Unsupported filter keys are ignored for backward compatibility.

## Completion Criteria

1. Supported filters are enforced in search query construction.
2. Default ranking demotes deprecated/superseded/raw evidence.
3. Historical opt-in avoids stale-source penalty.
4. Answer prompt includes source priority metadata for every retrieved context item.
5. Focused tests cover filters, governance ranking, and answer context.
6. Docs validators, Maven tests, compose config, and scoped runtime smoke pass or caveats are recorded.

## Completion Evidence

- `mvn -pl services/retrieval-service -am test` passed with focused filter, ranking, historical opt-in, and answer prompt tests.
- `docker compose --env-file .env.example up -d --build retrieval-service` rebuilt the runtime service.
- `/api/search` smoke with `filters.docType=task` and `filters.status=done` returned filtered T0013/T0014 task results.
- `/api/search` smoke with `filters.authority=source-default` and `filters.docType=task` returned task metadata and `governanceWeight`.
- `/api/answer` smoke returned T0014 citations and source metadata.

## Outputs / Handoff

- Filter and ranking behavior feeds `T0015-answer-quality-and-staleness-evaluation`.
- `governanceWeight` and source priority metadata feed `T0016-retrieval-audit-observability-expansion`.
- Historical opt-in policy should be preserved by future local reranker work.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Filter/ranking changes must improve the existing Local RAG pipeline end to end. | Search, ranker, answer prompt, and runtime smoke all use the same metadata. |
| SCOPE | This task should not absorb evaluation/audit/reranker follow-ups. | Out-of-scope and handoff sections split T0015/T0016/T0017 work. |
| EVIDENCE | Ranking policy can regress silently without tests and smoke. | Focused Maven tests and runtime search/answer smoke. |
| SECURITY | Source snippets must remain local-only. | No hosted reranker or hosted judge path is introduced. |
| CONTRACT | Existing clients must survive metadata-aware governance. | Public modes and response shape stay backward-compatible. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `RetrievalService.buildWhere` applies supported metadata filters; `RetrievalServiceTests.buildsWhereClauseWithSupportedMetadataFilters` covers query shape | Unsupported keys remain ignored for backward compatibility |
| G2 | Done | `RetrievalRankerTests.demotesDeprecatedAndSupersededSourcesByDefault` | Demotion keeps recall while making stale evidence lose by default |
| G3 | Done | `RetrievalRankerTests.historicalOptInAllowsDeprecatedSourcesToCompete` | Opt-in works through `includeHistorical`; historical query terms also opt in |
| G4 | Done | `RetrievalServiceTests.answerPromptIncludesSourcePriorityMetadata` and runtime answer smoke | |
| G5 | Done | Existing result fields and modes preserved; metadata and score fields are additive | |
| G6 | Done | Focused Maven tests, runtime search/answer smoke, docs validators, compose config, and diff check | Full P0002 staleness fixture expansion remains `T0015` |

## Completion Guardrails

- Do not remove deprecated/superseded evidence from the index solely to improve default search.
- Do not make historical access implicit for ordinary current-state questions.
- Do not add hosted ranking or hosted answer judging to this path.
- Do not change Codex/MCP tool names or public search modes.
- Do not make unsupported filter keys fail existing clients.

## Status

- 2026-05-30: task issued as P0002 P1 implementation slice after `T0013` completed metadata-aware chunking and search result metadata exposure.
- 2026-05-30: implemented and verified metadata filter enforcement, governance rerank weights, historical opt-in, and answer source priority cues. `T0014` is done; staleness/answer-quality evaluation expansion moves to `T0015`.
