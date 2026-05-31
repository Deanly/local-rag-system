---
type: design
title: retrieval-quality-improvement-design
status: current
domain: retrieval-quality
owner:
created: 2026-05-25
updated: 2026-05-31
retrieval_class:
  - domain-current
context:
  default_load: false
  section_load: false
  evidence_only: false
  size_tier: medium
referenced_by:
  - docs/projects/P0001-local-rag-system.md
  - docs/projects/P0002-retrieval-governance-hardening.md
  - docs/tasks/T0011-retrieval-quality-hardening.md
  - docs/tasks/T0013-retrieval-chunking-and-document-authority-hardening.md
  - docs/tasks/T0014-search-filter-and-answer-context-governance.md
  - docs/tasks/T0015-answer-quality-and-staleness-evaluation.md
  - docs/tasks/T0016-retrieval-audit-observability-expansion.md
  - docs/tasks/T0017-local-reranker-evaluation.md
source_refs:
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - docs/reports/2026-05-31-local-reranker-evaluation-decision.md
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
  - services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java
  - services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java
tags:
  - docs/design
  - local-rag-system
  - retrieval-quality
  - rerank
  - evaluation
---

# retrieval-quality-improvement-design

- Type: design
- Domain: retrieval-quality
- Owner:
- Created: 2026-05-25
- Updated: 2026-05-31
- Referenced By:
  - `docs/projects/P0001-local-rag-system.md`
  - `docs/projects/P0002-retrieval-governance-hardening.md`
  - `docs/tasks/T0011-retrieval-quality-hardening.md`
  - `docs/tasks/T0013-retrieval-chunking-and-document-authority-hardening.md`
  - `docs/tasks/T0014-search-filter-and-answer-context-governance.md`
  - `docs/tasks/T0015-answer-quality-and-staleness-evaluation.md`
  - `docs/tasks/T0016-retrieval-audit-observability-expansion.md`
  - `docs/tasks/T0017-local-reranker-evaluation.md`

## Context

Local RAG의 functional baseline은 완성되어 있다. 현재 시스템은 등록 source를 indexing하고, Weaviate `hybrid`, `keyword`, `vector` search를 제공하며, Codex는 citation과 source-safe document fetch를 사용할 수 있다.

다음 문제는 검색 품질이다.

현재 baseline의 핵심 판단:

- `hit@5`는 실사용 가능한 수준이다.
- `hit@1`은 자동화가 단독으로 신뢰하기에 부족하다.
- Korean natural language query에서는 keyword 품질이 낮고 vector/hybrid 의존도가 높다.
- project-scoped search에서 default context가 primary project source보다 앞서는 경우가 있다.
- reranker, source weighting, document-level diversity, 정식 evaluation harness가 없다.

이 설계는 검색 품질 개선의 current truth를 고정한다. 2026-05-25 기준 `T0011-retrieval-quality-hardening`에서 evaluation harness, deterministic source weighting, metadata/path rerank, and document diversity control이 구현됐다. 2026-05-31 기준 `P0002`의 `T0013`-`T0017`이 metadata-aware chunking, document authority metadata, metadata filters, stale-source demotion, answer source priority cues, staleness/citation evaluation checks, search audit observability, and local reranker deployment decision을 완료했다.

## Whole-System Role

이 설계의 역할은 Local RAG를 "검색은 되는 MVP"에서 "Codex가 안정적으로 활용할 수 있는 retrieval substrate"로 올리는 것이다.

전체 시스템 목표와의 연결:

- Local-only invariant를 유지한다.
- Source registry boundary를 우회하지 않는다.
- Project current truth가 support context보다 우선되게 한다.
- 검색 품질 개선을 감으로 하지 않고 evaluation harness로 회귀 측정한다.
- Rerank와 chunking 개선은 retrieval API 뒤에 숨겨 Codex/MCP tool contract를 흔들지 않는다.

`docs/design/control-plane.md`의 `Improve` stage가 이 설계의 control surface다.

## Boundary

포함하는 책임:

- retrieval evaluation harness contract
- primary source weighting and support context demotion
- local-only reranker architecture
- Markdown chunking quality improvement
- duplicate and diversity control
- multilingual query handling policy
- search observability and audit metric expansion
- staged rollout plan

포함하지 않는 책임:

- source registry의 등록 정책 자체 변경
- hosted external reranker or hosted LLM API 사용
- UI 구현
- PDF/OCR/canvas parsing
- Weaviate replacement
- Codex MCP tool names 변경

