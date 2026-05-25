---
type: task
doc_id: T0011
title: retrieval-quality-hardening
status: done
owner:
created: 2026-05-25
updated: 2026-05-25
current_focus: "Evaluation-backed deterministic retrieval hardening completed without changing the public RAG contract"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
  - services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java
  - services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java
quality_axes:
  - EVIDENCE
  - CONTRACT
  - SECURITY
  - HANDOFF
  - WHOLE
tags:
  - docs/task
  - local-rag-system
  - retrieval-quality
  - rerank
  - evaluation
---

# T0011 retrieval-quality-hardening

- Type: task
- Document ID: T0011
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-05-25
- Updated: 2026-05-25
- Current Focus: Evaluation-backed deterministic retrieval hardening completed without changing the public RAG contract
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 Local RAG의 검색 품질을 감이 아니라 평가 지표로 개선한다.

현재 시스템은 Codex 보조 기억장치로 실사용 가능하지만, `hit@1`이 충분히 안정적이지 않다. 특히 project-scoped search에서 support context가 primary project source보다 앞서거나, 한국어 자연어 query에서 keyword 품질이 낮아지는 문제가 있다.

이 task의 목적은 evaluation harness를 먼저 만들고, primary source weighting, deterministic rerank, duplicate/diversity control, chunk metadata 개선을 통해 top1 품질을 끌어올리는 것이다.

## Task Placement Check

- 이 작업은 새 project가 아니라 기존 `P0001 local-rag-system`의 retrieval improvement slice다.
- Functional baseline, source registry, Codex integration은 이미 존재한다.
- 별도 project를 발급하지 않는 이유는 검색 품질 개선이 기존 Local RAG runtime의 내부 품질 hardening이며, public API/MCP tool boundary를 유지하기 때문이다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 local-only RAG가 등록된 source truth를 더 정확한 순서로 Codex에게 제공하게 만드는 것이다.

깨면 안 되는 invariant:

- private source content는 hosted API로 전송하지 않는다.
- registered source boundary를 우회하지 않는다.
- public search modes는 `hybrid`, `keyword`, `vector`로 유지한다.
- `projectId`가 있으면 primary project source가 support context보다 우선되어야 한다.
- quality improvement는 evaluation result로 증명되어야 한다.
- Codex/MCP tool contract는 backward-compatible해야 한다.

## Completion Mode Notes

Completion mode는 `functional`이다. 이 task가 닫히려면 단순 문서화가 아니라, evaluation runner와 retrieval 품질 개선 코드가 실제로 동작하고 baseline 대비 개선 또는 명확한 no-regression evidence가 있어야 한다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- versioned retrieval evaluation cases가 repo에 존재한다.
- evaluation runner가 gateway search API를 호출해 `hit@1`, `hit@5`, MRR, source accuracy, latency를 산출한다.
- current baseline과 개선 후 결과가 비교 가능하게 기록된다.
- project-scoped search에서 primary source result가 support context보다 안정적으로 우선된다.
- duplicate/document diversity control이 top results를 더 넓은 evidence set으로 만든다.
- deterministic rerank 또는 source weighting이 retrieval-service에 적용되어 public API contract를 깨지 않는다.
- chunking 또는 chunk metadata 개선이 구현되면 reindex와 quality evaluation evidence가 남는다.
- docs validators, service tests, compose config, runtime smoke, quality evaluation이 통과한다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Retrieval evaluation harness를 만든다. | versioned query cases와 runner가 있고 `hit@1`, `hit@5`, MRR, source accuracy를 산출한다. |
| G2 | 현재 baseline을 재현 가능하게 고정한다. | English/identifier and Korean natural-language case sets가 현재 runtime에서 baseline metrics를 기록한다. |
| G3 | Primary source weighting을 구현한다. | `projectId` 검색에서 registered `primary_source_id`가 support context보다 우선되는 ranking evidence가 있다. |
| G4 | Deterministic rerank and diversity control을 구현한다. | top results가 source role/path/metadata를 반영하고 같은 문서 chunk 반복이 제한된다. |
| G5 | Chunk metadata improvement path를 구현하거나 안전하게 분리한다. | Markdown frontmatter/title/heading metadata가 ranking에 쓰이거나, 별도 follow-up으로 명확히 분리된다. |
| G6 | Observability를 품질 디버깅 가능 수준으로 확장한다. | audit 또는 evaluation output이 phase latency, source distribution, top result metadata를 제공한다. |
| G7 | Public contract and local-only security를 보존한다. | search/MCP contract smoke와 local-only config checks가 통과하고 hosted API 전송 경로가 없다. |

## Scope

- retrieval evaluation fixture and runner
- baseline metrics recording
- deterministic source weighting
- support context demotion
- document-level duplicate and diversity control
- optional deterministic rerank score composition
- optional chunk metadata extraction if low-risk
- search audit or evaluation observability expansion
- focused retrieval/indexer tests
- docs update for design/task/report evidence

## Out Of Scope

