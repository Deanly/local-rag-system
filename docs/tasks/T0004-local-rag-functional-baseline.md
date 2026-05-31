---
type: task
doc_id: T0004
title: local-rag-functional-baseline
status: done
owner:
created: 2026-05-24
updated: 2026-05-24
current_focus: "Completed end-to-end functional baseline: registry, indexer, hybrid retrieval, MCP REST bridge"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - docker-compose.yml
  - config/source-registry.example.yaml
  - database/postgres/ddl/001_core_schema.sql
  - docs/reports/2026-05-24-local-rag-functional-smoke.md
quality_axes:
  - WHOLE
  - CONTRACT
  - EVIDENCE
  - SECURITY
tags:
  - docs/task
  - local-rag-system
  - functional-baseline
---

# T0004 local-rag-functional-baseline

- Type: task
- Document ID: T0004
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-24
- Current Focus: Completed end-to-end functional baseline: registry, indexer, hybrid retrieval, MCP REST bridge
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/msa-runtime-and-storage.md`
  - `docs/design/source-registry-and-project-ssot.md`

## Purpose

이 task는 SDLC core 자동화가 고장난 상황에서, 사용자의 요청에 따라 `local-rag-system`의 functional MVP를 직접 완성한 implementation slice다.

목표는 설계 문서만 유지하는 것이 아니라 Docker Compose로 실행 가능한 registry, indexing, retrieval, Codex-facing bridge baseline을 실제 코드와 smoke evidence로 닫는 것이다.

## Task Placement Check

- 이 작업은 `P0001`의 원래 functional exit criteria를 닫는 implementation slice이므로 별도 project가 아니라 umbrella 아래 task가 맞다.
- `T0001`과 `T0003`의 경계를 넘어 watcher/indexing/retrieval까지 구현했지만, 새 human-facing initiative가 아니라 같은 local RAG baseline의 남은 critical path다.
- retrieval quality tuning, rerank, UI, true MCP transport는 별도 후속 task 후보로 남긴다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 "등록된 local source folder만 read-only로 감시하고, 변경된 Markdown 파일을 local embedding 기반 chunk index로 반영하며, 같은 API surface로 검색과 Codex bridge를 제공한다"는 end-to-end RAG 경계다.

깨면 안 되는 invariant:

- source registry에 등록되지 않은 folder는 indexing/search 대상이 아니다.
- source folders는 container에 read-only로 mount한다.
- PostgreSQL은 registry/document/chunk/audit state의 control store다.
- Weaviate는 derived search index이며 재생성 가능하다.
- Ollama embedding endpoint는 local/network local prerequisite이고, fallback은 smoke용이다.
- gateway public bind는 `127.0.0.1` 기본값을 유지한다.

## Committed Outcome

이 task가 `done`일 때 가능해야 하는 것:

- registry YAML load/validate/reload가 동작한다.
- project id로 source scope를 resolve한다.
- file watcher와 periodic scanner가 source changes를 감지한다.
- Markdown/plain text 파일을 chunking하고 embedding vector를 생성한다.
- Weaviate에 chunk object를 create/replace/delete한다.
- hybrid, vector, keyword mode search가 citation-bearing response를 반환한다.
- gateway와 MCP REST bridge가 같은 검색 경로를 제공한다.
- Maven test와 Compose smoke evidence가 남는다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Java/Spring Boot MSA build/runtime을 완성한다. | Maven test와 Compose up/build가 통과한다. |
| G2 | Source registry runtime contract를 구현한다. | registry validate/projects/sources/scope API가 응답한다. |
| G3 | Watcher/scanner/indexer pipeline을 구현한다. | 생성/수정/삭제가 document/chunk state와 Weaviate에 반영된다. |
| G4 | Hybrid retrieval API를 구현한다. | `/api/search`가 project-scoped citation-bearing result를 반환한다. |
| G5 | Codex-facing bridge smoke path를 구현한다. | `/api/mcp/rag_search`, list/status/force scan endpoints가 gateway를 통해 호출된다. |
| G6 | 운영상 남은 prerequisite을 명확히 한다. | Ollama embedding model 상태와 fallback 사용 여부가 report에 기록된다. |

## Scope

- Spring Boot services implementation
- source registry loader/validator/synchronizer
- PostgreSQL state DDL consumption
- file watch plus periodic scan fallback
- Markdown chunking
- Ollama embedding client with smoke fallback
- Weaviate schema and object upsert/delete
- project-scoped keyword/vector/hybrid search
- gateway and MCP REST bridge endpoints
- sample-folder smoke evidence

## Out Of Scope

- true MCP protocol server transport
- production observability stack
- PDF/OCR/canvas parsing
- multi-user auth/tenant model
- automatic model pull on the user's Ollama host
- UI implementation

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Multi-module Maven/Spring Boot services | Done | 100% | five service modules plus common module |
| W2 | Registry model/API/sync | Done | 100% | YAML registry validates and syncs to PostgreSQL |
| W3 | Indexer watcher/scanner/chunker | Done | 100% | WatchService plus periodic scan fallback |
| W4 | Weaviate indexing | Done | 100% | create/replace/delete chunk objects |
| W5 | Retrieval API | Done | 100% | keyword/vector/hybrid modes with citations |
| W6 | Gateway and MCP REST bridge | Done | 100% | public localhost gateway path |
| W7 | Smoke verification and report | Done | 100% | functional smoke report added |

## Overall Progress

- 100%

## Completion Criteria

1. Root Maven test succeeds.
2. Docker Compose starts PostgreSQL, Weaviate, and five Spring Boot services.
3. Gateway health returns `UP`.
4. Registry validation returns valid project/source counts.
5. Force scan indexes sample Markdown.
6. Watcher reflects modification and creation without force scan.
7. Deletion removes stale chunks from search.
8. Hybrid search returns citation-bearing results.
9. MCP REST bridge search returns citation-bearing results.
10. Ollama model prerequisite is documented.

## Completion Evidence

- `docs/reports/2026-05-24-local-rag-functional-smoke.md`
- `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -q test`
- `docker compose --env-file .env.example up -d --build`
- `GET http://127.0.0.1:42120/api/health`
- `POST http://127.0.0.1:42120/api/registry/validate`
- `POST http://127.0.0.1:42120/api/index/force?projectId=personal-notes`
- `POST http://127.0.0.1:42120/api/search`
- `POST http://127.0.0.1:42120/api/mcp/rag_search`