외부 시스템과의 경계:

- Ollama embedding/chat endpoint는 local 또는 LAN-local operator prerequisite이다.
- Weaviate는 first-stage retrieval engine으로 유지한다.
- PostgreSQL은 evaluation result, audit, and registry state의 control store가 될 수 있다.
- File source는 read-only mount로만 접근한다.

## Domain Model

주요 엔티티:

- `RetrievalEvalCase`: query, project id, mode, expected source/document, language, intent를 가진 평가 케이스.
- `RetrievalEvalRun`: 특정 runtime/config/model에서 evaluation set을 실행한 결과.
- `RetrievalCandidate`: Weaviate first-stage result.
- `WeightedCandidate`: source/role/path/metadata boost가 적용된 candidate.
- `RerankedCandidate`: local reranker 결과가 적용된 candidate.
- `FinalSearchResult`: diversity and duplicate suppression 후 API로 반환되는 result.
- `SourceWeightPolicy`: primary source, default context, archive, compiled wiki의 rank weight.
- `ChunkMetadata`: title, frontmatter status, heading hierarchy, document type, updated date.
- `SearchPhaseTiming`: embedding, Weaviate, weighting, rerank, finalization latency breakdown.

주요 값 객체:

- `HitAtK`: expected document이 top K 안에 들어왔는지.
- `MRR`: expected document의 reciprocal rank.
- `SourceAccuracy`: 기대 source 또는 primary source가 top result에 있는지.
- `CitationUsefulness`: citation이 operator가 읽을 수 있는 실제 근거인지.
- `LanguageClass`: `identifier-heavy`, `english`, `korean`, `mixed`.
- `IntentClass`: `task lookup`, `design lookup`, `report lookup`, `concept lookup`, `status lookup`.

주요 pipeline:

```text
query
  -> source scope resolution
  -> first-stage retrieval from Weaviate
  -> source/role/path weighting
  -> optional local rerank
  -> duplicate and diversity control
  -> citation/snippet finalization
  -> audit and evaluation metrics
```

## Invariants

- Private source content must not be sent to hosted APIs.
- Evaluation must use the same public search modes as runtime: `hybrid`, `keyword`, `vector`.
- `bm25` remains only a legacy alias for `keyword`, not a new public mode.
- `projectId` search must prefer the registered project's `primary_source_id`.
- `default_context` sources are recall support, not equal-rank project truth.
- Reranker must be optional and fail closed to first-stage retrieval when unavailable.
- Search API results must keep source metadata, citation, heading, snippet, and score information.
- Ranking changes must be measured against a versioned query set before being called an improvement.
- Evaluation fixtures must not contain raw private source content beyond short query strings and expected paths.

## Failure Boundaries

Handled inside retrieval-quality layer:

- Reranker timeout falls back to first-stage weighted results.
- Evaluation case references a missing expected document and marks the case invalid.
- Query returns zero results and records a miss without failing the whole run.
- Weighting policy config is absent and defaults to conservative primary-source boost.

Escalated to operator or task:

- embedding endpoint unavailable,
- Weaviate schema missing required metadata,
- source registry missing project primary source,
- evaluation set reveals severe regression after a ranking change.

Never silently swallowed:

- hosted API call with private source snippets,
- primary project source being excluded from project-scoped search,
- stale deleted source document appearing in top results,
- audit failure that prevents quality regression analysis.

## Interfaces

### Evaluation CLI

Initial harness can be a script under `docs/bin/` or `tools/` that calls the public gateway API.

Required input shape:

```yaml
version: 1
cases:
  - id: rq-en-001
    projectId: local-rag-system
    language: english
    intent: task-lookup
    query: codex rag utilization hardening rag_get_document unknown project error
    expected:
      sourceId: local-rag-system.docs
      relativePath: tasks/T0010-codex-rag-utilization-hardening.md
    modes:
      - hybrid
      - keyword
      - vector
```

Required output metrics:

```json
{
  "runId": "2026-05-25T12:00:00Z",
  "mode": "hybrid",
  "caseCount": 25,
  "hitAt1": 17,
  "hitAt5": 25,
  "mrr": 0.79,
  "sourceAccuracyAt1": 0.84,
  "avgLatencyMs": 230
}
```

### Retrieval Service Internal Interfaces

Suggested internal seams:

```java
interface CandidateRetriever {
    List<RetrievalCandidate> retrieve(SearchRequest request, SearchScope scope);
}

interface CandidateWeighter {
    List<WeightedCandidate> apply(SearchRequest request, SearchScope scope, List<RetrievalCandidate> candidates);
}

interface CandidateReranker {
    List<RerankedCandidate> rerank(SearchRequest request, List<WeightedCandidate> candidates);
}

interface ResultDiversifier {
    List<SearchResultItem> diversify(SearchRequest request, List<RerankedCandidate> candidates);
}
```

These seams should remain inside `retrieval-service` unless a reranker needs a separate process for model runtime isolation.

### Search API Contract

Public request fields can remain compatible:

```json
{
  "projectId": "local-rag-system",
  "query": "retrieval quality hardening",
  "limit": 5,
  "mode": "hybrid",
  "includeSourceIds": [],
  "excludeSourceIds": [],
  "filters": {}
}
```

Future optional fields may be added only if backward compatible:

```json
{
  "rerank": true,
  "debugScores": false,
  "candidateLimit": 30
}
```

## Source Weighting Policy

When `projectId` is present, source weights should be applied after first-stage retrieval.

Initial deterministic policy:

| Candidate Source Role | Recommended Effect |
| --- | --- |
| registered `primary_source_id` | strong boost |
| same project non-primary source | mild boost |
| `default_context` compiled wiki | support, mild demotion unless query explicitly targets wiki/synthesis |
| archive or legacy source | strong demotion unless explicitly included |
| inactive source | never returned |

The implementation should preserve recall by retrieving from all resolved sources, then adjust ranking before final result selection.

Do not implement source boosting by removing compiled support sources from default context. The support layer is useful; the problem is equal-rank competition with project current truth.

Current implementation:

- `retrieval-service` expands the first-stage candidate window before final ranking.
- `RetrievalRanker` boosts registered `primary_source_id`, mildly boosts same-project current truth, and demotes default support context.
- The policy is deterministic and local-only; it does not call a hosted reranker or hosted LLM.

## Reranking Architecture

Reranking should be staged.

### Stage 1: Deterministic Local Rerank

Use metadata and current result score:

- source role,
- source priority,
- relative path class (`design`, `tasks`, `projects`, `reports`, `guide`),
- exact filename/task id match,
- heading match,
- document status/frontmatter when available,
- duplicate document count.

This stage is fast, local, deterministic, and easy to test.

`T0011` implemented this as the active MVP. It uses existing source registry metadata, `relativePath`, `headingPath`, task id/path token matching, and a small Korean-to-English alias map for common document intent terms.

### Stage 2: Local Embedding Similarity Rerank

Use query embedding and candidate chunk embeddings or generated metadata embeddings to normalize first-stage scores. This is useful if Weaviate score shapes differ across modes.

This stage must avoid re-embedding every candidate on every request. It should prefer stored vectors or metadata fields.

### Stage 3: Local Cross-Encoder Rerank

Use a local reranker model only if an expanded portable fixture shows default-mode ranking regressions that deterministic governance ranking cannot fix, and an operationally acceptable model is selected. This may run as:

- a library inside `retrieval-service` only if Java runtime support is simple,
- a sidecar service if model runtime dependency would pollute the Spring Boot service,
- a local HTTP service bound to `127.0.0.1` or Docker internal network.

Cross-encoder rerank should operate on top N candidates, not the whole corpus.

Current decision:

- `T0017` decided not to ship a separate local model reranker in the P0002 deployment path.
- The portable fixture reached 100% hit@1 for `hybrid` and `vector`; the only top1 misses were Korean natural-language `keyword` cases where the expected document was rank 2.
- Future model reranker work needs a new task, local-only runtime proof, and an expanded fixture showing repeated `hybrid` or `vector` top1/source/staleness regressions.

### Stage 4: Local LLM Judge Rerank

LLM judge rerank is optional and should not be the default baseline because it is slower and less deterministic. It may be useful for answer generation or high-stakes manual query mode.

## Chunking Improvement

The current chunker should evolve from fixed heading-aware text windows to Markdown-aware chunk construction.

Required improvements:

- parse YAML frontmatter,
- extract document title and status,
- maintain full heading hierarchy, not only last heading text,
- include title and heading context in chunk text or metadata,
- add small overlap between adjacent chunks,
- avoid splitting tables and code blocks when practical,
- normalize heading ids for citation stability,
- estimate tokens more accurately than `length / 4`,
- preserve document type from path/frontmatter.