- hosted reranker or hosted LLM judge
- replacing Weaviate
- full PDF/OCR/canvas parsing
- UI implementation
- changing MCP tool names
- indexing unregistered source folders
- broad source registry redesign
- committing private runtime source content or machine-local paths

## References

- `docs/reports/2026-05-25-retrieval-quality-baseline.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/source-registry-and-project-ssot.md`
- `docs/design/msa-runtime-and-storage.md`
- `services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java`
- `services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java`
- `services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java`

## Dependencies

- Running local gateway or a test harness that can exercise `retrieval-service`
- Registered source metadata in PostgreSQL
- Existing Weaviate `LocalRagChunk` schema and chunk metadata
- Current local embedding endpoint for vector/hybrid cases
- `T0010` public search/MCP contract hardening

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Lock baseline report and design | Done | 100% | `2026-05-25-retrieval-quality-baseline` and `retrieval-quality-improvement-design` created |
| W2 | Add evaluation fixture | Done | 100% | `docs/evaluation/retrieval-quality-cases.yaml` contains English/identifier and Korean natural-language cases |
| W3 | Add evaluation runner | Done | 100% | `docs/bin/validate-retrieval-quality.sh` calls REST/MCP-compatible search endpoints and reports hit@1, hit@5, MRR, source accuracy, latency, and misses |
| W4 | Implement primary source weighting | Done | 100% | `RetrievalRanker` boosts registered primary source and demotes default support context without removing it from recall |
| W5 | Implement deterministic rerank and diversity | Done | 100% | source role, source priority, path class, task id/path token match, Korean aliases, and top-window document cap implemented |
| W6 | Add chunk metadata improvement | Split | 100% | schema/frontmatter/chunking changes deferred to a follow-up because they require reindex planning; current rerank uses existing path, heading, source, and contentHash metadata |
| W7 | Expand observability | Done | 100% | evaluation output records raw candidate count when available, final result count, top source/path, sources searched, and measured client latency; DB audit schema unchanged |
| W8 | Verify and close | Done | 100% | docs validators, Docker Maven tests, compose config, runtime smoke, and before/after evaluation comparison completed in development zone |

## Overall Progress

- 100%

## Completion Criteria

1. `./docs/bin/validate-codex-readiness.sh` passes.
2. `./docs/bin/validate-harness-foundation.sh` passes.
3. `./docs/bin/validate-doc-retrieval.sh` passes.
4. `./docs/bin/validate-closeout.sh --all` passes.
5. `docker compose --env-file .env.example config` passes.
6. Relevant Maven tests pass.
7. Evaluation runner records baseline and post-change metrics.
8. `hybrid` Korean natural-language `hit@1` improves or no regression is justified with evidence.
9. English/identifier `hit@5` does not regress.
10. Project-scoped search returns primary source above support context for targeted current-truth queries.
11. Runtime search smoke returns citation-bearing results for registered projects.
12. No hosted API path is introduced.

## Completion Evidence

Required evidence:

- evaluation fixture path and sample cases,
- evaluation runner command and output,
- before/after metric table,
- retrieval-service test results,
- indexer/chunker test results if chunking changes,
- runtime search smoke with citations,
- unknown project and search mode contract smoke if request DTO changes,
- docs validators,
- compose config,
- local-only endpoint/config check.

Evidence that is not sufficient alone:

- one manual query that "looks better",
- only top5 improvement without preserving source accuracy,
- code diff without evaluation metrics,
- reranker model selection without local runtime proof,
- hosted API benchmark.

## Outputs / Handoff

- retrieval quality evaluation cases and runner,
- ranking policy implementation notes,
- before/after retrieval quality report,
- updated design if decisions change,
- follow-up task if local cross-encoder reranker or chunk schema migration is too large for this task.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| EVIDENCE | Search quality must be measured, not guessed. | versioned cases and before/after metrics |
| CONTRACT | Codex/MCP clients depend on stable search behavior and fields. | API/MCP smoke and DTO tests |
| SECURITY | Rerank and evaluation must remain local-only. | config check and no hosted API dependency |
| HANDOFF | Future sessions need repeatable quality checks. | runner command and docs update |
| WHOLE | Retrieval must preserve source registry truth. | primary source weighting evidence |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `docs/evaluation/retrieval-quality-cases.yaml`; `docs/bin/validate-retrieval-quality.sh` | Fixture has 25 cases across `hybrid`, `keyword`, and `vector` for 75 case/mode runs. |
| G2 | Done | Baseline run against `http://127.0.0.1:42120/api/mcp/rag_search` | Baseline metrics captured before code change: overall hit@1 62.7%, hit@5 92.0%, MRR 0.760, source accuracy@1 90.7%. |
| G3 | Done | Post-change run against development retrieval-service on `http://127.0.0.1:43143/api/search`; `RetrievalRankerTests.promotesPrimarySourceOverSupportContext` | Korean hybrid hit@1 improved from 50.0% to 100.0%; source accuracy@1 improved from 70.0% to 100.0%. |
| G4 | Done | `RetrievalRanker`; `RetrievalRankerTests.limitsRepeatedChunksFromSameDocumentInTopWindow` | Deterministic rerank and top5 document diversity cap are covered by focused tests. |
| G5 | Done | This task status and design update | Chunk schema/frontmatter parsing is explicitly split because it requires schema/reindex planning; no unsafe partial migration was introduced. |
| G6 | Done | Evaluation runner output fields and rerank score map fields | Runner reports top source/path, sources searched, result counts, latency, and miss list; returned scores include `rawCandidateCount`, `finalResultCount`, and rerank components. |
| G7 | Done | Public mode contract unchanged; no hosted API dependency added; smoke covered REST/MCP surfaces | Search modes remain `hybrid`, `keyword`, `vector`; legacy `bm25` alias remains in `SearchRequest`. |

