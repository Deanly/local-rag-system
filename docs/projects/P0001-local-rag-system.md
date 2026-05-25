---
type: project
doc_id: P0001
title: local-rag-system
status: done
project_role: umbrella
umbrella_initiative: local-rag-system
parent_umbrella_project: self
completion_mode: functional
owner:
created: 2026-05-24
updated: 2026-05-25
current_focus: "Functional baseline completed with portable host-local Ollama answer runtime"
related_control_plane: docs/design/control-plane.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - source:planning/local-rag-system-project-note
  - source:planning/local-rag-system-design-note
  - source:planning/source-registry-ssot-strategy-note
quality_axes:
  - WHOLE
  - SCOPE
  - HANDOFF
  - EVIDENCE
  - SECURITY
tags:
  - docs/project
  - local-rag-system
---

# P0001 local-rag-system

- Type: project
- Document ID: P0001
- Status: done
- Project Role: umbrella
- Umbrella Initiative: local-rag-system
- Parent Umbrella Project: self
- Completion Mode: functional
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-25
- Current Focus: Functional baseline completed with portable host-local Ollama answer runtime
- Related Control Plane: docs/design/control-plane.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 project는 장비별 local source roots를 대상으로 하는 1인용 Local RAG System의 human-facing umbrella owner다.

완료 상태는 단순 설계 문서 작성이 아니라, local 장비의 프로젝트 SSOT 위치를 `project_id`와 함께 등록하고, 등록된 source root를 read-only로 감시/스캔하며, local embedding/chat과 hybrid retrieval을 통해 Codex/CLI/UI가 같은 검색 API를 사용할 수 있는 기능적 baseline이 동작하는 것이다.

## Automation Goal

SDLC 자동화가 끝까지 밀어야 하는 목표는 다음이다.

`local-rag-system`은 등록된 macOS local source folders를 read-only로 감시/스캔하고, 변경된 Markdown 파일만 local Ollama embedding으로 chunk indexing하여 Weaviate BM25/vector hybrid search로 검색하며, Codex/CLI/future UI가 같은 Spring Boot MSA API surface를 통해 citation 포함 검색 결과와 index status를 사용할 수 있게 하는 1인용 local-only RAG system을 Docker Compose로 재현 가능하게 제공한다.

자동화는 이 목표를 줄여서 닫으면 안 된다. 설계/스캐폴드/부분 구현은 중간 gate일 뿐이며, project closeout은 생성, 수정, 삭제, 검색, citation, index status, Codex bridge smoke evidence가 모두 있을 때만 가능하다.

## Umbrella Lineage

- 이 문서는 `local-rag-system` initiative의 기본 umbrella project다.
- 현재 별도 exception branch project는 없다.
- 후속 구현은 먼저 이 project 아래의 `task`로 발급한다.

## Project Issuance Check

- 발급 근거: 사용자가 기존 기획 문서를 읽고, `../document-harness`를 적용한 뒤 개발 방향 설계안을 작성하라고 요청했다.
- 이 문서는 project 전체의 lineage와 completion boundary를 잠그기 위한 umbrella surface다.
- 후속 세부 구현은 별도 project가 아니라 task로 분해한다.

## Whole-System Anchor

이 project가 보존해야 하는 전체 목표는 private local source folders를 외부 API에 노출하지 않고 local-only retrieval system으로 검색 가능하게 만드는 것이다.

깨면 안 되는 design invariant:

- source registry에 등록되지 않은 folder는 indexing/search 대상이 아니다.
- repo `docs/`는 해당 repo의 current truth이고, 별도 등록된 compiled knowledge source는 cross-project reference다.
- planning/meta source는 repo SSOT가 아니라 planning/meta layer다.
- source folder는 read-only다.
- watcher는 best-effort signal이고 scanner가 freshness authority다.
- index는 언제든 재생성 가능한 derived state다.
- search API는 BM25/vector hybrid와 citation을 제공해야 한다.
- embedding/chat은 local Ollama endpoint만 사용한다.
- implementation task는 `docs/design/local-rag-system-development-direction.md`의 boundary를 읽고 잘라야 한다.

## Completion Mode Notes