Chunk metadata should support ranking:

```text
doc_type
frontmatter_status
title
heading_path
heading_depth
updated
source_priority
source_role
```

Current implementation:

- `indexer-service` parses YAML frontmatter for title, type, status, authority, updated, supersedes, supersededBy, and tags.
- Missing frontmatter uses conservative defaults: first H1 or filename for title, path-derived document type where possible, `unknown` status, and `source-default` authority unless source role is clearly archive/raw.
- Chunk content is prepended with generated `chunkContext` containing title, document metadata, and full heading path, so a retrieved chunk carries enough context to stand alone.
- `headingPath`, `headingPathSegments`, `headingDepth`, and `headingSlug` are stored in Weaviate and exposed through search result metadata.
- PostgreSQL `document_state.metadata_version=1` forces previously indexed documents through the metadata migration on the next registered-source scan.
- Search results expose the new fields under a backward-compatible `metadata` map while preserving the existing result fields.

## Duplicate And Diversity Control

The final result list should avoid returning many chunks from the same document unless the query clearly asks for a deep dive.

Initial policy:

- at most 2 chunks per document in top 5,
- prefer the highest scoring chunk for each document first,
- allow additional chunks from the same document after other matching documents are represented,
- dedupe identical `contentHash`,
- include `documentId` in result metadata so clients can call `rag_get_document`.

This improves Codex behavior because it sees a broader evidence set before deciding which full document to fetch.

`T0011` implements this policy for the final ranked result list using `documentId` and `contentHash`.

## Multilingual Query Handling

The corpus contains English identifiers, Korean prose, and mixed project vocabulary. The policy should be:

- keep `hybrid` as default,
- do not rely on `keyword` for Korean-only query text,
- preserve exact identifier matching for task ids, filenames, model names, source ids, and API names,
- use vector retrieval for semantic Korean queries,
- consider query expansion only after evaluation harness proves a repeatable gap.

Possible future query expansion:

- normalize Korean/English project aliases,
- add path-aware synonyms such as "설계" -> `design`, "작업" -> `tasks`, "보고서" -> `reports`,
- expand "문서" queries toward `README`, `guide`, `design`, or `task` based on intent.

## Observability And Audit

`search_audit` should evolve from result count logging to quality debugging.

Recommended fields:

| Field | Purpose |
| --- | --- |
| `request_id` | correlate logs and audit |
| `candidate_limit` | distinguish first-stage depth from final limit |
| `embedding_latency_ms` | capture query embedding cost |
| `weaviate_latency_ms` | capture retrieval backend cost |
| `weighting_latency_ms` | deterministic rerank cost |
| `rerank_latency_ms` | model rerank cost |
| `total_latency_ms` | client-visible backend time |
| `raw_candidate_count` | first-stage result count |
| `final_result_count` | final returned result count |
| `source_distribution` | count of final results by source id |
| `top_result_source_id` | quick source drift signal |
| `top_result_relative_path` | quick regression triage |

Evaluation run output should not depend only on `search_audit`, but audit should make field debugging possible.

Current implementation:

- `docs/bin/validate-retrieval-quality.sh` reports top result source/path, sources searched, result counts, measured client latency, miss list, must-use pass rate, must-not-use pass rate, citation usefulness, staleness error count, and optional unknown-project skips.
- Search result score maps include `baseScore`, `sourceWeight`, `pathWeight`, `matchWeight`, `governanceWeight`, `rerankScore`, `rawRank`, `rawCandidateCount`, and `finalResultCount`.
- `T0016` expanded PostgreSQL `search_audit` with `candidate_limit`, raw/final counts, embedding/Weaviate/weighting/total latency, source distribution, top source/path, and top score JSON. The retrieval service applies this schema additively at runtime before the first audit write.
- `T0017` added `top1_misses` to the retrieval quality runner output and fixed a Korean retrieval-quality query drift with deterministic query expansion/path weighting before deciding against a separate local model reranker.

## Artifact Contracts

Authoritative artifacts:

- `docs/reports/2026-05-25-retrieval-quality-baseline.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/projects/P0002-retrieval-governance-hardening.md`
- `docs/tasks/T0011-retrieval-quality-hardening.md`
- `docs/tasks/T0013-retrieval-chunking-and-document-authority-hardening.md`
- `docs/tasks/T0014-search-filter-and-answer-context-governance.md`
- `docs/tasks/T0015-answer-quality-and-staleness-evaluation.md`
- `docs/tasks/T0016-retrieval-audit-observability-expansion.md`
- `docs/tasks/T0017-local-reranker-evaluation.md`
- `docs/reports/2026-05-31-local-reranker-evaluation-decision.md`
- `docs/evaluation/retrieval-quality-cases.yaml`
- `docs/bin/validate-retrieval-quality.sh`