## Metric Comparison

Baseline was measured against the currently running gateway before code changes. Post-change was measured by starting the updated `retrieval-service` from the development zone on `SERVER_PORT=43143`, pointed at the same local PostgreSQL, Weaviate, and local Ollama embedding endpoint. The production Compose stack was not restarted during this task.

| Query Set / Mode | Baseline hit@1 | Post-change hit@1 | Baseline hit@5 | Post-change hit@5 | Baseline MRR | Post-change MRR | Source accuracy@1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| English / `hybrid` | 66.7% | 80.0% | 100.0% | 100.0% | 0.800 | 0.889 | 100.0% -> 100.0% |
| English / `keyword` | 86.7% | 86.7% | 93.3% | 100.0% | 0.900 | 0.922 | 100.0% -> 100.0% |
| English / `vector` | 66.7% | 93.3% | 100.0% | 100.0% | 0.822 | 0.956 | 100.0% -> 100.0% |
| Korean / `hybrid` | 50.0% | 100.0% | 100.0% | 100.0% | 0.750 | 1.000 | 70.0% -> 100.0% |
| Korean / `keyword` | 30.0% | 40.0% | 50.0% | 50.0% | 0.370 | 0.420 | 60.0% -> 80.0% |
| Korean / `vector` | 60.0% | 90.0% | 100.0% | 100.0% | 0.800 | 0.950 | 100.0% -> 100.0% |
| Overall | 62.7% | 82.7% | 92.0% | 93.3% | 0.760 | 0.869 | 90.7% -> 97.3% |

## Completion Evidence

Executed evidence:

- `./docs/bin/validate-retrieval-quality.sh` baseline against `http://127.0.0.1:42120/api/mcp/rag_search`: completed, 75 case/mode runs.
- `LOCAL_RAG_BASE_URL=http://127.0.0.1:43143 LOCAL_RAG_SEARCH_PATH=/api/search ./docs/bin/validate-retrieval-quality.sh`: completed, 75 case/mode runs.
- `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -q -pl services/retrieval-service -am test`: passed.
- `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -q -pl services/retrieval-service -am -DskipTests package`: passed; used to launch the temporary development retrieval-service.

Final closeout evidence:

- `./docs/bin/validate-codex-readiness.sh`: passed.
- `./docs/bin/validate-harness-foundation.sh`: passed.
- `./docs/bin/validate-doc-retrieval.sh`: passed.
- `./docs/bin/validate-closeout.sh --all`: passed.
- `docker compose --env-file .env.example config`: passed.
- `git diff --check`: passed.
- `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -q test`: passed.
- REST smoke for `worknote`, `personal-core`, `crypto-bot`, `local-rag-system`, and `qr-service-mvp`: passed against development retrieval-service.
- Unknown project error smoke: passed with HTTP 400.
- MCP `rag_search` and `rag_get_document` smoke: passed against current gateway.

## Completion Guardrails

- Do not tune ranking without first capturing a reproducible baseline.
- Do not remove default context merely to improve metrics; weight it correctly.
- Do not send source snippets or documents to hosted APIs.
- Do not break `rag_search`, `rag_answer`, or `rag_get_document` public contracts.
- Do not claim quality improvement from anecdotal manual queries.
- Do not commit machine-local source paths or private source contents into evaluation fixtures.
- If chunk schema changes require broad reindex, document the reindex and rollback path.

## Risks / Open Questions

- A local cross-encoder reranker may add runtime and memory pressure on the Mac mini.
- Existing Weaviate score shapes may not expose enough BM25/vector detail for transparent score tuples.
- Chunk metadata improvements may require schema migration and full reindex.
- Evaluation cases can become stale as docs change; the fixture needs maintenance rules.
- T0010 may still be active in repo docs even though operation deployment happened later; coordinate active task ordering before closeout.

## Status

- 2026-05-25: task 문서 생성. Baseline report and improvement design are prepared; runtime code remains unchanged.
- 2026-05-25: evaluation fixture and runner added, deterministic retrieval ranker implemented, service tests passed, baseline/post-change metrics show Korean `hybrid` hit@1 improved from 50.0% to 100.0% and overall hit@1 improved from 62.7% to 82.7%. Chunk schema/frontmatter parsing is split to a later reindex-aware task.
