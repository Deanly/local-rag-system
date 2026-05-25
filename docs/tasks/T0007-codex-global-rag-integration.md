---
type: task
doc_id: T0007
title: codex-global-rag-integration
status: done
owner:
created: 2026-05-24
updated: 2026-05-25
current_focus: "Installable Codex global MCP and skill integration; active utilization hardening moved to T0010"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - integrations/codex/README.md
  - integrations/codex/local-rag-mcp-server.mjs
  - integrations/codex/install-codex-local-rag.sh
quality_axes:
  - CONTRACT
  - HANDOFF
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - codex
  - mcp
---

# T0007 codex-global-rag-integration

- Type: task
- Document ID: T0007
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-25
- Current Focus: Installable Codex global MCP and skill integration; active utilization hardening moved to T0010
- Related Control Plane: docs/design/control-plane.md
- Related Project: docs/projects/P0001-local-rag-system.md

## Purpose

이 task는 `local-rag-system`을 Codex가 전역에서 사용할 수 있도록 설치 가능한 integration surface로 고정한다.

## Task Placement Check

- 이 작업은 새 RAG 제품이 아니라 기존 `P0001`의 Codex integration slice다.
- 검색/indexing runtime은 이미 T0006에서 닫혔고, 이 task는 Codex가 그 runtime을 호출할 수 있게 하는 adapter와 설치 계약만 다룬다.
- true document fetch, answer synthesis, reranker는 별도 후속 task가 맞다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 Codex가 local-only RAG index를 전역 tool처럼 호출하되, source registry와 기존 gateway boundary를 우회하지 않는 것이다.

깨면 안 되는 invariant:

- Codex integration은 registered source registry를 우회해 직접 파일을 긁지 않는다.
- MCP adapter는 indexing/search logic을 갖지 않고 gateway REST bridge만 호출한다.
- global install은 `~/.codex/config.toml`과 `~/.codex/skills/local-rag`만 변경한다.
- 재설치는 idempotent해야 하며 기존 `local_rag` block만 교체한다.
- Codex restart 없이는 새 global MCP/skill discovery가 보장되지 않는다.

## Committed Outcome

- Project-owned installer exists under `integrations/codex/`.
- A dependency-light stdio MCP adapter forwards Codex tool calls to the local REST bridge.
- A global `local-rag` Codex skill defines when and how agents should query local RAG.
- This machine's `~/.codex/config.toml` is updated with `mcp_servers.local_rag`.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Package Codex integration inside the project | installer, MCP adapter, skill, and README live under `integrations/codex/` |
| G2 | Provide true Codex MCP surface | stdio MCP adapter exposes implemented RAG tools |
| G3 | Install global Codex usage on this device | `~/.codex/config.toml` and `~/.codex/skills/local-rag` are updated |
| G4 | Verify the integration path | adapter smoke and gateway REST smoke pass |

## Scope

- MCP stdio adapter
- Codex skill template
- idempotent install/uninstall script
- README and project/task documentation
- global install on this device

## Out Of Scope

- Replacing the Spring Boot REST bridge
- true document fetch tool
- remote deployment or shared multi-user auth

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | MCP stdio adapter | Done | 100% | Node standard library only |
| W2 | Codex skill | Done | 100% | installed under `~/.codex/skills/local-rag` |
| W3 | Installer | Done | 100% | updates `~/.codex/config.toml` idempotently |
| W4 | Global install | Done | 100% | `local_rag` server registered |
| W5 | Verification | Done | 100% | adapter and REST smoke pass |

## Overall Progress

- 100%

## Completion Criteria

1. `integrations/codex/install-codex-local-rag.sh` can install and uninstall the integration.
2. `integrations/codex/local-rag-mcp-server.mjs` passes Node syntax check.
3. `~/.codex/config.toml` contains a single `[mcp_servers.local_rag]` registration after install.
4. `~/.codex/skills/local-rag/SKILL.md` exists after install.
5. A representative MCP adapter call can reach the local RAG gateway.

## Completion Evidence

- `integrations/codex/install-codex-local-rag.sh` installs and uninstalls global config.
- `integrations/codex/local-rag-mcp-server.mjs` exposes `rag_search`, `rag_answer`, `rag_list_projects`, `rag_list_sources`, `rag_index_status`, and `rag_force_scan`.
- `integrations/codex/skill/SKILL.md` defines registry-driven local RAG use rules.
- `~/.codex/config.toml` contains `[mcp_servers.local_rag]`.
- 2026-05-25 refresh installed `[mcp_servers.local_rag]` with a machine-local default project id and exactly one config block.
- `~/.codex/skills/local-rag/SKILL.md` exists after install.

## Outputs / Handoff

- Operator install entrypoint: `integrations/codex/install-codex-local-rag.sh`
- Codex global skill source: `integrations/codex/skill/SKILL.md`
- MCP adapter source: `integrations/codex/local-rag-mcp-server.mjs`
- Follow-up: implement source-safe `rag_get_document` after retrieval service has a document fetch contract.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| CONTRACT | Codex config and MCP tool schemas become the integration contract | installer and adapter smoke |
| HANDOFF | Future installs should not depend on this session's manual edits | project-owned README and installer |
| EVIDENCE | Global setup must be observed, not just documented | config/skill checks and smoke |

## Completion Guardrails

- Do not expose a fake `rag_get_document` tool until the backend supports source-safe fetch.
- Do not add package dependencies for the adapter unless needed.
- Do not overwrite unrelated Codex config sections.
- Do not store private indexed content inside the skill or config.

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `integrations/codex/README.md` | operator install contract |
| G2 | Done | `local-rag-mcp-server.mjs` | true stdio MCP adapter over existing REST bridge |
| G3 | Done | `install-codex-local-rag.sh` | global Codex config and skill install |
| G4 | Done | smoke tests | local gateway and adapter calls verified |

## Status

- 2026-05-24: Created installable Codex integration and installed it globally on this device. Codex restart is required for new global MCP/skill discovery.
- 2026-05-25: Refreshed the global integration for registry-driven local sources, added `rag_answer`, and kept the default project id as machine-local install state.
- 2026-05-25: Post-install audit confirmed the runtime and stdio adapter are healthy, but active-session tool visibility and API/tool contract gaps require follow-up. Remediation moved to `T0010-codex-rag-utilization-hardening.md`.