Expected future artifacts:

- generated local report paths for evaluation runs.
- future local model reranker task only if expanded default-mode regression evidence justifies it.

## Quality Axes

| Axis | Why It Matters | Regression Signal |
| --- | --- | --- |
| EVIDENCE | Quality changes must be measured, not asserted. | No evaluation run accompanies ranking changes. |
| CONTRACT | API/MCP contract must remain stable while internals improve. | Tool schema or response fields drift from docs. |
| SECURITY | Rerank must remain local-only. | Source snippet is sent to hosted API. |
| HANDOFF | Codex must know when RAG evidence is good enough. | Results lack citation/source metadata or score explanation. |
| WHOLE | Retrieval quality must preserve source registry truth. | support context outranks primary project truth systematically. |

## Decisions

| Decision | Rationale |
| --- | --- |
| Keep Weaviate as first-stage retriever. | Current stack already supports BM25/vector/hybrid and source filtering. |
| Add evaluation harness before ranking changes. | Without hit/MRR baseline, quality work becomes anecdotal. |
| Implement deterministic source weighting before model rerank. | It directly addresses primary-source drift and is easy to test locally. |
| Keep reranker local-only and optional. | Private source content must not leave local/network-local infrastructure. |
| Do not ship a separate local model reranker in P0002. | T0017 evidence shows current default hybrid/vector quality is deployable, while model rerank would add memory/latency surface. |
| Improve chunking incrementally. | Reindexing all sources is acceptable, but chunk schema changes should be measurable and reversible. |
| Defer PostgreSQL audit schema expansion after evaluation-output observability exists. | It avoids a migration before the exact phase timing fields are proven useful. |

## Rollout Strategy

1. Done: Create evaluation fixture and runner.
2. Done: Record baseline metrics on current runtime.
3. Done: Implement deterministic source weighting and diversity control behind current search API.
4. Done: Re-run evaluation and compare metrics.
5. Done: `T0013` added chunk metadata parsing, document authority metadata, additive schema migration, reindex validation, and search result metadata exposure.
6. Done: Add search filters and answer context source-priority governance.
7. Done: Expand evaluation for stale/deprecated misuse, source-use checks, citation usefulness, and bounded local-only answer faithfulness substitutes.
8. Done: Expand audit fields once ranking phases exist.
9. Done: Evaluate local reranker deployment decision after deterministic improvements plateau; do not add separate model reranker to P0002.

## Open Questions

- How should committed baseline reports relate to generated local JSON reports from the runner?
- Should evaluation results be committed as reports or generated locally only?
- Which local cross-encoder reranker is acceptable remains a future question only if expanded default-mode evaluation shows a measurable need.
- Should source weighting remain code-defined from registry metadata, or become a registry/config policy?
- Should compiled support-source demotion stay default-context-only, or become intent-aware for explicit wiki/synthesis queries?

## Change Log

- 2026-05-25: design created from retrieval quality baseline. It locks evaluation-first retrieval improvement, primary source weighting, local-only rerank, chunking improvement, duplicate control, and audit expansion as the next quality path.
- 2026-05-25: `T0011` implemented the evaluation runner, deterministic primary-source/source-role/path rerank, and document diversity control. Chunking and audit schema expansion remain follow-up work.
- 2026-05-29: `P0002` and `T0013` added as the active retrieval governance hardening path for Markdown/frontmatter-aware chunking, document authority metadata, and reindex validation.
- 2026-05-30: `T0013` completed metadata-aware Markdown chunking, document authority metadata indexing, schema migration, reindex, and search result metadata exposure.
- 2026-05-30: `T0014` and `T0015` completed metadata filter enforcement, stale-source demotion, answer source priority cues, and deterministic staleness/source-use/citation evaluation expansion.
- 2026-05-30: `T0016` completed PostgreSQL search audit and runtime observability expansion for retrieval debugging.
- 2026-05-31: `T0017` completed the local reranker deployment decision. P0002 ships deterministic governance ranking and defers separate local model reranker work until expanded default-mode regression evidence exists.
