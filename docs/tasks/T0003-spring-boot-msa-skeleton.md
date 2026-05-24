---
type: task
doc_id: T0003
title: spring-boot-msa-skeleton
status: done
owner:
created: 2026-05-24
updated: 2026-05-24
current_focus: "Completed Spring Boot multi-service skeleton, build, health endpoints, Compose runtime smoke"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - docs/guide/sdlc-automation.md
  - docs/design/msa-runtime-and-storage.md
  - docker-compose.yml
quality_axes:
  - WHOLE
  - CONTRACT
  - HANDOFF
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - spring-boot
  - msa
---

# T0003 spring-boot-msa-skeleton

- Type: task
- Document ID: T0003
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-24
- Current Focus: Completed Spring Boot multi-service skeleton, build, health endpoints, Compose runtime smoke
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/msa-runtime-and-storage.md`
  - `docs/design/source-registry-and-project-ssot.md`

## Purpose

이 task는 `local-rag-system`의 첫 실제 구현 slice로, repository 내부에 Spring Boot MSA 하위 프로젝트를 만들고 Docker Compose가 infra와 application services를 함께 build/run할 수 있는 최소 실행 baseline을 만든다.

## Task Placement Check

- 이 작업은 `P0001`의 MSA runtime 구현을 여는 선행 task다.
- source registry, scanner, retrieval, MCP 구현이 모두 service skeleton과 build/runtime convention에 의존하므로 별도 project가 아니라 umbrella 아래 task가 맞다.
- 이 task가 닫힌 뒤 `T0001` source registry implementation이 unblocked 된다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 "local RAG system 내부에 여러 MSA 하위 프로젝트가 있고, Docker Compose로 로컬에서 쉽게 실행할 수 있다"는 구조적 전제다.

깨면 안 되는 invariant:

- active implementation target은 Java/Spring Boot MSA다.
- application services는 `services/*` 아래에 위치한다.
- source folder mount는 read-only로 유지한다.
- public ports는 기본적으로 `127.0.0.1`에 bind한다.
- Ollama는 external local/network prerequisite으로 유지한다.
- skeleton은 후속 registry/indexing/retrieval 구현이 바로 붙을 package 구조를 제공해야 한다.

## Completion Mode Notes

Completion mode는 `functional`이다. 이 task가 닫히려면 skeleton 문서만 있는 상태가 아니라 build/test/compose smoke가 실제로 동작해야 한다.

## Committed Outcome

이 task가 `done`일 때 가능해야 하는 것:

- root build system이 five Spring Boot service subprojects를 빌드한다.
- 각 service가 최소 health endpoint와 focused test를 가진다.
- Dockerfiles가 실제 build artifact를 실행한다.
- `docker compose up --build`가 PostgreSQL, Weaviate, five application services를 시작한다.
- `api-gateway` health endpoint가 public local endpoint에서 응답한다.
- 다음 task가 사용할 Java package/module convention이 README 또는 build file에서 확인된다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Root Spring Boot multi-service build skeleton을 만든다. | root build command가 five service subprojects를 compile/test한다. |
| G2 | Five service applications를 만든다. | `api-gateway`, `source-registry-service`, `indexer-service`, `retrieval-service`, `mcp-bridge`가 각각 boot application과 health endpoint를 가진다. |
| G3 | Dockerfiles와 Compose가 실제 artifact를 실행하게 한다. | `docker compose up --build`가 jar missing 없이 application services를 시작한다. |
| G4 | Public health/status smoke path를 만든다. | `http://127.0.0.1:42120/api/health` 또는 documented equivalent가 응답한다. |
| G5 | 후속 구현 handoff를 정리한다. | service README, project WBS, active docs index가 다음 source registry task를 가리킨다. |

## Scope

- root Gradle multi-project 또는 Maven multi-module skeleton
- Java/Spring Boot application entrypoints
- minimal health endpoints
- focused unit/smoke tests
- Dockerfile artifact path alignment
- Compose runtime smoke
- service README update
- SDLC docs/index update

## Out Of Scope

- source registry validation logic
- scanner/watcher implementation
- Markdown parser/chunker implementation
- Ollama embedding implementation
- Weaviate upsert/search implementation
- MCP protocol implementation beyond placeholder health

## References

- `docs/guide/sdlc-automation.md`
- `docs/design/control-plane.md`
- `docs/design/msa-runtime-and-storage.md`
- `docs/projects/P0001-local-rag-system.md`
- `docker-compose.yml`
- `services/README.md`

## Dependencies

- `T0002` runtime/storage contract must remain aligned with this implementation.
- Java build tooling choice must be recorded in the build files and, if operator-facing commands change, `README.md`.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Choose and create root build skeleton | Done | 100% | Maven multi-module |
| W2 | Add five Spring Boot service entrypoints | Done | 100% | health endpoint per service |
| W3 | Add focused tests | Done | 100% | module tests under Maven |
| W4 | Align Dockerfiles with build artifacts | Done | 100% | Maven package artifact copied into runtime image |
| W5 | Run Compose smoke | Done | 100% | `docker compose up --build` plus gateway health |
| W6 | Update docs/index handoff | Done | 100% | T0004 issued for expanded functional baseline |

## Overall Progress

- 100%

## Completion Criteria

1. Root build command succeeds.
2. Five service applications compile and test.
3. Docker Compose config remains valid.
4. Docker Compose runtime starts all services.
5. Gateway health/status endpoint responds on localhost.
6. Documentation points the next SDLC step to source registry implementation.

## Completion Evidence

- build/test command output
- `docker compose --env-file .env.example config`
- `docker compose --env-file .env up -d --build`
- gateway health response
- updated docs validator output

## Outputs / Handoff

- Spring Boot service skeleton and build system.
- Runtime command path for subsequent implementation tasks.
- Package/module convention for `source-registry-service`.
- Unblocked `T0001-source-registry-project-ssot-registration`.

Downstream tasks:

- `T0001` implements source registry loader/validator and scope resolver.
- scanner/indexer task adds file state and job execution to `indexer-service`.
- retrieval task adds hybrid search to `retrieval-service`.
- MCP bridge task implements Codex-facing tools.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | 모든 후속 RAG 기능은 이 runtime skeleton 위에 붙는다. | five services build/run under Compose |
| CONTRACT | Dockerfiles, ports, env vars, package boundaries가 후속 task의 계약이다. | build files, Compose config, service README |
| HANDOFF | T0001과 후속 task가 바로 구현을 시작해야 한다. | package/module convention and docs handoff |
| EVIDENCE | skeleton은 실제 실행 가능해야 한다. | build/test and Compose health smoke |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | Maven multi-module build and tests passed | Root `pom.xml` builds common plus five services |
| G2 | Done | Five services start and expose `/api/health` | `api-gateway`, `source-registry-service`, `indexer-service`, `retrieval-service`, `mcp-bridge` |
| G3 | Done | Compose build/up succeeded | Dockerfiles use Maven package artifacts |
| G4 | Done | `GET http://127.0.0.1:42120/api/health` returned `UP` | Gateway aggregates downstream health |
| G5 | Done | `T0004-local-rag-functional-baseline.md` documents expanded handoff | Source registry and end-to-end RAG implementation proceeded there |

## Completion Guardrails

- Do not implement registry/indexing/search logic inside this task.
- Do not collapse all services into one application.
- Do not add Python runtime code as an implementation shortcut.
- Do not expose public ports beyond localhost defaults.
- Do not mark this task done without runtime smoke evidence.

## Risks / Open Questions

- Gradle multi-project vs Maven multi-module should be decided in W1.
- Spring Boot dependency versions should be selected conservatively and recorded in build files.
- Service-to-service client style can remain simple HTTP until domain logic requires stronger contracts.

## Status

- 2026-05-24: task 문서 생성. SDLC automation critical path의 first implementation gate로 발급.
- 2026-05-24: Maven multi-module Spring Boot skeleton, five service health endpoints, Dockerfile artifact alignment, Compose build/up, and gateway health smoke completed. End-to-end RAG implementation continued under `T0004`.
