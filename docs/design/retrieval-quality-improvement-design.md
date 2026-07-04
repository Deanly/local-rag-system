---
type: design
title: retrieval-quality-improvement-design
status: current
domain: retrieval-quality
owner:
created: 2026-05-25
updated: 2026-07-04
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
  - docs/projects/P0003-scoregate-adaptive-context-selection.md
  - docs/tasks/T0021-scoregate-offline-selector-experiment.md
  - docs/tasks/T0022-scoregate-offline-evaluation-fixture.md
  - docs/tasks/T0023-local-cross-encoder-score-source.md
  - docs/tasks/T0024-local-cross-encoder-sidecar-proof.md
  - docs/tasks/T0025-recontext-context-grounding.md
  - docs/tasks/T0026-recontext-grounding-benchmark.md
source_refs:
  - https://arxiv.org/abs/2607.02509
  - "ai-paper-product-fit-research:sources/papers/2607.02509-recontext/fulltext.md"
  - "ai-paper-product-fit-research:docs/research/papers/2607.02509-recontext/summary.md"
  - "ai-paper-product-fit-research:docs/research/papers/2607.02509-recontext/eval-card.md"
  - https://arxiv.org/abs/2606.14269
  - ~/Workspace/personal-assistant-wiki/inbox/2026-06-15-agent-system-paper-scrap.md
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - docs/reports/2026-05-31-local-reranker-evaluation-decision.md
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
  - docs/projects/P0003-scoregate-adaptive-context-selection.md
  - docs/tasks/T0021-scoregate-offline-selector-experiment.md
  - docs/tasks/T0022-scoregate-offline-evaluation-fixture.md
  - docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md
  - docs/tasks/T0023-local-cross-encoder-score-source.md
  - docs/tasks/T0024-local-cross-encoder-sidecar-proof.md
  - docs/tasks/T0025-recontext-context-grounding.md
  - docs/tasks/T0026-recontext-grounding-benchmark.md
  - docs/reports/2026-07-04-recontext-answer-grounding-benchmark.md
  - services/retrieval-service/src/main/java/com/localrag/retrieval/AnswerEvidencePacker.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/AnswerGroundingBenchmarkTests.java
  - docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md
  - docs/reports/2026-06-16-scoregate-before-after-comparison.md
  - docs/reports/2026-06-16-scoregate-sidecar-proof-smoke.md
  - docs/evaluation/scoregate-offline-cases.json
  - docs/evaluation/scoregate-runtime-probes.json
  - docs/bin/validate-scoregate-offline.sh
  - docs/bin/collect-scoregate-runtime-snapshot.py
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/LocalRerankerClient.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/HttpLocalRerankerClient.java
  - services/reranker-sidecar/README.md
  - services/reranker-sidecar/app/main.py
  - services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateCandidateSelectorTests.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateOfflineEvaluationTests.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/RetrievalServiceTests.java
  - services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java
  - services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java
tags:
  - docs/design
  - local-rag-system
  - retrieval-quality
  - rerank
  - evaluation
  - recontext
---

# retrieval-quality-improvement-design

- Type: design
- Domain: retrieval-quality
- Owner:
- Created: 2026-05-25
- Updated: 2026-07-04
- Referenced By:
  - `docs/projects/P0001-local-rag-system.md`
  - `docs/projects/P0002-retrieval-governance-hardening.md`
  - `docs/tasks/T0011-retrieval-quality-hardening.md`
  - `docs/tasks/T0013-retrieval-chunking-and-document-authority-hardening.md`
  - `docs/tasks/T0014-search-filter-and-answer-context-governance.md`
  - `docs/tasks/T0015-answer-quality-and-staleness-evaluation.md`
  - `docs/tasks/T0016-retrieval-audit-observability-expansion.md`
  - `docs/tasks/T0017-local-reranker-evaluation.md`
  - `docs/projects/P0003-scoregate-adaptive-context-selection.md`
  - `docs/tasks/T0021-scoregate-offline-selector-experiment.md`
  - `docs/tasks/T0022-scoregate-offline-evaluation-fixture.md`
  - `docs/tasks/T0023-local-cross-encoder-score-source.md`
  - `docs/tasks/T0024-local-cross-encoder-sidecar-proof.md`
  - `docs/tasks/T0025-recontext-context-grounding.md`
  - `docs/tasks/T0026-recontext-grounding-benchmark.md`

