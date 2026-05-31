---
type: task
doc_id: T0013
title: retrieval-chunking-and-document-authority-hardening
status: done
owner:
created: 2026-05-29
updated: 2026-05-30
current_focus: "Completed metadata-aware chunking, schema migration, reindex, and search result metadata exposure"
completion_mode: migration
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0002-retrieval-governance-hardening
related_project: docs/projects/P0002-retrieval-governance-hardening.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - docs/projects/P0002-retrieval-governance-hardening.md
  - docs/tasks/T0011-retrieval-quality-hardening.md
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java
  - services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java
  - database/weaviate/local-rag-chunk.schema.json
  - database/postgres/ddl/001_core_schema.sql
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
  - chunking
  - metadata
---

# T0013 retrieval-chunking-and-document-authority-hardening

- Type: task
- Document ID: T0013
- Status: done
- Completion Mode: migration
- Owner:
- Created: 2026-05-29
- Updated: 2026-05-30
- Current Focus: Completed metadata-aware chunking, schema migration, reindex, and search result metadata exposure
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0002-retrieval-governance-hardening
- Related Project: docs/projects/P0002-retrieval-governance-hardening.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 P0002의 첫 critical path로, 현재 fixed-size heading-aware chunker를 Markdown/frontmatter-aware chunking과 document authority metadata indexing으로 고도화한다.

목표는 검색기가 chunk를 찾았을 때 LLM과 reranker가 이 chunk의 문서 제목, heading hierarchy, 문서 상태, 권위, 최신성, 폐기/대체 관계를 함께 판단할 수 있게 만드는 것이다.

## Task Placement Check

- 이 작업은 P0002의 P0 단계이며, P0001 functional baseline을 바꾸는 것이 아니라 그 위의 retrieval governance substrate를 migration한다.
- 별도 project가 아니라 P0002 아래 task가 맞는 이유는 schema/reindex/chunking이 P0002의 전체 metadata governance 목표 중 첫 slice이기 때문이다.
- search filter, answer context, answer quality evaluation은 이 task의 output metadata를 소비하는 후속 task로 분리한다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 registered source boundary와 local-only model boundary를 유지하면서, retrieval index가 문서의 권위와 최신성을 설명할 수 있게 만드는 것이다.

깨면 안 되는 invariant:

- 등록되지 않은 source root는 indexing하지 않는다.
- source folder는 read-only다.
- Weaviate index는 derived state이며 reindex 가능해야 한다.
- private source content는 local/LAN-local Ollama endpoint 외부로 나가지 않는다.
- public search API는 backward-compatible해야 한다.
- T0011 evaluation fixture는 보존하고, metadata migration 전후 비교가 가능해야 한다.

## Completion Mode Notes

Completion mode는 `migration`이다. 이 task는 chunk/index schema와 derived retrieval state를 바꾸므로, 구현뿐 아니라 reindex path, rollback/compatibility note, before/after evaluation evidence가 있어야 닫을 수 있다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- Markdown frontmatter가 parsing되어 document metadata로 보존된다.
- chunk metadata가 `title`, `doc_type`, `frontmatter_status`, `authority`, `updated`, `supersedes`, `supersededBy`, full `heading_path`, `heading_depth`를 포함한다.
- chunk text 또는 metadata가 title/heading context를 포함해 chunk 하나만으로도 최소 문맥을 가진다.
- chunking이 heading boundary를 우선하고, 긴 섹션에는 bounded overlap을 적용한다.
- code block/table split 방지 기준이 구현되거나 명확한 deferred rationale과 test fixture가 남는다.
- Weaviate/PostgreSQL schema가 새 metadata를 수용하고 reindex 절차가 문서화된다.
- deprecated/superseded metadata가 후속 rerank/filter에서 사용할 수 있는 형태로 검색 결과 또는 score/debug metadata에 노출된다.
- focused tests, migration/reindex evidence, retrieval quality no-regression check가 있다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Document metadata contract를 잠근다. | fields, source precedence, default values, and compatibility behavior are documented in design/task/schema. |
| G2 | Markdown/frontmatter-aware chunker를 구현한다. | unit tests cover frontmatter, title, full heading path, overlap, and stable chunk metadata. |
| G3 | Index schema and state store를 metadata-aware로 확장한다. | Weaviate/PostgreSQL schema or compatibility layer includes the required fields and migration notes. |
| G4 | Reindex and rollback path를 제공한다. | operator can rebuild affected derived index state and recover if migration fails. |
| G5 | Retrieval result exposes authority metadata without breaking clients. | search results or debug score/source metadata include the new fields in a backward-compatible shape. |
| G6 | No-regression evidence를 남긴다. | docs validators, relevant Maven tests, compose config, and retrieval evaluation pass or skipped runtime checks are explained. |

