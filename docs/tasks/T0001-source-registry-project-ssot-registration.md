---
type: task
doc_id: T0001
title: source-registry-project-ssot-registration
status: done
owner:
created: 2026-05-24
updated: 2026-05-24
current_focus: "Completed source registry runtime contract and project-scoped source resolution"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - source:planning/source-registry-ssot-strategy-note
  - source:planning/local-rag-system-project-note
  - source:planning/local-rag-system-design-note
quality_axes:
  - WHOLE
  - SCOPE
  - HANDOFF
  - EVIDENCE
  - SECURITY
tags:
  - docs/task
  - source-registry
  - ssot
---

# T0001 source-registry-project-ssot-registration

- Type: task
- Document ID: T0001
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-24
- Current Focus: Completed source registry runtime contract and project-scoped source resolution
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`

## Purpose

이 task는 local RAG가 장비별 SSOT 위치와 프로젝트 식별자를 명시적으로 등록하고, Codex 같은 RAG skill이 `project_id` 기준으로 안전하게 검색할 수 있게 하는 첫 구현 slice다.

단순히 문서에 source path를 적는 것이 아니라, 이후 indexer/search/API가 공통으로 사용할 `SourceRegistry`, `ProjectRegistration`, `SourceRoot`, `ssot_role`, `read/write policy` 계약을 구현 가능 상태로 만든다.

## Task Placement Check

- 이 작업은 `local-rag-system` 전체 목표의 첫 runtime boundary를 여는 작업이므로 `P0001` 아래 task가 맞다.
- 별도 project가 필요하지 않은 이유는 source registry가 독립 제품이 아니라 indexing/search/Codex bridge가 공유해야 하는 control surface이기 때문이다.
- 후속 Spring Boot service scaffold, scanner, search API task는 이 registry contract를 입력으로 읽는다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 "local RAG는 등록된 local source roots만 검색하고, 프로젝트별 repo docs SSOT와 보조 compiled knowledge source를 명확히 구분한다"는 경계다.

깨면 안 되는 invariant:

- 등록되지 않은 folder는 indexing/search 대상이 아니다.
- repo `docs/`는 해당 repo의 `project-current-truth`다.
- compiled knowledge source는 `compiled-wiki` 보조 source다.
- planning/meta source는 repo SSOT가 아니라 `planning-meta`다.
- search output은 evidence이지 write authorization이 아니다.
- source registration은 operator action이며, Codex search call이 자동 등록하지 않는다.

## Completion Mode Notes

Completion mode는 `functional`이다. 이 task가 닫히려면 registry contract가 문서에만 있지 않고 target Spring Boot service에서 실행 가능한 형태로 검증되어야 한다.

첫 구현은 Spring Boot service scaffold 안의 Java package로 진행한다. Python BM25 scaffold는 active runtime surface에서 제거되었으므로 새 기능 구현 경로로 사용하지 않는다.

## Committed Outcome

이 task가 `done`일 때 가능해야 하는 것:

- local registry YAML을 load/validate할 수 있다.
- `project_id`로 project registration을 조회할 수 있다.
- `project_id`와 query scope로 검색 대상 `source_id` 목록을 결정할 수 있다.
- active/inactive source, missing path, duplicate id, invalid primary SSOT source를 검증에서 잡는다.
- indexed chunk 또는 search result metadata에 `project_id`, `source_id`, `ssot_role`, `source_type`, `write_policy`가 전달된다.
- Codex/RAG skill이 호출할 API contract가 `rag_list_projects`, `rag_list_sources`, `rag_search` 기준으로 고정된다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Source registry schema와 validation rule을 구현 기준으로 고정한다. | schema fields, allowed values, validation failures가 design과 tests에 반영된다. |
| G2 | Project registration과 search scope resolver를 구현한다. | `project_id`로 primary SSOT와 default context source가 결정된다. |
| G3 | Registry metadata를 indexing/search result로 전달한다. | search result 또는 stored chunk가 `project_id`, `source_id`, `ssot_role`을 포함한다. |
| G4 | Codex/RAG skill contract를 registry-aware 형태로 고정한다. | `rag_list_projects`, `rag_list_sources`, `rag_search`, `rag_get_document`, `rag_index_status` contract가 문서와 code/API에 정렬된다. |
| G5 | 검증 가능한 baseline evidence를 남긴다. | focused tests와 docs validators가 통과하거나, 미실행 사유가 명시된다. |

## Scope

- source registry schema
- registry loader/validator
- project registration model
- source root model
- source type and SSOT role enum/value contract
- project-scoped search source resolution
- registry metadata propagation contract
- Codex/RAG skill names and request/response fields
- tests for registry validation and source resolution

## Out Of Scope

- Weaviate schema implementation
- Ollama embedding integration
- full file watcher/scanner runtime
- Docker Compose runtime
- PDF/OCR/canvas parsing
- automatic discovery of every repo under `/path/to/workspace`
- write automation into source folders or project docs

## References

- `docs/design/control-plane.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/source-registry-and-project-ssot.md`
- `docs/projects/P0001-local-rag-system.md`
- `source:planning/source-registry-ssot-strategy-note`

## Dependencies

- Current design docs must remain aligned with this task.
- Spring Boot service scaffold task를 먼저 열거나 갱신한 뒤 registry code changes를 시작한다.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Lock registry design and terminology | Done | 100% | `source-registry-and-project-ssot.md` added |
| W2 | Define implementation model and validation cases | Done | 100% | Java registry records and validator implemented |
| W3 | Implement registry loader/validator | Done | 100% | `SourceRegistryLoader`, `SourceRegistryValidator` |
| W4 | Implement project-scoped source resolution | Done | 100% | retrieval resolves active sources by `project_id` |
| W5 | Propagate registry metadata to chunks/results | Done | 100% | chunks/results include project/source/SSOT metadata |
| W6 | Verify focused tests and docs validators | Done | 100% | Maven tests and runtime registry smoke passed |

## Overall Progress

- 100%

## Completion Criteria

1. Registry schema and allowed values are documented in `docs/design/source-registry-and-project-ssot.md`.
2. Runtime or baseline code can load and validate a source registry.
3. `project_id` search scope resolution is covered by tests.
4. Search/index metadata includes project/source/SSOT role fields or the implementation handoff explicitly defines where those fields are introduced.
5. `docs/projects/P0001-local-rag-system.md`, `docs/design/README.md`, and retrieval indexes expose this task/design relationship.
6. Focused tests and document validators pass, or skipped validators are explained.

## Completion Evidence

- source registry validation test output
- project scope resolver test output
- sample registry fixture or example path
- docs validator output
- updated design/project/task docs

Documentation-only evidence is not enough unless this task is explicitly reissued as `design-lock`; current mode is `functional`.

## Outputs / Handoff

- `SourceRegistry` implementation contract
- project/source identifier contract for Codex/RAG skill
- validation cases for active/missing/duplicate/invalid source roots
- search scope resolution rules for future retrieval/API services
- metadata propagation requirements for future index/search implementation

Downstream tasks:

- Spring Boot service scaffold task consumes the model/package boundaries.
- scanner/indexer task consumes active source roots and include/exclude rules.
- search API task consumes `project_id` and source resolution.
- Codex bridge task consumes tool names and response metadata.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Registry is the first control surface for all later indexing/search work. | P0001, control-plane, design docs reference the same registry contract |
| SCOPE | The task must not turn into full RAG implementation. | Out Of Scope leaves Weaviate, watcher, embedding, Docker to later tasks |
| HANDOFF | Later tasks need exact fields and validation behavior. | Schema, API contract, model names, tests |
| EVIDENCE | Boundary correctness must be executable, not only descriptive. | focused registry tests and validators |
| SECURITY | Incorrect source selection can expose private or stale context. | `ssot_role`, `sensitivity`, read/write policy validation |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | Registry Java records, YAML fixture, validator, and smoke validation | Runtime validation catches duplicates, missing paths, invalid primary source |
| G2 | Done | `/api/registry/projects`, `/api/registry/sources`, `/api/registry/projects/{projectId}/scope` | `project_id` determines active source scope |
| G3 | Done | Weaviate chunk object and search result include `projectId`, `sourceId`, `ssotRole`, `sourceType` | Metadata propagates through indexing and retrieval |
| G4 | Done | Gateway and MCP REST bridge expose list/search/status/force-scan paths | True MCP transport remains out of scope for this task |
| G5 | Done | Maven tests passed; registry smoke returned `valid: true`, `projects: 1`, `sources: 1` | See functional smoke report |

## Completion Guardrails

- Do not close this task with design docs only while `completion_mode: functional`.
- Do not let registry auto-discover and index all local repositories.
- Do not treat planning/meta source folders as project repo SSOT.
- Do not let Codex search calls mutate registry state implicitly.
- Do not omit `ssot_role` and `source_id` metadata from chunks/results.
- Do not create a new project for this work unless P0001 scope is explicitly superseded.

## Risks / Open Questions

- Whether to implement registry inside `source-registry-service` first or shared Java library first.
- Whether compiled knowledge sources should be default context for every project or enabled per project.
- Whether code source roots should be registered as paused sources or task-scoped transient sources.

## Status

- 2026-05-24: task 문서 생성. Source registry and project SSOT registration design을 구현 handoff로 연결.
- 2026-05-24: SDLC automation alignment에서 `T0003` Spring Boot skeleton 완료 전까지 blocked로 전환.
- 2026-05-24: Spring Boot skeleton completion unblocked implementation. Source registry loader/validator/synchronizer, API endpoints, project-scoped retrieval resolution, and metadata propagation completed as part of the functional baseline.
