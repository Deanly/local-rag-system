---
type: task
doc_id: T0009
title: host-local-ollama-rag-configuration
status: done
owner:
created: 2026-05-25
updated: 2026-05-25
current_focus: "Completed portable host-local Ollama RAG runtime and global Codex registration"
completion_mode: operational-baseline
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - README.md
  - docker-compose.yml
  - config/source-registry.local.example.yaml
  - common/src/main/java/com/localrag/common/ollama/OllamaChatClient.java
quality_axes:
  - WHOLE
  - SECURITY
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - ollama
  - operations
---

# T0009 host-local-ollama-rag-configuration

- Type: task
- Document ID: T0009
- Status: done
- Completion Mode: operational-baseline
- Owner:
- Created: 2026-05-25
- Updated: 2026-05-25
- Current Focus: Completed portable host-local Ollama RAG runtime and global Codex registration
- Related Control Plane: docs/design/control-plane.md
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 checkout을 host-local Ollama 기반 RAG runtime으로 구성하고, 장비마다 다른 source registry를 read-only mount와 local-only answer path로 사용할 수 있는 운영 기준선을 만든다.

## Task Placement Check

- 이 작업은 새 product가 아니라 기존 `P0001` functional baseline의 host-local operation slice다.
- 기존 registry, indexer, retrieval, Compose contract를 유지하며 local config와 answer endpoint를 보강한다.
- 장비별 실제 source 이름, host path, default project 선택은 ignored local config 또는 설치된 global skill에 둔다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 private source content를 hosted API로 보내지 않고, 등록된 local folder만 local Ollama embedding/chat과 Weaviate hybrid retrieval로 사용할 수 있게 하는 것이다.

깨면 안 되는 invariant:

- `LOCAL_RAG_OLLAMA_BASE_URL`은 local/container-local endpoint만 가리킨다.
- `LOCAL_RAG_EMBEDDING_FALLBACK_ENABLED=false`를 유지한다.
- `config/source-registry.local.yaml`은 machine-local config이며 committed default로 취급하지 않는다.
- repo에 커밋되는 Compose/source 문서는 generic source slot만 제공한다.
- 실제 source 이름과 host path는 `.env`, local Compose override, installed skill 같은 machine-local surface에만 둔다.

## Committed Outcome

- `/api/search`는 host-local Ollama embedding model로 생성한 vector를 사용해 indexed local docs를 검색한다.
- `/api/answer`는 retrieved snippets만 local Ollama chat model에 전달해 citation-bearing answer를 생성한다.
- tracked Compose는 `/sources/source-01`부터 `/sources/source-05`까지 generic read-only slots를 제공한다.
- global Codex adapter는 `rag_search`와 `rag_answer`를 포함한 registry-driven tool descriptions를 제공한다.
- Maven tests, Compose config, runtime health, force scan, search, answer smoke가 통과한다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Host-local Ollama prerequisite is available | embedding and chat models are reachable through the configured local endpoint |
| G2 | Source registration stays machine-local | committed defaults contain generic source slots and ignored local config carries real paths |
| G3 | Retrieval and answer paths use local Ollama | `/api/search` and `/api/answer` return citation-bearing responses |
| G4 | Changes are verified and documented | Maven, Compose, docs validators, and smoke commands pass |

## Scope

- generic read-only source slots in Compose
- local Ollama model installation check
- retrieval-service `/api/answer` implementation
- gateway `/api/answer` delegation
- MCP bridge and Codex adapter `rag_answer` exposure
- README smoke command update
- task closeout evidence

## Out Of Scope

- production-quality reranking
- indexing arbitrary unregistered local folders
- committing machine-specific source names or host paths
- persistent macOS LaunchAgent repair if local service management is blocked
- source-safe full-document fetch tool

## References

- `docs/design/control-plane.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/source-registry-and-project-ssot.md`
- `docs/design/msa-runtime-and-storage.md`
- `README.md`
- `docker-compose.yml`

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Verify local Ollama | Done | 100% | embedding and chat endpoints reachable |
| W2 | Keep source registration portable | Done | 100% | committed source slots are generic; real source registry is ignored |
| W3 | Add answer endpoint | Done | 100% | retrieval and gateway routes implemented |
| W4 | Verify runtime | Done | 100% | Compose stack, force scan, search, and answer smoke passed |
| W5 | Close docs | Done | 100% | completion evidence recorded without source-specific committed defaults |

