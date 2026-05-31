---
type: task
doc_id: T0010
title: codex-rag-utilization-hardening
status: active
owner:
created: 2026-05-25
updated: 2026-05-29
current_focus: "Support initial user-managed source slots in the ~/Services operation registry"
completion_mode: remediation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - integrations/codex/README.md
  - integrations/codex/local-rag-mcp-server.mjs
  - integrations/codex/skill/SKILL.md
  - docs/tasks/T0007-codex-global-rag-integration.md
quality_axes:
  - CONTRACT
  - EVIDENCE
  - HANDOFF
  - SECURITY
tags:
  - docs/task
  - local-rag-system
  - codex
  - mcp
  - remediation
---

# T0010 codex-rag-utilization-hardening

- Type: task
- Document ID: T0010
- Status: active
- Completion Mode: remediation
- Owner:
- Created: 2026-05-25
- Updated: 2026-05-29
- Current Focus: Support initial user-managed source slots in the ~/Services operation registry
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 `T0007`에서 설치한 Codex global RAG integration을 실제 Codex 사용 관점에서 hardening한다.

현재 runtime, index, global config, adapter 자체는 동작하지만, 활성 Codex 세션에서 `mcp__local_rag__...` tool이 항상 노출된다고 보장되지 않고, 일부 API/MCP 계약이 실제 구현과 어긋나 있다. 이 task의 목적은 Codex가 registry에 등록된 project/source 관련 질문에서 Local RAG를 안정적으로 발견하고, 실패 시 명확한 오류를 받고, 필요한 경우 source-safe document fetch까지 사용할 수 있는 상태로 만드는 것이다.

## Task Placement Check

- 이 작업은 새 RAG 제품이 아니라 기존 `P0001`의 Codex integration remediation slice다.
- `T0007`은 installable global integration을 닫았고, 이 task는 실제 사용성, tool discovery, API contract drift, smoke/doctor evidence를 보완한다.
- 별도 `project`를 발급하지 않는 이유는 source registry, retrieval API, MCP adapter, Codex skill 모두 기존 `local-rag-system` umbrella의 하위 책임이기 때문이다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 Codex가 local-only RAG를 project knowledge source로 우선 활용하되, source registry와 read-only source boundary를 우회하지 않는 것이다.

깨면 안 되는 invariant:

- Codex integration은 등록 source root 밖 파일을 직접 읽지 않는다.
- MCP adapter는 검색/색인 로직을 갖지 않고 gateway REST bridge만 호출한다.
- private source content는 외부 hosted API로 전송하지 않는다.
- unknown `projectId`는 backend 500이 아니라 operator/debug 가능한 client error로 실패해야 한다.
- MCP tool schema, REST bridge, health surface, skill 문구는 같은 tool contract를 말해야 한다.
- Codex restart 없이는 새 global MCP discovery가 보장되지 않는다는 운영 전제를 문서화한다.

## Completion Mode Notes

Completion mode는 `remediation`이다. 이미 설치된 Codex integration의 결함과 사용성 gap을 보완해, 새 세션에서 반복 검증 가능한 상태로 닫는다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- Codex global config와 MCP adapter가 새 Codex 세션에서 실제 tool로 노출되는지 검증하는 절차가 있다.
- `rag_search` mode contract가 `hybrid`, `vector`, `keyword`로 정렬되고, legacy `bm25` 입력은 `keyword`로 normalize된다.
- health, MCP adapter, REST bridge가 같은 tool 목록을 광고한다.
- `rag_get_document`가 source-safe하게 구현되거나, 구현 전까지 모든 광고 surface에서 제거된다. 권장 완료 방향은 구현이다.
- unknown project 검색은 400 계열 client error로 반환되고 audit FK violation 500을 만들지 않는다.
- `local-rag-system` 자체 문서가 source registry에 등록되어 검색 가능하다.
- Codex skill이 등록 프로젝트 관련 질문에서 Local RAG를 먼저 쓰도록 더 명확해진다.
- 설치 후 사용할 수 있는 `doctor` 또는 smoke script가 gateway health, index status, MCP framing, search, document fetch, unknown project failure를 검증한다.
- 초기 운영 source set은 generic `/sources/source-06..08` slots로 확장 가능하며, 실제 source names and host paths는 untracked local registry/config에만 둔다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | MCP discovery를 실제 Codex 사용 관점에서 검증한다. | adapter framing smoke와 Codex restart/new-session tool visibility check가 문서화되고 통과한다. |
| G2 | API/MCP/search mode contract drift를 제거한다. | `hybrid`, `vector`, `keyword`, legacy `bm25` smoke가 모두 기대한 응답을 반환한다. |
| G3 | source-safe document fetch 계약을 정렬한다. | `rag_get_document`가 구현되어 smoke를 통과하거나, 모든 advertised surface에서 제거된다. |
| G4 | invalid project failure를 명확하게 만든다. | unknown `projectId` 검색이 400 계열 response와 명확한 message를 반환하고 서버 로그에 FK violation을 남기지 않는다. |
| G5 | `local-rag-system` 문서도 RAG 대상에 포함한다. | `local-rag-system.docs` source가 등록되고 force scan/search smoke가 citation을 반환한다. |
| G6 | Codex 사용 규칙을 RAG-first에 가깝게 강화한다. | skill 문서가 등록 프로젝트 질문, freshness 질문, fallback 경로, citation 규칙을 명확히 말한다. |
| G7 | 반복 가능한 operator verification을 제공한다. | `doctor` 또는 equivalent script가 전체 Codex RAG readiness를 검증하고 설치 가이드에서 호출된다. |