## Context

Local RAG의 functional baseline은 완성되어 있다. 현재 시스템은 등록 source를 indexing하고, Weaviate `hybrid`, `keyword`, `vector` search를 제공하며, Codex는 citation과 source-safe document fetch를 사용할 수 있다.

다음 문제는 검색 품질이다.

현재 baseline의 핵심 판단:

- `hit@5`는 실사용 가능한 수준이다.
- `hit@1`은 자동화가 단독으로 신뢰하기에 부족하다.
- Korean natural language query에서는 keyword 품질이 낮고 vector/hybrid 의존도가 높다.
- project-scoped search에서 default context가 primary project source보다 앞서는 경우가 있다.
- reranker, source weighting, document-level diversity, 정식 evaluation harness가 없다.

이 설계는 검색 품질 개선의 current truth를 고정한다. 2026-05-25 기준 `T0011-retrieval-quality-hardening`에서 evaluation harness, deterministic source weighting, metadata/path rerank, and document diversity control이 구현됐다. 2026-05-31 기준 `P0002`의 `T0013`-`T0017`이 metadata-aware chunking, document authority metadata, metadata filters, stale-source demotion, answer source priority cues, staleness/citation evaluation checks, search audit observability, and local reranker deployment decision을 완료했다. 2026-06-16 기준 `P0003`는 ScoreGate를 final context selection 개선 후보로 검토했고 selector/evaluator artifact를 남겼지만, 현재 profile에 true local cross-encoder `r_i` score source가 없어 default runtime rollout은 no-ship으로 닫았다. 이후 `T0024`는 local reranker sidecar와 explicit debug/opt-in proof path를 추가했으며, default `rag_search` promotion은 real `r_i` snapshot calibration 전까지 보류한다. 2026-07-04 기준 `T0025`는 ReContext의 grounded evidence replay 개념을 full paper method가 아니라 prompt-only answer context packing으로 제한해 `/api/answer`에 적용했다. 같은 날 `T0026`은 replay block을 query-aware, budget-bounded evidence selection(`AnswerEvidencePacker`)으로 확장하고 baseline 대비 deterministic grounding benchmark를 추가했다.

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
- `ScoreGateCandidate`: normalized bi-encoder similarity `s_i`, normalized local cross-encoder reranker score `r_i`, and metadata를 가진 adaptive selection candidate.
- `ScoreGateDecision`: bucket, fusion score, retained flag, and decision reason을 가진 final context selection decision.
- `FinalSearchResult`: diversity and duplicate suppression 후 API로 반환되는 result.
- `AnswerEvidenceReplay`: `/api/answer` prompt에서 final search results의 citation-bearing snippets를 중복 제거해 질문 가까이에 재제시하는 grounded evidence block.
- `AnswerEvidencePacker`: deterministic query-overlap 점수로 replay evidence를 선택·순서화하고 char/count budget을 지키는 answer-only packing 컴포넌트. lexical grounding이 없으면 dedupe된 retrieval order로 fail-open한다.
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
  -> optional adaptive context selection
  -> duplicate and diversity control
  -> citation/snippet finalization
  -> answer-only grounded evidence replay
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
- Answer evidence replay must use only retrieved citation-bearing snippets and must preserve the full retrieved context block for fallback and inspection.
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

### Answer API Context Packing

`/api/answer` may transform final `SearchResultItem` rows into a prompt-only context packet, but it must not change the search response contract. As of `T0025`, answer generation receives two grounded context views:

1. `Grounded evidence replay`: deduplicated citation-bearing snippets placed close to the question to emphasize likely supporting spans. As of `T0026`, `AnswerEvidencePacker` selects and orders this block by deterministic query-term overlap, drops zero-overlap candidates when any candidate is grounded, and enforces a replay char budget (1600) and evidence count budget (6). When no candidate overlaps the query, the block fails open to deduplicated retrieval order.
2. `Retrieved context`: the full final result list with citation and source priority metadata preserved.

Replay selection quality is benchmarked without an LLM by `AnswerGroundingBenchmarkTests` (baseline replay-everything arm vs packer arm) using rule-based proxy metrics: expected-evidence hit, distractor exclusion, first evidence position, and replay chars. Results are recorded in `docs/reports/2026-07-04-recontext-answer-grounding-benchmark.md`.