## Overall Progress

- 100%

## Completion Criteria

1. `mvn test` passes.
2. `docker compose --env-file .env.example config` passes.
3. `docker compose --env-file .env config` passes for the current machine-local registry.
4. `POST /api/index/force` indexes registered sources without errors.
5. `POST /api/search` returns citations from registered sources.
6. `POST /api/answer` returns a local Ollama-generated answer with citations.
7. Harness validators pass or any skip is explicitly explained.

## Completion Evidence

- `mvn -q test` passed after dependency warmup.
- `docker compose --env-file .env.example config` passed with generic `/sources/source-*` read-only slots.
- `docker compose --env-file .env config` passed for the ignored current-device registry.
- Docker images pulled successfully: `postgres:16-alpine`, `cr.weaviate.io/semitechnologies/weaviate:1.25.9`, `maven:3.9.9-eclipse-temurin-17`, and the `eclipse-temurin:17-jre` base image layers used by local service builds.
- Docker registry access was not the blocker: Docker daemon was healthy, no proxy override was set, and Docker Hub plus Weaviate registry endpoints answered. The runtime blocker was missing images plus a slow previous pull; explicit pulls completed successfully.
- `docker compose --env-file .env up -d --build` built all five Spring Boot service images and started PostgreSQL, Weaviate, registry, indexer, retrieval, MCP bridge, and API gateway.
- `GET /api/health` returned all services `UP`; source registry validation returned `valid: true`.
- `POST /api/index/force` completed for the current registered sources with no errors.
- `POST /api/search` and `POST /api/mcp/rag_search` returned citation-bearing results.
- `POST /api/answer` and `POST /api/mcp/rag_answer` returned local answers with citations.
- The chat request sends `think:false`, and `OllamaChatClientTests` verifies the request contract.
- Local Ollama embedding and chat endpoints were verified directly.
- Docs validators and `git diff --check` passed after closeout updates.

## Outputs / Handoff

- API: `POST /api/answer`
- MCP tools: `rag_search`, `rag_answer`, `rag_list_projects`, `rag_list_sources`, `rag_index_status`, `rag_force_scan`
- Tracked source mount contract: `/sources/source-01` through `/sources/source-05`
- Local env: `.env` remains ignored
- Local registry: `config/source-registry.local.yaml` remains ignored
- Installed global skill may be customized per device with source-specific hints; repo skill template remains registry-driven.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Search and answer must use the same local registry/index boundary | registry, force scan, search, answer smoke |
| SECURITY | Private source snippets must not leave local Ollama | local endpoint config and fallback disabled |
| EVIDENCE | RAG configuration must be proven by runtime calls | Maven, Compose, curl smoke |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | local Ollama API checks | model choice is controlled by local env |
| G2 | Done | `.env.example`, `docker-compose.yml`, `.gitignore` | real paths stay in ignored local config |
| G3 | Done | `/api/search`, `/api/mcp/rag_search`, `/api/answer` smoke | search and answer return citations from indexed local sources |
| G4 | Done | `mvn -q test`, Compose config, full runtime smoke, local Ollama API smoke | image pull blocker resolved |

## Completion Guardrails

- Do not commit machine-local `.env` or `config/source-registry.local.yaml`.
- Do not enable fallback embeddings for completion evidence.
- Do not index folders outside the explicit local registry.
- Do not send source content to hosted LLM APIs.

## Status

- 2026-05-25: Issued for host-local Ollama RAG configuration on the current checkout.
- 2026-05-25: Added `/api/answer`, generic source slots, and registry-driven Codex tool descriptions. Maven tests, Compose config, runtime health, force scan, search, MCP search, and local answer generation passed.
- 2026-05-25: Kept source-specific names, host paths, and preferred default project selection out of committed repo defaults; those values remain machine-local.