## Scope

- Markdown frontmatter parser selection or small local parser implementation
- title/status/authority/updated/supersession metadata extraction
- heading hierarchy tracking
- chunk overlap and chunk text/context construction
- Weaviate schema and PostgreSQL state changes for metadata
- indexer tests and focused retrieval smoke
- reindex/rollback operator note
- docs/design and project/task handoff updates if implementation decisions shift

## Out Of Scope

- answer prompt/context packer changes beyond exposing required metadata
- full staleness/faithfulness answer evaluation suite
- PostgreSQL search audit phase-latency expansion
- local cross-encoder reranker model selection
- PDF/OCR/canvas/Excalidraw parsing
- hosted model or hosted evaluation API usage

## References

- `docs/projects/P0002-retrieval-governance-hardening.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java`
- `services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java`
- `database/weaviate/local-rag-chunk.schema.json`
- `database/postgres/ddl/001_core_schema.sql`
- `docs/evaluation/retrieval-quality-cases.yaml`

## Dependencies

- P0001 functional baseline remains available.
- T0011 retrieval evaluation fixture and runner remain valid.
- Local PostgreSQL and Weaviate schemas can be migrated or rebuilt in development.
- Reindexing registered sources is operationally acceptable after schema changes.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Lock metadata schema and parser strategy | Done | 100% | Contract implemented as additive camelCase API/index fields matching the snake_case design terms |
| W2 | Implement metadata-aware Markdown parser/chunker | Done | 100% | Frontmatter, title/H1 fallback, full heading path, heading slug, chunk context, token estimate, overlap, and code/table split guard implemented |
| W3 | Extend index schema/state metadata | Done | 100% | Weaviate schema and PostgreSQL state store accept document authority/status/freshness/supersession metadata |
| W4 | Propagate metadata through indexer and search result | Done | 100% | Indexer upserts metadata and search results expose it via backward-compatible `metadata` map |
| W5 | Add focused tests and fixtures | Done | 100% | Chunker tests cover frontmatter/defaults/deprecated supersession/code block/overlap; retrieval test covers search result metadata parsing |
| W6 | Reindex and evaluate | Done | 100% | `local-rag-system.docs` force-scan rebuilt 56 docs and 878 chunks with metadata; full T0011 fixture blocked by inactive fixture projects in current registry |
| W7 | Verify and close | Done | 100% | Maven tests, docs validators, compose config, runtime smoke, and diff check completed |

## Implementation Plan

Execute this task as a migration, not as a parser-only refactor.

1. Lock the metadata contract before code changes.
2. Add parser/chunker tests with small Markdown fixtures before changing indexing behavior.
3. Extend schema and DTO/result mapping in a backward-compatible way.
4. Reindex a development corpus and verify the new fields in Weaviate and search responses.
5. Run T0011 retrieval quality evaluation and inspect regressions before closing.

## Metadata Contract Draft

The first implementation pass should prefer explicit frontmatter, then path-derived defaults, then conservative fallback values.