## Outputs / Handoff

- Runnable Docker Compose local RAG baseline.
- Maven multi-module Spring Boot codebase.
- Registry YAML example and PostgreSQL DDL.
- Gateway API for health, registry, index status, force scan, and search.
- MCP-style REST bridge for Codex-side integration work.
- Functional smoke report with model prerequisite note.

Downstream tasks:

- production embedding model setup on reachable Ollama host.
- true MCP protocol adapter if Codex should call tools without REST wrapping.
- retrieval quality baseline with evaluation set, rerank, graph expansion, and larger corpus testing.
- parser expansion for PDF/OCR/canvas files.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Registry, watcher, indexer, retrieval, and bridge must work as one local system. | Compose smoke and gateway health |
| CONTRACT | APIs, env vars, registry fields, and storage tables are future maintenance contracts. | README, compose, DDL, service code |
| EVIDENCE | RAG freshness/search claims must be runtime-observed. | create/modify/delete/search smoke report |
| SECURITY | Private local folders must remain read-only and local-bound. | read-only source mount, localhost port binding, registry scope |

## Completion Guardrails

- Do not claim production-quality embeddings until a real Ollama embedding model is installed and fallback is disabled.
- Do not expand indexing beyond registered source roots.
- Do not treat the REST bridge as a true MCP protocol server.
- Do not mark retrieval quality tuning complete from a one-document smoke corpus.
- Do not add write automation to source folders as part of retrieval.

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | Maven test passed; Compose build/up passed | Java 17, Maven multi-module, Dockerfiles aligned |
| G2 | Done | Registry validation returned `valid: true`, `projects: 1`, `sources: 1` | `primary_source_id` contract is active |
| G3 | Done | Modification/create/delete smoke passed | Watcher is best-effort; periodic scan remains fallback |
| G4 | Done | Hybrid search returned `knowledge.md#Local RAG Notes` | Results include citation, snippet, score, source metadata |
| G5 | Done | Gateway `/api/mcp/rag_search` returned same result shape | REST bridge, not true MCP transport |
| G6 | Done | Smoke report records the initial missing embedding-model state on an operator-local Ollama endpoint; later deployment baseline pins `qwen3-embedding:4b` | Production run should keep fallback disabled |

## Status

- 2026-05-24: task issued and completed in one orchestrated implementation pass because SDLC core automation was unavailable. Functional baseline code, tests, Compose runtime, smoke evidence, and prerequisite notes were produced.