Completion mode는 `functional`이다. 이 project가 닫히려면 실제 로컬 RAG 기능 baseline이 동작해야 한다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- Spring Boot 기반 MSA services가 Docker Compose로 실행된다.
- 장비별 source registry가 project id, SSOT source, default context를 검증하고 search scope를 결정한다.
- 지정한 source folder를 read-only로 mount하고 scanner/watcher로 변경을 감지한다.
- Markdown 파일을 parsing/chunking하고 변경분만 Weaviate에 upsert/delete한다.
- Ollama embedding을 사용해 Weaviate hybrid search를 수행한다.
- `/api/search`, `/api/index/status`, force scan API가 동작한다.
- Codex가 사용할 MCP tool 또는 REST bridge smoke path가 있다.
- 샘플 source folder 기준 생성/수정/삭제/search smoke evidence가 남는다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | 하네스 기반 문서 control surface를 적용하고 프로젝트 방향을 current truth로 고정한다. | `AGENTS.md`, `docs/` harness, control-plane, ubiquitous-language, project, design index가 검증을 통과한다. |
| G2 | 장비별 source registry와 project SSOT registration contract를 구현 기준으로 고정한다. | `SourceRegistry`, `ProjectRegistration`, `project_id`, `source_id`, `ssot_role`, Codex/RAG skill scope가 design/task에 잠긴다. |
| G3 | Spring Boot MSA services와 Docker Compose 운영 baseline을 만든다. | compose로 gateway, registry, indexer, retrieval, MCP bridge, PostgreSQL, Weaviate가 구성되고 health/status가 응답한다. |
| G4 | source folder scanner/watcher와 state diff를 구현한다. | 샘플 폴더 생성/수정/삭제가 `DocumentState`와 index job에 반영된다. |
| G5 | Markdown chunking, Ollama embedding, Weaviate upsert/delete를 구현한다. | 샘플 Markdown 문서가 chunk로 저장되고 변경/삭제가 검색 결과에 반영된다. |
| G6 | hybrid search API와 Codex 연동 경로를 구현한다. | BM25/vector hybrid 결과가 citation과 score breakdown을 포함하고 Codex smoke tool이 호출된다. |

## Scope

- document-harness 기반 문서 체계 적용
- 개발 방향 design 기준 고정
- 장비별 source registry와 project SSOT registration 설계 및 구현
- Spring Boot MSA services 구현
- Docker Compose local runtime
- Markdown 우선 ingestion
- Weaviate hybrid retrieval
- Ollama local embedding/chat 연동
- Codex MCP 또는 REST bridge
- sample folder integration/smoke test

## Out Of Scope

- multi-user SaaS 권한 모델
- 특정 노트 앱 전용 플러그인
- 특정 cloud provider 전용 동기화 구현
- PDF/OCR/canvas/Excalidraw deep parsing
- production-grade observability stack
- 검색 엔진 다중 구현 동시 지원

## References

- `docs/design/control-plane.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/source-registry-and-project-ssot.md`
- `source:planning/local-rag-system-project-note`
- `source:planning/local-rag-system-design-note`
- `source:planning/source-registry-ssot-strategy-note`
- `README.md`
- `docs/architecture.md`

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| T0002 | MSA runtime baseline | Done | 100% | service directories, compose, PostgreSQL DDL, Weaviate schema contract validated |
| T0003 | Spring Boot MSA skeleton | Done | 100% | Maven multi-module, five service health endpoints, Compose smoke |
| T0001 | Source registry and project SSOT registration | Done | 100% | registry validation, project scope resolution, metadata propagation |
| T0004 | Local RAG functional baseline | Done | 100% | watcher/scanner, Markdown indexing, Weaviate hybrid search, MCP REST bridge smoke |
| T0005 | Multi-source application hardening | Done | 100% | Personal Notes wiki, Project Alpha docs, Project Beta docs source contract; glob enforcement; default context resolution |
| T0006 | Local device application baseline | Done | 100% | qwen3-embedding:4b production baseline and actual local source indexing completed |
| T0007 | Codex global RAG integration | Done | 100% | installable stdio MCP adapter and global Codex skill |
| T0008 | Portable ops zone deployment | Done | 100% | portable localhost defaults and `~/Service` operation zone |
| T0009 | Host-local Ollama RAG configuration | Done | 100% | portable source slots, answer endpoint, global integration refresh, indexing/search/answer smoke |
| T-candidate-08 | Retrieval quality baseline | Deferred | 0% | evaluation set, rerank, graph expansion remain next optimization work |