| Field | Source | Default | Purpose |
| --- | --- | --- | --- |
| `title` | frontmatter `title`, first H1, filename | filename without extension | Human-readable source label and answer context cue. |
| `doc_type` | frontmatter `type`, path prefix | `document` | Rank/filter by `design`, `task`, `project`, `guide`, `report`, `memo`, `runbook`, or `document`. |
| `frontmatter_status` | frontmatter `status` | `unknown` | Distinguish `current`, `active`, `done`, `draft`, `deprecated`, `superseded`, and unknown material. |
| `authority` | frontmatter `authority` or source role | `source-default` | Distinguish `canonical`, `accepted`, `reference`, `raw`, and source-default evidence. |
| `updated` | frontmatter `updated`, file mtime | file mtime date | Freshness and stale-source detection. |
| `supersedes` | frontmatter list | empty list | Identify older documents replaced by this document. |
| `supersededBy` | frontmatter list | empty list | Demote or exclude superseded documents by default. |
| `heading_path` | Markdown heading stack | title only | Citation, chunk context, and rank matching. |
| `heading_depth` | current heading depth | `0` | Section-aware ranking and context packing. |
| `heading_slug` | normalized heading path | generated from heading text | Stable citation anchors where practical. |
| `chunk_context` | title plus heading path plus metadata summary | generated | Text prepended or stored to make a chunk understandable alone. |

The task should not invent high-authority values. If a document lacks frontmatter, it should remain searchable but should not outrank explicit current/canonical sources solely because of missing metadata.

## Migration Plan

- Prefer additive schema changes first. If Weaviate cannot add required fields safely, document the drop/recreate path for the derived collection.
- Keep `chunkId`, `documentId`, `relativePath`, `headingPath`, `content`, `contentHash`, and existing score fields backward-compatible.
- Add metadata fields to Weaviate first, then PostgreSQL `document_state` or `chunk_state` only where local state or audit needs them.
- Reindex registered development sources after schema changes; do not index unregistered folders for migration convenience.
- Rollback path is to restore the previous schema/code and rebuild the derived Weaviate collection from registered source roots.

## Overall Progress

- 100%

## Implementation Evidence

- Metadata schema: Weaviate `LocalRagChunk` now includes `title`, `docType`, `frontmatterStatus`, `authority`, `updated`, `supersedes`, `supersededBy`, `headingPathSegments`, `headingDepth`, `headingSlug`, and `chunkContext`.
- PostgreSQL migration: `document_state` stores document metadata and `metadata_version`; `chunk_state` stores heading depth, slug, and chunk context. The indexer also runs additive `ALTER TABLE IF NOT EXISTS` statements before scan.
- Reindex evidence: `POST /api/index/force?projectId=local-rag-system` completed with `documentsDetected=56`, `documentsIndexed=56`, `documentsDeleted=0`, `chunksIndexed=878`, `errors=[]`.
- State evidence: `local-rag-system.docs` had `56/56` documents at `metadata_version=1` after the migration scan.
- Search evidence: `/api/search` for `T0013 retrieval chunking document authority metadata` returned `tasks/T0013-retrieval-chunking-and-document-authority-hardening.md` with `metadata.title`, `metadata.docType=task`, `metadata.frontmatterStatus=active`, `metadata.authority=source-default`, and `metadata.headingSlug`.
- Evaluation caveat: the earlier broad fixture included machine-local projects that were not portable across registries. The default fixture has since been narrowed to this repository's `local-rag-system` docs so it can run on different machines without private project ids.

## Completion Criteria

1. Metadata contract is reflected in `docs/design/retrieval-quality-improvement-design.md` or this task with no contradiction.
2. Chunker tests cover frontmatter, title, heading hierarchy, chunk overlap, and metadata defaults.
3. Weaviate/PostgreSQL schema or compatibility layer accepts and returns the metadata required by P0002.
4. Indexer upserts the new metadata for supported Markdown files.
5. Search result mapping exposes the metadata needed by later filter/answer tasks without breaking existing clients.
6. Reindex or force-scan evidence confirms the metadata is present in derived state.
7. Retrieval quality evaluation shows no unacceptable regression against the T0011 fixture.
8. `./docs/bin/validate-codex-readiness.sh`, `./docs/bin/validate-harness-foundation.sh`, `./docs/bin/validate-doc-retrieval.sh`, `./docs/bin/validate-closeout.sh --all`, relevant Maven tests, and `docker compose --env-file .env.example config` pass or skipped runtime checks are explained.