## Scope

- Codex MCP adapter schema and smoke verification
- REST bridge and health advertised tool alignment
- retrieval request validation and mode normalization
- source-safe document fetch endpoint/tool or explicit deferral cleanup
- source registry addition for `local-rag-system.docs`
- Codex skill wording and install guide updates
- operation-zone config/update notes for this device
- focused tests and smoke scripts

## Out Of Scope

- multi-user auth
- hosted search provider integration
- reranker/evaluation-set quality optimization
- non-Markdown attachment parsing
- replacing Weaviate
- changing Codex's internal MCP loader implementation beyond documented config/restart requirements

## References

- `docs/projects/P0001-local-rag-system.md`
- `docs/tasks/T0007-codex-global-rag-integration.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/source-registry-and-project-ssot.md`
- `docs/design/msa-runtime-and-storage.md`
- `integrations/codex/README.md`
- `integrations/codex/local-rag-mcp-server.mjs`
- `integrations/codex/skill/SKILL.md`
- `docs/guide/installing-on-a-new-machine.md`

## Dependencies

- `T0007` global integration baseline
- running local gateway at `http://127.0.0.1:42120`
- operation-zone config under `~/Services/local-rag-system/config`
- Codex restart after global MCP config changes

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Capture current gap and plan | Done | 100% | current session has REST fallback path, but MCP tools are not visible in the active tool surface |
| W2 | Fix MCP/search contract drift | Done | 100% | public modes are `hybrid`, `vector`, `keyword`; legacy `bm25` normalizes to `keyword` in DTO and adapter |
| W3 | Implement or remove `rag_get_document` | Done | 100% | implemented source-safe `sourceId + relativePath` fetch through indexer, bridge, gateway, and MCP adapter |
| W4 | Harden invalid project handling | Done | 100% | retrieval validates active project before query/audit and returns 400 for unknown `projectId` |
| W5 | Register `local-rag-system.docs` | Done | 100% | operation config registers `/sources/source-04`; force-scan and search citation smoke passed for `local-rag-system.docs` |
| W6 | Strengthen Codex skill and install guide | Done | 100% | skill now says RAG-first for registered project docs, freshness/status check, REST fallback, and citation rules |
| W7 | Add doctor/smoke verification | Done | 100% | `integrations/codex/smoke-local-rag.mjs` and `local-rag codex-smoke` passed adapter framing and runtime checks |
| W8 | Deploy to operation zone and verify | Done | 100% | deployed under `~/Services/local-rag-system`, installed `~/Services/bin/local-rag`, started Compose stack, installed Codex integration, and passed live smoke |
| W9 | Register initial user source set | Done | 100% | Generic `/sources/source-06..08` mounts and local registry override guidance support user-managed sources without committing machine-specific names |

## Overall Progress

- 98%

## Completion Criteria

1. `local-rag-system` repo validators and relevant service tests pass.
2. `docker compose --env-file .env.example config` passes.
3. MCP adapter framing smoke returns the implemented tool list.
4. A new Codex session can see `mcp__local_rag__rag_search` or the documented blocker is captured with fallback instructions.
5. `rag_search` works for `local-rag-system` and any user-managed project ids returned by the active registry.
6. Unknown `projectId` returns a 400-level response, not a 500.
7. `rag_get_document` behavior matches advertised tools.
8. `doctor` or equivalent smoke script is documented for new-machine and operation-zone installs.

## Completion Evidence

- command transcript or summarized output for validators/tests
- adapter framing smoke output
- REST/MCP search smoke output with citations
- unknown project error response
- document fetch smoke or explicit removal evidence
- source registry entry and index status for `local-rag-system.docs`
- Codex skill diff and installed skill check
- operation-zone health/index/search verification after restart

Evidence that is not sufficient alone:

- config file containing `[mcp_servers.local_rag]` without adapter and new-session visibility checks
- health response alone without search/tool smoke
- search audit rows from manual curl only, if Codex-facing tool discovery remains unverified

