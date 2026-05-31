---
type: design
title: source-registry-and-project-ssot
status: current
domain: source-registry
owner:
created: 2026-05-24
updated: 2026-05-24
retrieval_class:
  - domain-current
context:
  default_load: false
  section_load: false
  evidence_only: false
  size_tier: medium
referenced_by:
  - docs/design/control-plane.md
  - docs/design/local-rag-system-development-direction.md
  - docs/projects/P0001-local-rag-system.md
  - docs/tasks/T0001-source-registry-project-ssot-registration.md
source_refs:
  - source:planning/source-registry-ssot-strategy-note
  - source:planning/local-rag-system-project-note
  - source:planning/local-rag-system-design-note
tags:
  - docs/design
  - local-rag-system
  - source-registry
  - ssot
---

# source-registry-and-project-ssot

- Type: design
- Domain: source-registry
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-24
- Referenced By:
  - `docs/design/control-plane.md`
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/projects/P0001-local-rag-system.md`
  - `docs/tasks/T0001-source-registry-project-ssot-registration.md`

## Purpose

이 문서는 `local-rag-system`이 장비별 SSOT 위치와 프로젝트 식별자를 어떻게 등록하고, Codex 같은 RAG skill이 어떤 프로젝트 문서 집합을 우선 검색해야 하는지 고정한다.

핵심 결정은 파일 시스템을 symlink로 섞지 않고, local RAG가 `SourceRegistry`와 `ProjectRegistration`을 통해 명시적으로 등록된 source root만 검색한다는 것이다.

## Decision

Local RAG는 장비별 source registry를 가진다.

각 장비는 자신이 실제로 다루는 프로젝트만 `project_id`와 SSOT 경로로 등록한다. 등록되지 않은 폴더는 존재하더라도 RAG 대상이 아니다.

기본 SSOT 분리는 다음과 같다.

- repo `docs/`는 해당 repo의 코드/운영/설계 current truth다.
- compiled knowledge source는 개인 지식, cross-project synthesis, 장기 wiki memory다.
- planning/meta source는 repo SSOT가 아니라 personal planning/meta layer다.
- local RAG index는 SSOT가 아니라 검색용 derived state다.
- Codex/RAG skill은 registry를 읽어 현재 작업 프로젝트의 SSOT를 먼저 검색하고, 필요할 때 compiled knowledge source를 보조 source로 검색한다.

## Runtime Boundary

포함하는 책임:

- 장비별 source registry loading
- `project_id` 기반 project registration lookup
- source root path validation
- source별 read/write policy, priority, sensitivity default, include/exclude rule
- registry metadata를 indexing pipeline과 search API에 전달
- Codex/RAG skill이 `project_id`로 scoped search를 호출할 수 있는 API contract

포함하지 않는 책임:

- registry가 source file 자체의 truth를 대체하는 것
- planning/meta notes를 repo docs의 SSOT로 승격하는 것
- symlink를 통해 프로젝트별 docs tree에 별도 knowledge corpus 전체를 삽입하는 것
- inactive repository를 자동으로 모두 indexing하는 것
- Codex가 registry만 보고 source file을 열지 않아도 된다고 판단하는 것

## Registry Location

장기 runtime은 local-only config file을 읽는다.

권장 기본값:

```text
~/.config/local-rag-system/source-registry.yaml
```

대체 경로:

```text
LOCAL_RAG_SOURCE_REGISTRY=/absolute/path/to/source-registry.yaml
```

Repo에는 실제 개인 경로가 들어간 active config를 커밋하지 않는다. 개발용 예시는 `docs/examples/` 또는 후속 task의 `config/source-registry.example.yaml`로 둔다.

이 config는 machine-local truth다. Git, 수동 복사, 파일 동기화 도구로 공유될 수는 있지만, 각 장비의 mounted path와 active project set이 다를 수 있으므로 registry 자체는 장비 프로파일을 명시해야 한다.

## Identifier Contract

식별자는 사람이 바꾸는 표시명과 분리한다.

| Field | Meaning | Rule |
| --- | --- | --- |
| `device_id` | 장비 프로파일 식별자 | 사람이 읽을 수 있는 kebab-case. 예: `macbook-m4`, `mac-studio` |
| `project_id` | Codex/RAG skill이 사용하는 stable project key | repo 또는 knowledge domain 기준 kebab-case. 예: `project-alpha`, `project-beta`, `personal-notes` |
| `source_id` | indexing 대상 root key | `project_id.layer` 형식 권장. 예: `project-alpha.docs`, `personal-notes` |
| `display_name` | UI/로그 표시명 | 변경 가능. key로 사용하지 않음 |
| `ssot_role` | 해당 source의 truth 역할 | `project-current-truth`, `compiled-wiki`, `planning-meta`, `historical-evidence`, `code-source` 중 하나 |

`project_id`는 query API, Codex skill, context packet, index status에서 모두 같은 값을 사용한다.

## Source Root Types

| Type | Default Search | Write Policy | Example |
| --- | --- | --- | --- |
| `project-docs` | current project에서는 yes | project agent가 해당 repo rules에 따라 write | `project-alpha/docs` |
| `compiled-wiki` | cross-project 보조로 yes | 해당 source의 운영 규칙 필요 | `knowledge/compiled` |
| `planning-meta` | opt-in | planning/meta source 규칙에 따라 write | `knowledge/projects/local-rag-system` |
| `source-folder` | opt-in, scoped | 원본 source 보호 | `knowledge/resources/...` |
| `code-source` | task별 opt-in | repo code edit workflow | `src/`, `services/.../src/` |
| `archive` | explicit historical reason only | 기본 read-only | `docs/legacy`, `09. Archive` |

Default search에서 `archive`와 일반 `source-folder`를 제외하는 이유는 검색 noise와 sensitive exposure를 줄이기 위해서다.

## Source Registry Schema

초기 YAML shape:

```yaml
version: 1
device_id: macbook-m4
default_project_id: project-alpha