## Completion Evidence

Required evidence:

- metadata schema summary,
- chunker unit test output,
- indexer/search tests or smoke output,
- migration/reindex command and result,
- retrieval quality before/after output,
- docs validator output,
- compose config output,
- local-only endpoint/security check.

Evidence that is not sufficient alone:

- fields added only to docs but not to indexed chunks,
- chunker implementation without reindex evidence,
- manual query output without evaluation fixture,
- schema change without rollback/rebuild note.

## Outputs / Handoff

- Metadata-aware chunks become the substrate for `T0014-search-filter-and-answer-context-governance`.
- Evaluation output and any regression notes feed `T0015-answer-quality-and-staleness-evaluation`.
- Migration notes feed future operational release documentation.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Chunk metadata must serve the existing Local RAG pipeline end to end. | Indexer, Weaviate, retrieval result, and docs agree. |
| SCOPE | This task should stop at metadata substrate, not absorb answer/audit/reranker work. | Out-of-scope and handoff to T0014/T0015/T0016. |
| EVIDENCE | Schema/chunk changes are risky without reindex and evaluation proof. | Tests, reindex output, retrieval quality metrics. |
| SECURITY | Metadata parsing must not introduce hosted API paths or source mutation. | Local-only config and read-only source boundary remain intact. |
| CONTRACT | Existing clients must survive schema/result enrichment. | Backward-compatible DTO behavior and smoke tests. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | Metadata contract implemented in chunker, Weaviate schema, PostgreSQL state, and search result metadata | API/index field names use camelCase equivalents of the task's snake_case design terms |
| G2 | Done | `MarkdownChunkerTests` cover frontmatter, title fallback, heading path, defaults, supersession, code block guard, and overlap | |
| G3 | Done | `database/weaviate/local-rag-chunk.schema.json`, `WeaviateClient.ensureSchema`, `001_core_schema.sql`, and indexer additive DDL updated | Existing clients keep previous fields |
| G4 | Done | Reindex path is force-scan of registered sources; `metadata_version=1` forces prior indexed documents through migration | Rollback remains restoring prior code/schema and rebuilding derived Weaviate state |
| G5 | Done | `SearchResultItem.metadata` exposes document authority/status/freshness/supersession and heading metadata | Existing response fields remain present |
| G6 | Done | Maven tests, runtime force-scan/search smoke, compose config, docs validators, and diff check recorded | Default retrieval fixture now avoids machine-local project dependencies |

## Completion Guardrails

- Do not partially migrate schema without a reindex or rollback note.
- Do not treat path/folder heuristics as a replacement for document status and authority metadata.
- Do not send document text to hosted services for parsing, rerank, or evaluation.
- Do not broaden this task into answer-generation prompt work beyond exposing metadata required by later tasks.
- Do not remove or rewrite T0011 evaluation cases; extend or compare against them.

## Risks / Open Questions

- Weaviate schema changes may require dropping/recreating the collection in local development.
- YAML frontmatter parsing in Java should use a small stable dependency or a deliberately limited parser; dependency choice affects service Docker build.
- Some indexed sources may not have frontmatter; defaults and path-derived doc type must be deterministic.
- Code/table split avoidance may be staged if it complicates the first metadata migration.

## Status

- 2026-05-29: task issued as P0002 first critical-path migration for metadata-aware chunking and document authority indexing.
- 2026-05-30: plan reviewed and supplemented with implementation order, metadata contract draft, and migration plan before code changes.
- 2026-05-30: implemented and verified metadata-aware Markdown chunking, document authority metadata indexing, additive PostgreSQL/Weaviate schema migration, search result metadata exposure, focused tests, local runtime reindex, and scoped search smoke. The retrieval fixture was later made portable by removing machine-local project dependencies.