## Planned Task Candidates

- `design-baseline-closeout`: 하네스 적용과 설계 기준 검증 완료
- `spring-boot-msa-skeleton`: Spring Boot MSA skeleton, build, health endpoints, Compose smoke
- `source-registry-project-ssot-registration`: 장비별 source registry와 project SSOT registration 구현
- `source-state-indexer`: scanner/watcher/state diff
- `markdown-weaviate-indexing`: Markdown chunking, embedding, upsert/delete
- `hybrid-search-api`: search API, citation, filters, score breakdown
- `codex-mcp-bridge`: Codex 연동
- `retrieval-quality-baseline`: evaluation set, rerank, graph expansion 기준

## Overall Progress

- 100%

## Milestones

- M1: 하네스 적용, 설계 방향, source registry 기준 고정
- M2: Spring Boot MSA skeleton과 Compose runtime smoke
- M3: source registry loader/validator와 project-scoped search scope 구현
- M4: scanner/watcher/state diff 구현
- M5: 샘플 Markdown 인덱싱과 삭제 반영
- M6: hybrid search API와 citation 반환
- M7: Codex smoke integration

## Exit Criteria

1. project 목적에 적은 로컬 RAG functional baseline이 Docker Compose로 실행된다.
2. 필수 task가 모두 `done`이거나, 범위 재발급 근거와 함께 `superseded` 또는 `cancelled`로 정리되어 있다.
3. 샘플 source folder 기준 생성, 수정, 삭제, 검색, citation, index status evidence가 남아 있다.
4. `project_id` 기반 source selection과 compiled knowledge source fallback이 registry smoke로 검증되어 있다.
5. private content가 외부 API로 나가지 않는 local-only 운영 경계가 검증되어 있다.
6. 남은 범위는 후속 task 또는 exception branch project로 명시되어 있으며, 현재 project의 원래 목적을 축소한 `done` 처리로 위장하지 않는다.

## Completion Evidence

- 하네스 validator 결과
- source registry validation 결과
- `docker compose up` 또는 동등한 local runtime smoke 결과
- scanner/watcher integration test 결과
- Weaviate upsert/delete/search smoke 결과
- `/api/index/status`와 `/api/search` 응답 예시
- Codex MCP 또는 REST bridge 호출 증빙
- Actual local device application: 612 documents and 3463 chunks indexed from `personal-notes`, `project-alpha.docs`, and `project-beta.docs` with `qwen3-embedding:4b` and fallback disabled.
- Codex global integration: `integrations/codex/install-codex-local-rag.sh` installs `mcp_servers.local_rag` and the `local-rag` skill.
- Operation zone: `~/Service/bin/local-rag` manages the stack from `~/Service/code/local-rag-system` using untracked config under `~/Service/config/local-rag-system`.

문서만 작성된 상태는 G1의 증빙일 수 있지만 project 전체 `done` evidence로는 부족하다.

## Outputs / Handoff

