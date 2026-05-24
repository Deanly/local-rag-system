---
type: task
doc_id: T0002
title: msa-runtime-baseline
status: done
parent_project: P0001
owner:
created: 2026-05-24
updated: 2026-05-24
current_focus: "MSA service boundaries, Docker Compose, PostgreSQL DDL, Weaviate schema"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - docs/design/msa-runtime-and-storage.md
  - docker-compose.yml
  - database/postgres/ddl/001_core_schema.sql
quality_axes:
  - WHOLE
  - CONTRACT
  - HANDOFF
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - runtime
---

# T0002 msa-runtime-baseline

- Type: task
- Document ID: T0002
- Status: done
- Completion Mode: functional
- Owner:
- Parent Project: P0001
- Created: 2026-05-24
- Updated: 2026-05-24
- Current Focus: MSA service boundaries, Docker Compose, PostgreSQL DDL, Weaviate schema
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md

## Purpose

Local RAG System의 target runtime을 MSA-style Docker Compose 구성으로 시작할 수 있게 만든다.

이 task는 실제 검색 품질 구현보다 먼저 service boundary, compose contract, storage DDL, schema, local-only 운영 경계를 고정한다.

## Task Placement Check

- 이 작업은 `local-rag-system` 전체 runtime의 baseline을 여는 작업이므로 `P0001` 아래 task가 맞다.
- 별도 project가 필요하지 않은 이유는 runtime/storage boundary가 source registry, indexer, retrieval, Codex bridge의 공통 실행 기반이기 때문이다.
- 후속 scanner, indexing, search, MCP 구현 task는 이 task의 service directory, compose, storage contract를 입력으로 읽는다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 "확장 가능한 local RAG system을 repo 내부의 여러 Spring Boot MSA 하위 프로젝트와 Docker Compose로 쉽게 실행할 수 있게 한다"는 경계다.

깨면 안 되는 invariant:

- application service는 `services/*` 아래 독립 하위 프로젝트로 위치한다.
- local source folder는 read-only로 mount한다.
- registry/state/job/failure/audit은 PostgreSQL control store에 저장한다.
- hybrid retrieval chunk index는 Weaviate에 저장하고 재생성 가능한 derived state로 취급한다.
- Ollama endpoint는 local/network prerequisite이며 private source content를 외부 API로 보내지 않는다.
- Compose public port는 기본적으로 `127.0.0.1`에 bind한다.

## Completion Mode Notes

Completion mode는 `functional`이다. 이 task가 닫히려면 runtime service code가 완성될 필요는 없지만, compose topology와 storage contracts가 검증 가능해야 한다.

현재 service Dockerfiles는 Spring Boot jar artifact convention을 고정하는 scaffold다. 실제 `docker compose up --build`는 후속 Spring Boot service 구현 task가 jar를 만들기 전까지 완료 조건이 아니다.

## Committed Outcome

이 task가 `done`일 때 가능해야 하는 것:

- `docker compose --env-file .env.example config`가 통과한다.
- runtime service 경계와 directory contract가 문서와 파일 구조에 함께 존재한다.
- PostgreSQL DDL이 source registry, document state, chunk state, index jobs, failure, search audit 테이블을 포함한다.
- Weaviate schema가 external vector, BM25, source metadata, citation metadata를 표현한다.
- 후속 Spring Boot 구현자가 어떤 service부터 만들고 어떤 storage contract를 따라야 하는지 README와 design에서 바로 확인할 수 있다.

## Goal Inventory

| Goal ID | Goal | Done When |
| --- | --- | --- |
| G1 | Docker Compose runtime contract를 만든다. | `docker-compose.yml`과 `.env.example`이 존재하고 `docker compose config`가 통과한다. |
| G2 | MSA 하위 프로젝트 경계를 만든다. | `services/*` 디렉터리와 각 README/Dockerfile이 존재한다. |
| G3 | PostgreSQL control schema를 고정한다. | `database/postgres/ddl/001_core_schema.sql`이 registry/state/job/failure/audit 테이블을 포함한다. |
| G4 | Weaviate retrieval schema를 고정한다. | `database/weaviate/local-rag-chunk.schema.json`이 chunk metadata와 vectorizer none 설정을 포함한다. |
| G5 | 후속 Spring Boot 구현 handoff를 가능하게 한다. | 각 service README가 책임, endpoints, out-of-scope를 명시한다. |