projects:
  - project_id: project-alpha
    display_name: Project Alpha
    repo_path: /path/to/workspace/project-alpha
    primary_source_id: project-alpha.docs
    codex_cwd: /path/to/workspace/project-alpha
    default_context:
      - project-alpha.docs
      - personal-notes

  - project_id: personal-notes
    display_name: Personal Notes
    primary_source_id: personal-notes
    default_context:
      - personal-notes

sources:
  - source_id: project-alpha.docs
    project_id: project-alpha
    type: project-docs
    ssot_role: project-current-truth
    path: /path/to/workspace/project-alpha/docs
    priority: 100
    active: true
    sensitivity_default: private
    include:
      - "**/*.md"
      - "**/*.yaml"
      - "**/*.yml"
    exclude:
      - "**/.git/**"
      - "**/.rag_index/**"
      - "**/_templates/**"
      - "**/node_modules/**"
      - "**/build/**"
      - "**/dist/**"
    read_policy: registered-default
    write_policy: repo-docs

  - source_id: personal-notes
    project_id: personal-notes
    type: compiled-wiki
    ssot_role: compiled-wiki
    path: "${LOCAL_RAG_SOURCE_PERSONAL_NOTES}"
    priority: 70
    active: true
    sensitivity_default: private
    include:
      - "**/*.md"
    exclude:
      - "**/*.json"
    read_policy: cross-project-support
    write_policy: source-specific-rule-required