This is a conservative ReContext-inspired adaptation. It does not implement model-internal attention readout, recursive token scoring, KV-cache replay, or official ReContext code. The official code URL in the paper was still HTTP 404 at `T0025` issue time, so local product evidence must come from Local RAG tests and future evaluation fixtures, not from paper benchmark transfer.

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

### ScoreGate Adaptive Context Selection

ScoreGate is a final context selection strategy after first-stage retrieval and local reranker scoring. It is not a replacement for deterministic source/path/governance ranking, and the current `RetrievalRanker.rerankScore` is not a valid cross-encoder `r_i`.

Required score contract:

| Score | Meaning | Local RAG Requirement |
| --- | --- | --- |
| `s_i` | normalized bi-encoder or first-stage semantic similarity | Must be a normalized similarity score in `[0, 1]`, not a raw Weaviate score shape that changes by mode. |
| `r_i` | normalized local cross-encoder relevance score | Must come from a true local reranker that jointly reads query and chunk, or the runtime integration must remain disabled. |
| source/path/governance score | deterministic project/source authority signal | Must remain separate metadata, not fused as `r_i`. |

Bucket policy:

| Bucket | Condition | Decision |
| --- | --- | --- |
| B1 | high `s_i`, high `r_i` | retain |
| B2 | high `s_i`, low `r_i` | retain only when fusion crosses the stricter B2 threshold |
| B3 | low `s_i`, high `r_i` | rescue when fusion crosses the lower B3 threshold |
| B4 | low `s_i`, low `r_i` | discard |

Reference formula:

```text
f_i = alpha * s_i + (1 - alpha) * r_i
```

The paper reference example uses `alpha=0.3`, `tau_s=0.70`, `tau_r=0.08`, `theta_B2=0.255`, `theta_B3=0.15`, and `MAX-K=10`. These are reference defaults only. Local RAG must calibrate thresholds against its own corpus, `qwen3-embedding:4b`, Weaviate mode scores, and selected local cross-encoder model.

Current implementation:

- `T0021` added `ScoreGateCandidateSelector` as a package-private pure selector for offline experiments.
- The selector accepts normalized `s_i`/`r_i`, returns bucket/fusion/retained/reason decisions, and applies MAX-K by fusion score.
- Focused tests cover bucket classification, B3 rescue, disagreement thresholds, MAX-K cap, and normalized-score validation.
- `T0022` added an offline snapshot fixture and validator that use the actual Java selector to report hit@1, hit@5, MRR, source accuracy@1, fixed top-K miss rescue, retained token estimate, B3 rescue count, and multi-hop coverage.
- `T0023` checked the current profile for local cross-encoder score sourcing and closed runtime ScoreGate as no-ship because no true local `r_i` source is available.
- `T0024` adds the proof substrate for the no-ship blocker: optional local reranker sidecar, retrieval-service timeout/fallback client, request-level `filters.scoreGate=debug|on`, runtime probes, and a snapshot collector.
- No `rag_search` public contract or default runtime behavior changed in `T0021`.
- No `rag_search` public contract or default runtime behavior changed in `T0022`.
- No `rag_search` public contract or default runtime behavior changed in `T0023`.
- No default `rag_search` behavior changes in `T0024`; ScoreGate remains explicit opt-in/debug only until real sidecar snapshots and calibration evidence justify promotion.

Runtime decision:

- Keep the selector and offline evaluator as reusable artifacts.
- P0003 remains no-ship for default runtime ScoreGate.
- T0024 may provide explicit debug/opt-in runtime proof paths, but only with local/LAN-local cross-encoder score sourcing and fallback.
- Do not use deterministic `RetrievalRanker.rerankScore` as `r_i`.
- Do not promote ScoreGate to default until local cross-encoder snapshots, latency, retained-token, source accuracy, Korean false-negative, stale-demotion, and multi-hop coverage evidence are documented.

Offline experiment requirements before runtime use:

- Extend retrieval-quality fixtures with Korean natural-language queries, English filename/task-id queries, primary-source preference cases, stale/deprecated demotion cases, and broad/multi-hop cases.
- Retrieve top-N candidates with `candidateLimit=30` or `40`.
- Keep `s_i`, `r_i`, and deterministic governance/source scores separate in snapshots.
- Compare hit@1, hit@5, MRR, source accuracy@1, retained chunk count, estimated context tokens, latency, Korean false-negative rate, and multi-hop coverage regression.
- Only consider runtime opt-in after local-only score sourcing and threshold calibration are documented.

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
- `docs/projects/P0003-scoregate-adaptive-context-selection.md`
- `docs/tasks/T0021-scoregate-offline-selector-experiment.md`
- `docs/tasks/T0022-scoregate-offline-evaluation-fixture.md`
- `docs/tasks/T0023-local-cross-encoder-score-source.md`
- `docs/tasks/T0024-local-cross-encoder-sidecar-proof.md`
- `docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md`
- `docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md`
- `docs/reports/2026-06-16-scoregate-before-after-comparison.md`
- `docs/reports/2026-06-16-scoregate-sidecar-proof-smoke.md`
- `docs/reports/2026-05-31-local-reranker-evaluation-decision.md`
- `docs/evaluation/retrieval-quality-cases.yaml`
- `docs/evaluation/scoregate-offline-cases.json`
- `docs/evaluation/scoregate-runtime-probes.json`
- `docs/bin/validate-retrieval-quality.sh`
- `docs/bin/validate-scoregate-offline.sh`
- `docs/bin/collect-scoregate-runtime-snapshot.py`
- `services/reranker-sidecar/README.md`
- `services/reranker-sidecar/app/main.py`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/LocalRerankerClient.java`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/HttpLocalRerankerClient.java`
- `services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateCandidateSelectorTests.java`
- `services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateOfflineEvaluationTests.java`

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
| Treat ScoreGate as offline-first context selection in P0003 and explicit proof-only in T0024. | It requires real normalized `s_i` and local cross-encoder `r_i`; current deterministic composite scores are not sufficient evidence for production behavior. |
| Treat ReContext as prompt-only answer evidence replay until official code or a separate local reimplementation task exists. | T0025 can reuse grounded search snippets without changing retrieval contracts, while the full paper method requires model-internal attention access and unavailable official code. |
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
10. Done: P0003 evaluated ScoreGate as offline-first adaptive context selection. T0021 implemented the pure selector, T0022 added the offline snapshot fixture/validator, and T0023 closed runtime rollout as no-ship for the current profile because no true local cross-encoder `r_i` source exists.
11. In progress: T0024 adds the local reranker sidecar and explicit debug/opt-in proof path needed to collect real `r_i` snapshots without changing default `rag_search`.
12. Done: T0025 adds prompt-only grounded evidence replay to `/api/answer` without changing `rag_search`, retrieval ranking, source registry, or indexing.
13. Done: T0026 upgrades the replay block to query-aware budget-bounded selection with a deterministic baseline-vs-ReContext grounding benchmark; LLM answer-quality A/B remains future work.

## Open Questions

- How should committed baseline reports relate to generated local JSON reports from the runner?
- Should evaluation results be committed as reports or generated locally only?
- Which normalized `s_i` source is stable across Weaviate `hybrid`, `keyword`, and `vector` modes for ScoreGate calibration?
- Whether `BAAI/bge-reranker-v2-m3` on this machine can provide `r_i` with acceptable latency/resource cost remains to be measured through the T0024 sidecar.
- What ScoreGate threshold set preserves multi-hop coverage while reducing retained chunks?
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
- 2026-06-16: `P0003`, `T0021`, `T0022`, and `T0023` evaluated ScoreGate as an offline-first adaptive context selection path. `ScoreGateCandidateSelector` and the offline snapshot validator are implemented and tested; runtime ScoreGate is no-ship for the current profile because no true local cross-encoder `r_i` source exists.
- 2026-06-16: `T0024` added the local cross-encoder sidecar proof substrate, retrieval-service reranker client with fallback, explicit ScoreGate debug/opt-in request mode, runtime probes, and snapshot collection script. Default runtime behavior remains unchanged pending real sidecar calibration evidence.
- 2026-07-04: `T0025` added ReContext-inspired prompt-only grounded evidence replay for `/api/answer` context packing. This preserves search contracts and does not claim full ReContext attention-readout reproduction.
- 2026-07-04: `T0026` added `AnswerEvidencePacker` query-aware budget-bounded replay selection and the deterministic answer grounding benchmark comparing baseline and ReContext packing arms.