## Scope

- Compose topology
- Service directories
- Runtime environment variables
- PostgreSQL DDL
- Weaviate schema
- Source registry example
- Documentation index update

## Out Of Scope

- Spring Boot application source implementation
- Weaviate schema bootstrap code
- Actual embedding/indexing/search behavior
- MCP protocol implementation
- Reranker implementation

## Completion Evidence

- `docker compose --env-file .env.example config`
- document harness validators
- file list showing service directories and storage contracts

## Completion Criteria

1. `docker-compose.yml`, `.env.example`, `config/source-registry.example.yaml`이 local-only runtime contract를 표현한다.
2. `services/api-gateway`, `services/source-registry-service`, `services/indexer-service`, `services/retrieval-service`, `services/mcp-bridge`가 README와 Dockerfile을 가진다.
3. `database/postgres/ddl/001_core_schema.sql`이 control store schema를 포함한다.
4. `database/weaviate/local-rag-chunk.schema.json`이 hybrid retrieval chunk schema를 포함한다.
5. `docs/design/msa-runtime-and-storage.md`와 `docs/projects/P0001-local-rag-system.md`가 이 task와 runtime boundary를 노출한다.
6. Compose config와 document validators가 통과하거나 미실행 사유가 명시된다.

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `docker compose --env-file .env.example config` passed on 2026-05-24 | Runtime contract validates; service code remains downstream |
| G2 | Done | `services/*/README.md`, `services/*/Dockerfile` | Service directories and artifact convention exist |
| G3 | Done | `database/postgres/ddl/001_core_schema.sql`; repeated DDL smoke passed in Postgres 16 container on 2026-05-24 | Runtime seed/migrations remain downstream |
| G4 | Done | `database/weaviate/local-rag-chunk.schema.json` | Schema contract exists; bootstrap code remains downstream |
| G5 | Done | service READMEs and `docs/design/msa-runtime-and-storage.md` | Spring Boot skeleton issued as `T0003` |

## Outputs / Handoff

- Docker Compose service topology and local port contract.
- `.env.example` runtime variable names and operator prerequisites.
- service directory names and Spring Boot jar artifact convention.
- PostgreSQL control-store DDL.
- Weaviate chunk schema.
- source registry example config.
- runtime/storage design handoff for scanner, indexing, retrieval, and MCP tasks.

Downstream tasks:

- Spring Boot service scaffold consumes `services/*` directory and artifact conventions.
- source registry implementation consumes config path and PostgreSQL registry tables.
- scanner/indexer implementation consumes source mount, state tables, and job tables.
- retrieval implementation consumes Weaviate schema and search audit table.
- MCP bridge implementation consumes internal service URLs.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Runtime shape must match the expandable local RAG goal and the Spring Boot MSA target. | design/project/task docs reference the same MSA topology |
| CONTRACT | Later code depends on stable ports, env vars, paths, DDL, and schema. | compose config, DDL, schema, service README |
| HANDOFF | Next Codex session should implement services without rediscovering architecture. | implementation order and downstream task mapping |
| EVIDENCE | Scaffold must be mechanically valid even before service code exists. | `docker compose config`, DDL smoke, docs validators |

## Completion Guardrails

- Do not mark this task done by writing prose only; compose and storage files must exist.
- Do not bind public service ports to all interfaces by default.
- Do not mount source folders read-write.
- Do not add external hosted LLM APIs as defaults.
- Do not hide missing Spring Boot service implementation; this task is runtime contract, not full application implementation.

## Status

- 2026-05-24: Issued after final pre-implementation review found that the repository still lacked MSA project structure, Docker Compose runtime, and storage DDL despite having high-level design documents.
- 2026-05-24: Marked done after Compose config, DDL repeat-run smoke, service directory contract, and docs validators passed. Actual Spring Boot implementation continues in `T0003`.