## Outputs / Handoff

- patched MCP adapter and REST bridge/API contract
- updated source registry examples and operation-zone config guidance
- `doctor` or smoke script path and install guide entry
- updated global skill source under `integrations/codex/skill/SKILL.md`
- follow-up note if Codex Desktop does not expose newly installed MCP servers until application restart

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| CONTRACT | Codex tool schemas, REST bridge, health, and docs must describe the same surface. | schema/docs/API smoke |
| EVIDENCE | "Codex can use RAG" must be proven through tool or adapter smoke, not inferred from config. | MCP framing and search smoke |
| HANDOFF | New machines and future sessions need one repeatable readiness check. | doctor script and install guide |
| SECURITY | document fetch cannot bypass source registry or expose arbitrary paths. | path traversal and registered-source checks |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | In Progress | `local-rag codex-smoke` passed adapter framing with 7 advertised tools after operation install | Active Codex Desktop session/new-session tool visibility still requires app restart verification; REST and adapter fallback are documented |
| G2 | Done | `SearchRequestTests`, adapter `tools/list`, and docs align on `hybrid`, `vector`, `keyword`; legacy `bm25` normalizes to `keyword` | Runtime mode smoke waits for operation deploy |
| G3 | Done | `DocumentFetchServiceTests` passed; `rag_get_document` is exposed by MCP adapter, mcp-bridge, gateway, and indexer with source-root checks | Source-safe implementation chosen instead of removal |
| G4 | Done | `RetrievalServiceTests` passed for unknown project 400 and unsupported mode 400 before search execution | Runtime unknown-project smoke is included in `smoke-local-rag.mjs` |
| G5 | Done | `~/Services/bin/local-rag force-scan local-rag-system` completed with no errors; `/api/search` returned citations from `local-rag-system.docs` | Operation source points to `~/Services/local-rag-system/code/docs` |
| G6 | Done | `integrations/codex/skill/SKILL.md` documents RAG-first use, freshness/index status, REST fallback, `rag_get_document`, and citation rule | Machine-local installed skill should be refreshed during operation deploy/install |
| G7 | Done | `~/Services/bin/local-rag codex-smoke` passed gateway health, registry, index status, search, document fetch, and unknown-project 400 checks | Full runtime smoke now runs without `--allow-empty-search` |

## Completion Guardrails

- Do not claim Codex uses RAG well because config exists; active tool visibility or fallback behavior must be verified.
- Do not expose `rag_get_document` until source-safe fetch is implemented.
- Do not register broad repository roots when only `docs/` should be indexed.
- Do not treat `local-rag-system` source registration as a hidden machine-local side effect; examples and operation config guidance must show the contract.
- Do not broaden this task into retrieval quality ranking, rerank, or graph expansion.
- Do not store private indexed content in skill files, config, logs, or release notes.

## Risks / Open Questions

- Codex Desktop may require full application restart for MCP server discovery; a running session may continue without local RAG tools even after config changes.
- If Codex's tool discovery path excludes user-defined MCP servers in the current app mode, REST fallback remains necessary until that platform behavior is understood.
- `rag_get_document` should prefer `sourceId + relativePath` over opaque search result text to preserve registry enforcement.

## Status

- 2026-05-25: task 문서 생성. Current audit shows the RAG service and adapter are healthy, but the active Codex session does not expose local RAG MCP tools directly; REST fallback remains usable.
- 2026-05-25: implementation hardening completed in the development zone: source-safe `rag_get_document`, mode normalization, unknown-project 400 handling, `local-rag-system.docs` example registration, Codex skill hardening, and repeatable smoke script. Validation passed for docs validators, Maven tests, compose config, Node syntax, and MCP adapter framing. Operation-zone deploy/restart and live runtime smoke remain pending by instruction.
- 2026-05-29: user requested operation deployment under the current configurable service root; operation script defaults and install guide were updated from the earlier operation-root convention.
- 2026-05-29: operation deployment completed under `~/Services/local-rag-system`. Installed `~/Services/bin/local-rag`, synced this checkout to the operation code directory, initialized local config, started Docker, enabled host Ollama with `brew services start ollama`, pulled `qwen3-embedding:4b`, configured `qwen3.5:4b` for chat, recreated the Compose stack, installed Codex MCP/skill integration, force-scanned `local-rag-system`, and passed `local-rag codex-smoke`. Active Codex Desktop MCP visibility still requires an application restart/new-session check.
- 2026-05-29: user-managed source slots were added through generic Compose mounts and untracked local registry/config guidance, without committing machine-local host paths or private project names.
- 2026-05-29: initial user-managed sources were verified in the operation registry through live search smoke and `local-rag codex-smoke`; committed docs now keep the source identities generic for portability across machines.
