---
type: report
title: retrieval-quality-baseline
status: done
owner:
created: 2026-05-25
updated: 2026-05-25
current_focus: "Baseline retrieval quality measured before source weighting, rerank, and chunking hardening"
report_type: retrieval-quality-baseline
related_project: docs/projects/P0001-local-rag-system.md
related_task:
  - docs/tasks/T0010-codex-rag-utilization-hardening.md
  - docs/tasks/T0011-retrieval-quality-hardening.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
  - services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java
  - services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
quality_axes:
  - EVIDENCE
  - CONTRACT
  - HANDOFF
tags:
  - docs/report
  - local-rag-system
  - retrieval-quality
  - evaluation
---

# 2026-05-25 retrieval-quality-baseline

- Type: report
- Status: done
- Owner:
- Created: 2026-05-25
- Updated: 2026-05-25
- Current Focus: Baseline retrieval quality measured before source weighting, rerank, and chunking hardening
- Report Type: retrieval-quality-baseline
- Related Project: `docs/projects/P0001-local-rag-system.md`
- Related Task:
  - `docs/tasks/T0010-codex-rag-utilization-hardening.md`
  - `docs/tasks/T0011-retrieval-quality-hardening.md`
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`
  - `docs/design/msa-runtime-and-storage.md`

## Summary

현재 Local RAG는 Codex 보조 기억장치로 실사용 가능한 수준이다. project-scoped search에서 필요한 문서는 대부분 top5 안에 들어온다. 그러나 top1만 믿고 자동화를 진행하기에는 아직 부족하다.

핵심 수치:

| Query Set | Mode | Queries | hit@1 | hit@5 | MRR | Source accuracy@1 | Interpretation |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- |
| English / filename-heavy | `hybrid` | 15 | 66.7% | 100.0% | 0.800 | 100.0% | 기본값으로 적합. top5 context loading에는 충분하다. |
| English / filename-heavy | `keyword` | 15 | 86.7% | 93.3% | 0.900 | 100.0% | 파일명과 고유 용어가 들어간 query에서 강하다. |
| English / filename-heavy | `vector` | 15 | 66.7% | 100.0% | 0.822 | 100.0% | semantic backup으로 유효하지만 top1 안정성은 낮다. |
| Korean natural language | `hybrid` | 10 | 50.0% | 100.0% | 0.750 | 70.0% | 한국어 실사용 query에서 top5는 안정적이나 primary source 순위가 흔들린다. |
| Korean natural language | `keyword` | 10 | 30.0% | 50.0% | 0.370 | 60.0% | 한국어 query와 영어 문서 corpus 간 lexical mismatch가 크다. |
| Korean natural language | `vector` | 10 | 60.0% | 100.0% | 0.800 | 100.0% | 한국어에서는 vector가 keyword보다 더 중요하다. |

운영 판단:

- Codex가 `limit: 5` 이상 결과를 보고 citation 기반으로 답하는 용도에는 충분하다.
- 단일 top result만 읽고 자동 수정하거나 결정을 내리는 workflow에는 부적합하다.
- 다음 품질 hardening은 primary source weighting, rerank, evaluation harness, chunking 개선을 중심으로 잘라야 한다.

## Scope

이 보고서는 2026-05-25 현재 운영 Local RAG의 검색 품질 baseline을 기록한다.

포함한 범위:

- 현재 운영 index 상태의 문서/청크 수
- 등록 source별 indexing 상태
- 영어/파일명 중심 query와 한국어 자연어 query의 `hit@1`, `hit@5`
- `hybrid`, `keyword`, `vector` 모드 비교
- source selection, ranking, chunking, audit 관점의 known weakness
- 다음 설계/task로 승격해야 하는 개선 축

포함하지 않은 범위:

- 코드 수정
- 모델 교체 실험
- external hosted reranker 또는 hosted LLM 평가
- 대규모 benchmark dataset 구축
- citation usefulness 자동 산출

## Inputs

Baseline runtime inputs:

- gateway: `http://127.0.0.1:42120`
- registered projects: `support-notes`, `project-a`, `project-b`, `local-rag-system`, `project-c`
- registered sources: `support-notes.wiki`, `project-a.docs`, `project-b.docs`, `local-rag-system.docs`, `project-c.docs`
- embedding model: `qwen3-embedding:4b`
- retrieval engine: Weaviate `LocalRagChunk`
- default public search modes: `hybrid`, `keyword`, `vector`

Current index snapshot:

| Metric | Value |
| --- | ---: |
| Documents | 714 indexed, 1 removed |
| Chunks | 3967 total |
| Failure records | 0 |

Source distribution:

| Source | Indexed Documents | Chunks |
| --- | ---: | ---: |
| `support-notes.wiki` | 147 | 452 |
| `project-a.docs` | 341 | 2422 |
| `project-b.docs` | 135 | 717 |
| `local-rag-system.docs` | 50 | 196 |
| `project-c.docs` | 42 | 183 |

Implementation facts used in this report:

- `RetrievalService` resolves project scope into primary source, default context, and other active project sources.
- `hybrid` search uses Weaviate `hybrid:{query, vector, alpha:0.5}`.
- `keyword` search uses Weaviate BM25.
- `vector` search uses query embedding and `nearVector`.
- `MarkdownChunker` creates heading-aware chunks around a fixed character target.
- `IndexerService` currently calls the chunker with target size `1600`.

## Evaluation Method

The baseline used small labeled query sets, not a production benchmark.

English / filename-heavy set:

- 15 queries
- 3 queries each for `support-notes`, `project-a`, `project-b`, `local-rag-system`, and `project-c`
- Queries intentionally included file names, task ids, design names, or domain phrases.

Korean natural language set:

- 10 queries
- 2 representative Korean queries for each registered project group except broad cross-project search
- Queries approximated how a Korean-speaking operator would ask Codex for local project knowledge.

Metrics:

| Metric | Meaning |
| --- | --- |
| `hit@1` | expected document is the first result |
| `hit@5` | expected document appears anywhere in the first five results |
| `MRR` | reciprocal rank of the expected document averaged across cases |
| `source accuracy@1` | first result comes from the expected source |

The test checked expected document path, not exact heading or snippet relevance. Therefore the numbers are a useful baseline for document discovery, but not enough to prove answer quality.

The automated baseline command was:

```bash
./docs/bin/validate-retrieval-quality.sh
```

It ran against `http://127.0.0.1:42120/api/mcp/rag_search` and produced 75 case/mode observations from the versioned fixture at `docs/evaluation/retrieval-quality-cases.yaml`.

## Findings

### F1. Top5 Retrieval Is Good Enough For Codex Context Loading

`hybrid` and `vector` both achieved `hit@5 = 100%` in the English and Korean sets. This means the system is already useful when Codex reads several cited chunks before answering.

This is the current safe use pattern:

1. run `rag_search` with `mode: "hybrid"` and `limit: 5` or higher,
2. inspect citations,
3. use `rag_get_document` for the most relevant source document,
4. answer or edit with citation-backed context.

### F2. Top1 Ranking Is Not Yet Reliable Enough For Automation

`hybrid` reached `hit@1 = 10/15` for English-like queries and `5/10` for Korean queries in the automated fixture. This is acceptable for interactive search but not for workflows that automatically trust the first result.

Observed miss classes:

- task documents outrank design documents that are the intended current truth,
- compiled wiki or lint documents outrank primary project docs,
- related umbrella/project docs outrank a more specific report,
- semantically related support context appears above the project source document.

### F3. Keyword Search Is Strong For English Identifiers But Weak For Korean Queries

`keyword` performed best on English/file-name queries. It fell sharply on Korean natural language queries.

This is expected because many document titles, filenames, and headings are English identifiers, while the operator may ask in Korean. Korean query support should rely on vector/hybrid retrieval, not keyword-only retrieval.

### F4. Default Context Can Pollute Project-Scoped Top Results

When `projectId` is present, the system searches the primary project source plus registered default context. That is correct for recall, but current ranking does not apply a visible boost to primary source results.

Observed result:

- `support-notes.wiki` lint or compiled pages can appear above project docs.
- support context is sometimes useful evidence, but it should rarely displace project current truth in top rank.

This directly affects Codex behavior because Codex may overfit to a synthesis/lint note instead of the repo `docs/` source of record.

### F5. Rerank Is Not Implemented

The current retrieval service returns Weaviate ranking directly after DTO mapping. There is no local reranker stage, no document-level diversity stage, and no score tuple that separates BM25, vector, source weight, and rerank score.

This keeps MVP simple, but it limits top1 quality and makes regressions hard to reason about.

### F6. Chunking Is Functional But Too Coarse For Quality Tuning

The chunker is heading-aware, but it is not a Markdown AST parser. It does not explicitly extract title/frontmatter/status fields, does not preserve heading hierarchy, does not add overlap, and does not use title or document metadata as weighted retrieval fields.

This works for baseline indexing, but quality tuning needs richer chunk metadata.

### F7. Audit Data Is Useful But Incomplete

`search_audit` records project id, query, mode, sources searched, result count, and latency. Current latency is measured around Weaviate request execution and excludes some client-visible work such as embedding generation.

For retrieval quality hardening, audit should record enough phase timing and source distribution to explain why a query behaved badly.

## Source-Level Observations

| Source | Observation |
| --- | --- |
| `support-notes.wiki` | Useful compiled support layer. Can outrank project docs when included as default context. Needs lower support-context weight in project-scoped search. |
| `project-a.docs` | Large corpus. Related tasks and reports often compete with design docs. Needs current-truth/source-role weighting and document diversity. |
| `project-b.docs` | Similar terms across reports, tasks, and legacy material create ranking ambiguity. Korean query quality depends heavily on vector search. |
| `local-rag-system.docs` | Strong enough for self-querying, but task docs can outrank design docs where source-registry current truth is expected. |
| `project-c.docs` | Small corpus performs well. It is a good smoke source for regression checks because expected documents are clear. |

## Recommendations

1. Add a small versioned evaluation harness.
   - Store labeled query cases in the repo.
   - Track separate sets for English/identifier queries and Korean natural language queries.
   - Report `hit@1`, `hit@5`, MRR, source accuracy, and citation usefulness.

2. Add primary source weighting.
   - If `projectId` is present, boost `primary_source_id`.
   - Treat default context as support evidence with lower rank weight.
   - Do not remove support context; lower its rank unless the query explicitly targets it.

3. Add local-only reranking.
   - Start with deterministic source/role/path/heading score adjustment.
   - Then evaluate local cross-encoder reranker if a Java-friendly or service-friendly local model path is available.
   - Keep LLM judge rerank optional because it is slower and less deterministic.

4. Improve chunking and metadata.
   - Parse frontmatter/title/status.
   - Preserve heading hierarchy.
   - Add chunk overlap for long sections.
   - Add document-level grouping and duplicate suppression.

5. Expand audit.
   - Capture embedding latency, Weaviate latency, rerank latency, total request latency, raw candidate count, final result count, and result source distribution.

## Follow-Up Promotion

Promote this report into:

- `docs/design/retrieval-quality-improvement-design.md`
- `docs/tasks/T0011-retrieval-quality-hardening.md`

Those documents should become the active implementation preparation surfaces for the retrieval quality work.

## Status

- 2026-05-25: baseline report created from live runtime search checks and implementation inspection. No runtime code was changed.