- 현재 산출물: `docs/design/local-rag-system-development-direction.md`
- 현재 산출물: `docs/design/source-registry-and-project-ssot.md`
- 현재 산출물: `docs/design/msa-runtime-and-storage.md`
- 현재 산출물: `docs/guide/sdlc-automation.md`
- 현재 산출물: `docs/projects/P0001-local-rag-system.md`
- 후속 handoff: `T0003`이 Spring Boot MSA skeleton, build, health endpoints, Compose smoke를 시작한다.
- 후속 handoff: `T0003` 완료 후 `T0001`이 source registry loader/validator와 project-scoped search 구현을 시작한다.
- 후속 handoff: retrieval quality task는 MVP search API가 나온 뒤 evaluation set과 rerank/graph expansion을 다룬다.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Java/Spring services, Weaviate/Ollama/Codex가 하나의 로컬 RAG 목표로 정렬되어야 한다. | control-plane, project, design 문서의 같은 target architecture |
| SCOPE | MVP가 attachment/OCR/multi-user까지 확장되어 흐려지면 기능 baseline이 늦어진다. | Scope/Out Of Scope와 task 후보 분리 |
| HANDOFF | 후속 task가 바로 구현할 수 있어야 한다. | WBS, planned task candidates, API/state/interface 계약 |
| EVIDENCE | RAG는 freshness와 검색 품질을 실제 smoke로 확인해야 한다. | index/search/status/Codex smoke 결과 |
| SECURITY | private source와 프로젝트별 SSOT boundary가 섞이지 않아야 한다. | registry read/write policy, sensitivity/default context 검증 |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `./docs/bin/validate-codex-readiness.sh`, `./docs/bin/validate-harness-foundation.sh`, `./docs/bin/validate-doc-retrieval.sh`, `./docs/bin/validate-closeout.sh --all` passed on 2026-05-24 | Control surface and indexes are valid |
| G2 | Done | `T0001` done; registry smoke returned `valid: true`, `projects: 1`, `sources: 1` | Runtime registry API and sync implemented |
| G3 | Done | `T0002` and `T0003` done; gateway health returned all services `UP` | Compose starts infra plus five Spring Boot services |
| G4 | Done | `T0004` smoke: modification/create/delete reflected in index status and search | Watcher is best-effort; periodic scanner remains fallback |
| G5 | Done | `T0004` smoke: sample Markdown produced chunk in Weaviate; deleted temp source removed stale chunk | Later deployment baseline pins `qwen3-embedding:4b` with fallback disabled |
| G6 | Done | `/api/search` and `/api/mcp/rag_search` returned citation-bearing hybrid results | Bridge is REST-shaped, not true MCP transport |

## Completion Guardrails

- 기존 Purpose를 더 작은 하위 조각으로 축소해 `done` 처리하지 않는다.
- 남은 핵심 목표를 후속 task나 project로 넘겼다면 이 project는 `done`이 아니라 `active`, `blocked`, `superseded`, `cancelled` 중 하나여야 한다.
- `done`으로 닫기 전 `Goal Inventory`와 `Goal Verification`을 맞추고 `./docs/bin/validate-closeout.sh`를 통과해야 한다.
- `Related Control Plane`, `Whole-System Anchor`, `Outputs / Handoff`, `Quality Axes In Scope` 없이 부분 delivery를 전체와 분리된 local project처럼 닫지 않는다.
- 새 `project`를 쉽게 남발하지 않으며, 기본값은 이 umbrella project 아래의 새 `task`다.
- `Completion Mode`는 terminal condition이어야 하며 phase 이름으로 대체하지 않는다.

## Status

- 2026-05-24: 원문 요청에 따라 `document-harness`를 적용하고 umbrella project 문서를 active 상태로 발급. 개발 방향 design 문서와 연결.
- 2026-05-24: source registry와 project SSOT registration 결정을 umbrella goal, WBS, milestone, handoff에 반영.
- 2026-05-24: SDLC automation goal, critical path, T0003 first implementation gate를 반영. T0002는 done, T0001은 T0003 전까지 blocked로 정렬.
- 2026-05-24: SDLC core automation unavailable, so implementation was completed directly in this Codex session. T0001, T0003, and T0004 are done. Functional baseline smoke passed for registry, watcher/scanner, indexing, deletion cleanup, hybrid search, and MCP REST bridge. Initial smoke used fallback embeddings before a production embedding model was available.
- 2026-05-24: T0006 completed actual local device application with `qwen3-embedding:4b`. The stack indexed 612 documents and 3463 chunks from the registered local source folders, verified source-scoped hybrid search for all three source classes, and confirmed stable Ollama memory behavior on `local-llm-host`.
- 2026-05-24: T0005 completed the pre-application hardening for the local target sources: multi-source Compose mounts, local registry example, scanner glob enforcement, default context source resolution, and inactive source retirement.
- 2026-05-24: T0007 added and installed the Codex global integration: project-owned installer, stdio MCP adapter, and global `local-rag` skill.
- 2026-05-24: T0008 separated portable defaults from this device's direct-network config and moved operation to `~/Service`.
- 2026-05-25: T0009 added host-local Ollama configuration, implemented `/api/answer`, kept source selection in ignored local registry state, and refreshed the Codex integration with registry-driven MCP descriptions.
- 2026-05-25: T0009 blocker resolved. The missing images were pulled, the Compose stack started, the current machine-local registry was indexed, and search/MCP search/local answer smoke passed after disabling Ollama thinking output for bounded answer generation.
