---
type: design
title: control-plane
status: current
domain: control-plane
owner:
created: 2026-05-24
updated: 2026-05-29
retrieval_class:
  - core-start
context:
  default_load: true
  section_load: false
  evidence_only: false
  size_tier: small
referenced_by:
  - docs/README.md
  - docs/projects/P0001-local-rag-system.md
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - source:planning/local-rag-system-project-note
  - source:planning/local-rag-system-design-note
  - source:planning/source-registry-ssot-strategy-note
tags:
  - docs/design
  - control-plane
  - local-rag-system
---

# control-plane

- Type: design
- Domain: control-plane
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-29
- Referenced By:
  - `docs/README.md`
  - `docs/projects/P0001-local-rag-system.md`
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`

## Purpose

이 문서는 `local-rag-system`의 전체 목표, active control surface, execution handoff, validator를 한 곳에 모아 두는 central control surface다.

새 project/task/design은 이 문서를 whole-system anchor로 참조해야 한다. 현재 initiative의 기본 owner는 `docs/projects/P0001-local-rag-system.md`다.

## Whole-System Outcome

`local-rag-system`은 장비별로 등록된 local source roots를 대상으로 하는 1인용 local-only RAG system이다. 모든 입력은 macOS에서 접근 가능한 일반 파일 시스템 폴더로 취급한다.

끝까지 보존해야 하는 outcome:

- source folder를 read-only로 직접 감시하고 스캔한다.
- local 장비에서 실제로 다루는 프로젝트만 `project_id`와 SSOT source root로 등록한다.
- repo `docs/`는 해당 repo의 current truth로, 별도 등록된 compiled knowledge source는 cross-project reference로 구분한다.
- 변경된 파일만 재인덱싱하고 삭제/이름 변경을 index에 반영한다.
- local 또는 LAN-local Ollama embedding/chat만 사용한다.
- BM25/keyword와 vector를 결합한 hybrid retrieval을 기본 검색으로 제공한다.
- Codex, CLI, future UI가 같은 API surface를 사용한다.
- Docker Compose로 로컬에서 재현 가능하게 실행한다.
- 장기 target은 Spring Boot MSA services 중심 구조이며, Python BM25 scaffold는 active runtime surface에서 제거했다.

## Control Surfaces

### Whole-System Control

- `docs/design/control-plane.md`
- `docs/design/ubiquitous-language.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/source-registry-and-project-ssot.md`
- root `AGENTS.md`

### Focused Execution

- `docs/projects/P0001-local-rag-system.md`
- 후속 `docs/tasks/T*.md`
- WBS와 gate가 있는 implementation task

### Drift Control

- `Goal Inventory`
- `Goal Verification`
- `docs/guide/quality-axes.md`
- YAML frontmatter properties
- `source_refs`
- `./docs/bin/validate-codex-readiness.sh`
- `./docs/bin/validate-harness-foundation.sh`
- `./docs/bin/validate-doc-retrieval.sh`
- `./docs/bin/validate-closeout.sh`
- Compose contract validation: `docker compose --env-file .env.example config`

### Codex Agent Control

- root `AGENTS.md`
- `docs/_templates/agents.md`
- `docs/guide/codex-agent-guidance.md`
- `docs/bin/validate-codex-readiness.sh`

### Source-Backed Knowledge

- 참조 원문은 `source_refs`에 경로로 연결하되 runtime source boundary와 혼동하지 않는다.
- 생성 문서는 `source_refs`로 원문 경로를 연결한다.
- `design`은 current truth, `project`와 `task`는 execution history를 담당한다.

## Active Design Surfaces

| Surface | Purpose | Status | Notes |
| --- | --- | --- | --- |
| `docs/design/control-plane.md` | 전체 목표, active surfaces, validators 정렬 | Active | 이 문서 |
| `docs/design/ubiquitous-language.md` | canonical term 정렬 | Active | Local RAG domain terms 포함 |
| `docs/design/local-rag-system-development-direction.md` | 개발 방향, architecture, invariants, API 경계 | Active | 구현 task의 primary design input |
| `docs/design/source-registry-and-project-ssot.md` | 장비별 source registry, project id, SSOT 등록, Codex/RAG skill scope | Active | source registration implementation의 primary design input |
| `docs/design/msa-runtime-and-storage.md` | MSA runtime, Docker Compose, PostgreSQL DDL, Weaviate schema | Active | runtime/storage implementation의 primary design input |
| `docs/design/retrieval-quality-improvement-design.md` | 검색 품질 evaluation, source weighting, rerank, chunking 개선 | Active | P0002/T0013 retrieval governance hardening의 primary design input |

## Umbrella Initiative Policy

- human이 인식하는 `local-rag-system` functional baseline은 `P0001`로 닫고, 후속 delivery boundary가 명확히 분리될 때만 exception branch project를 둔다.
- 새 work는 먼저 active project 아래의 새 `task`로 수용 가능한지 검토한다.
- 새 `project` 발급은 사용자 명시 요청 또는 completion boundary/owner/검증 체계가 분리되는 예외가 명확할 때만 허용한다.
- exception branch project가 필요하면 왜 task가 아닌지와 왜 human에게 별도 project가 더 읽기 쉬운지 남긴다.

## Active Umbrella Projects

| Project | Initiative | Status | Notes |
| --- | --- | --- | --- |
| `docs/projects/P0002-retrieval-governance-hardening.md` | governed Hybrid RAG retrieval quality | Active | P0001 functional baseline 이후 metadata/chunking/filter/answer/evaluation/audit hardening owner |

## Active Execution Surfaces

| Surface | Purpose | Status | Notes |
| --- | --- | --- | --- |
| `docs/projects/README.md` | active project 입구 | Active | `P0002` active, `P0001` done |
| `docs/tasks/README.md` | active task 입구 | Active | `T0013` P0002 critical path active; `T0010` operation-zone follow-up active |
| `docs/guide/sdlc-automation.md` | SDLC 자동화 목표, critical path, gate, verification ladder | Active | implementation session entry guide |
| `docs/reports/README.md` | active report 입구 | Active | 현재 active report 없음 |
| `docs/design/README.md` | design retrieval 입구 | Active | domain design 포함 |
| `docs/_indexes/active-docs.md` | active docs retrieval index | Active | README active surface와 함께 유지 |
| `docs/_indexes/design-map.md` | compact design retrieval map | Active | design README에서 파생 |
| `docs/_indexes/context-packets.yaml` | context packet manifest | Active | default broad-load guard 대상 |
| `docs/guide/context-loading-playbooks.md` | work-type context loading rules | Active | 하네스 기본 가이드 |

## Standard Pipeline

| Stage | Enters When | Produces | Exit Gate |
| --- | --- | --- | --- |
| Whole alignment | 목표, 용어, 범위가 흐릴 때 | `control-plane`, `ubiquitous-language`, domain `design` | 전체 목표와 MVP boundary가 잠김 |
| Project issue | human-facing initiative owner가 필요할 때 | umbrella `project` | lineage, scope, WBS, whole-system anchor 고정 |
| Task issue | 구현 가능한 실행 slice가 생길 때 | `task` | goal inventory, handoff, quality axes 고정 |
| Register | 장비별 source roots와 project ids가 필요할 때 | `SourceRegistry`, `ProjectRegistration` | 등록 source만 indexing/search 대상 |
| Scaffold | runtime baseline이 필요할 때 | Spring Boot MSA services, compose, health | local runtime이 재현 가능 |
| Index | source freshness를 구현할 때 | scanner, watcher, state, index jobs | 생성/수정/삭제 smoke 통과 |
| Retrieve | 검색 기능을 구현할 때 | hybrid search API, citation, filters | sample query smoke 통과 |
| Integrate | Codex/CLI/UI 경로가 필요할 때 | MCP tool 또는 REST bridge | Codex smoke 호출 통과 |
| Improve | 검색 품질 기준선 이후 | rerank, graph expansion, evaluation set | 회귀 측정 가능 |
| Closeout | 문서를 닫을 수 있을 때 | `done` 상태와 evidence | goal verification 전부 `Done` |

## Quality Axes

- 기본 품질 축은 `docs/guide/quality-axes.md`를 따른다.
- 이 project의 active axis는 WHOLE, SCOPE, HANDOFF, EVIDENCE, SECURITY다.
- 각 task는 어떤 axis를 직접 책임지는지 `Quality Axes In Scope`에 적어야 한다.

## Required Validators

- `./docs/bin/validate-harness-foundation.sh`
- `./docs/bin/validate-codex-readiness.sh`
- `./docs/bin/validate-doc-retrieval.sh`
- `./docs/bin/validate-closeout.sh --all`
- Spring Boot build: Docker Maven build, or local `./mvnw test` if a wrapper is added later
- 후속 runtime 추가 후 추가: Docker Compose smoke validator

## Handoff Rules

- `design`은 현재 truth를 잠그고 `project`와 `task`가 이를 읽는다.
- `P0001`은 functional baseline lineage와 completion boundary를 보존한다.
- `P0002`는 retrieval governance exception branch lineage와 completion boundary를 보존한다.
- 구현은 active project 아래의 task로 분해한다.
- raw source는 일반 파일 시스템 경로로 참조하고 생성 문서는 `source_refs`로 연결한다.
- project planning/meta source는 repo SSOT가 아니라 planning/meta layer로 취급한다.
- source registry와 project SSOT 구현 task는 `docs/design/source-registry-and-project-ssot.md`를 primary design input으로 읽는다.
- Java/Spring 외 구현을 추가하려면 먼저 design 문서에서 active runtime boundary를 갱신한다.
- Spring Boot 구현 task는 `docs/design/local-rag-system-development-direction.md`와 `docs/design/msa-runtime-and-storage.md`의 invariants와 interfaces를 우선한다.
- report는 시점성 정리를 담되, 재사용 가치가 생기면 `design`, `guide`, `project`, `task`로 승격한다.
- SDLC 자동화 세션은 `docs/guide/sdlc-automation.md`를 읽고 active critical path task 하나를 우선 진행한다.
- Retrieval governance 세션은 `docs/projects/P0002-retrieval-governance-hardening.md`, `docs/tasks/T0013-retrieval-chunking-and-document-authority-hardening.md`, and `docs/design/retrieval-quality-improvement-design.md`를 우선 읽는다.

## Change Log

- 2026-05-24: `local-rag-system` 전용 control-plane으로 초기화. 원문 경로와 document-harness 적용 결과를 연결.
- 2026-05-24: 장비별 source registry, project SSOT registration, compiled knowledge source 결정을 whole-system outcome과 active design surfaces에 반영.
- 2026-05-24: MSA runtime, Docker Compose, PostgreSQL DDL, Weaviate schema design surface를 active control plane에 추가.
- 2026-05-24: Python BM25 scaffold를 active runtime surface에서 제거하고 Spring Boot MSA target으로 단일화.
- 2026-05-24: SDLC automation guide와 T0003 first implementation gate를 active execution surface에 추가.
- 2026-05-25: `T0010`을 active execution surface로 추가해 Codex MCP discovery, API/tool contract drift, source-safe document fetch, invalid project handling, and self-indexing registration remediation을 추적.
- 2026-05-25: `retrieval-quality-improvement-design`과 `T0011`을 추가해 evaluation-backed retrieval quality hardening을 추적.
- 2026-05-25: `T0011` 개발존 구현 완료. Retrieval evaluation runner, deterministic ranker, primary source weighting, and document diversity control are now part of the implementation baseline.
- 2026-05-29: Ollama endpoint 설정은 notebook-local과 LAN-local Mac mini를 모두 담을 수 있는 ordered local endpoint list로 확장했다.
- 2026-05-29: `P0002`를 P0001 functional baseline 이후 active retrieval governance hardening exception branch로 추가했다. `T0013`은 metadata-aware chunking and document authority indexing의 첫 critical-path task다.