```

Required validation:

- `version` is supported.
- `device_id` is present.
- every `project_id` is unique.
- every `source_id` is unique.
- every `primary_source_id` exists in `sources`.
- every active source path exists and is readable.
- source path is absolute.
- `source_id` and `project_id` use stable kebab/dot identifiers.
- include/exclude rules are syntactically valid.
- write policy is explicit.

The scanner must enforce registered `include` and `exclude` globs before indexing. Registry globs are not documentation-only metadata: an active source can only produce chunks for supported files that pass the source filter.

## Search Defaults

When a query includes `project_id`, retrieval uses this order:

1. registered project's primary SSOT source,
2. source ids listed in the registered project's `default_context` order,
3. registered project's other active sources by priority,
4. explicitly requested sources,
5. archive or source notes only when query asks for historical/raw evidence.

`default_context` may include a source from another registered project, such as a compiled wiki source. In that case retrieval filters by the resolved source ids, not by a single project id, so cross-project support sources can participate without being mislabeled as the primary project truth.

For a project task, Codex should first receive a context packet that favors:

- root `AGENTS.md`,
- `docs/README.md`,
- `docs/design/control-plane.md`,
- active design map and active docs index,
- relevant `design` and `guide`,
- relevant `project` and `task`,
- compiled knowledge synthesis only after project current truth.

## API Contract

Search request:

```json
{
  "projectId": "project-alpha",
  "query": "source registry Codex skill contract",
  "limit": 10,
  "mode": "hybrid",
  "scope": "default",
  "includeSourceIds": [],
  "excludeSourceIds": [],
  "filters": {
    "ssotRole": ["project-current-truth", "compiled-wiki"],
    "status": ["current", "active"],
    "sensitivity": ["private", "work"]
  },
  "contextPacket": true
}
```

Search response:

```json
{
  "projectId": "project-alpha",
  "sourcesSearched": ["project-alpha.docs", "personal-notes"],
  "results": [
    {
      "sourceId": "project-alpha.docs",
      "projectId": "project-alpha",
      "ssotRole": "project-current-truth",
      "path": "docs/design/control-plane.md",
      "heading": "Active Design Surfaces",
      "score": {
        "bm25": 0.82,
        "vector": 0.77,
        "rerank": null
      },
      "citation": "project-alpha:docs/design/control-plane.md#Active Design Surfaces",
      "snippet": "..."
    }
  ]
}
```

Required skill/tool names:

```text
rag_list_projects
rag_list_sources
rag_search
rag_get_document
rag_index_status
rag_force_scan
```

Later operator-only tools:

```text
rag_validate_registry
rag_register_source
rag_deactivate_source
```

Codex-facing search should not register new sources implicitly. Registration is an operator action because it changes the retrieval boundary and possible sensitive exposure.

## Indexing Metadata

Every chunk must carry registry metadata:

```text
device_id
project_id
source_id
source_type
ssot_role
root_path
relative_path
repo_path
doc_type
frontmatter_status
sensitivity
updated
git_commit
index_policy
write_policy
```

Without this metadata, the system cannot safely decide whether a result is current project truth, compiled synthesis, archived evidence, or raw sensitive material.

## Write Policy

RAG search never decides the write destination by itself. It can recommend a destination based on metadata.

Default write rules:

- project-specific implementation/design/task truth -> project repo `docs/`,
- local-rag-system implementation truth -> the checked-out `local-rag-system/docs` in the active development or operation zone,
- cross-project or personal synthesis -> registered compiled knowledge source,
- planning/meta notes -> registered planning/meta source,
- historical source evidence -> archive/source stays read-only unless user asks for migration.

If Codex is operating inside a project repo, it writes to that repo unless the user explicitly asks to update another registered source.

## Lifecycle

Registration lifecycle:

```text
proposed -> validated -> active -> indexed -> monitored
active -> paused
active -> retired
validated -> rejected
```

State meanings:

- `proposed`: config exists but not accepted for indexing.
- `validated`: paths and policies pass validation.
- `active`: source is eligible for scan/index.
- `indexed`: at least one successful scan has completed.
- `monitored`: watcher/scanner freshness is running.
- `paused`: preserved but excluded from default indexing/search.
- `retired`: no longer indexed; existing chunks must be deleted or marked inactive.

Source changes must be explicit. A folder becoming present on disk does not automatically register it.

## Implementation Handoff

The first implementation task should produce:

- `SourceRegistry` loader and validator,
- data classes for `DeviceProfile`, `ProjectRegistration`, `SourceRoot`, `IndexPolicy`, and `AccessPolicy`,
- CLI command to validate registry,
- index metadata propagation from source root to chunk,
- search API input field `projectId`,
- default source selection by `projectId`,
- tests for duplicate ids, missing paths, invalid primary SSOT source, inactive source exclusion, and compiled knowledge source fallback.

Suggested Java package names for target Spring Boot implementation:

```text
registry/
  SourceRegistry
  SourceRegistryLoader
  SourceRegistryValidator
  ProjectRegistration
  SourceRoot
  SourceRootType
  SsotRole
  AccessPolicy
  IndexPolicy

search/
  SearchScopeResolver
  SearchQuery
  SearchResult
```

## Risks

| Risk | Mitigation |
| --- | --- |
| All local repos are indexed by accident | registry-only indexing; no implicit directory crawl outside registered source roots |
| Planning/meta notes are mistaken for repo SSOT | `ssot_role=planning-meta` and lower default priority |
| legacy/archive results outrank active docs | default filters prefer `project-current-truth` and `compiled-wiki`; archive opt-in |
| private material leaks into unrelated project context | `project_id`, `source_id`, `sensitivity`, and `default_context` filters |
| Codex writes to another source through search result path | write policy is explicit; search output is evidence, not write authorization |
| Local path availability differs by machine | device profile and validation status are per-machine |

## Open Questions

- Should source registry live only in YAML, or should source-registry-service persist normalized registry state in the state store?
- Should compiled knowledge sources be global defaults for every project, or enabled per project through `default_context` only?
- Should code source indexing be opt-in per task, or registered as a paused source that Codex can activate for code navigation?
- Should retired source chunks be deleted immediately or marked inactive for audit?

## Change Log

- 2026-05-24: Initial design lock for per-device source registry, project SSOT registration, and Codex/RAG skill scoping.
