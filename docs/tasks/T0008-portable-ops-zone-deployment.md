---
type: task
doc_id: T0008
title: portable-ops-zone-deployment
status: done
owner:
created: 2026-05-24
updated: 2026-05-24
current_focus: "Separate portable defaults from operator-local operation-zone config"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/msa-runtime-and-storage.md
  - docs/design/local-rag-system-development-direction.md
source_refs:
  - .env.example
  - docker-compose.yml
  - ops/service/local-rag
quality_axes:
  - CONTRACT
  - HANDOFF
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - operations
---

# T0008 portable-ops-zone-deployment

- Type: task
- Document ID: T0008
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-24
- Current Focus: Separate portable defaults from operator-local operation-zone config
- Related Control Plane: docs/design/control-plane.md
- Related Project: docs/projects/P0001-local-rag-system.md

## Purpose

이 task는 development checkout과 operator-local operation zone을 분리하고, tracked defaults에서 장비 전용 direct-network endpoint를 제거한다. Current install docs use the configurable `ops/service/local-rag` command; older operation roots from this historical task are not portable instructions.

## Task Placement Check

- 이 작업은 새 product가 아니라 기존 `P0001`의 deployment hardening slice다.
- T0006은 이 장비에서 기능 baseline을 증명했고, T0008은 그 runtime을 운영 zone으로 옮겨 관리 가능하게 만든다.
- 검색 품질, reranker, answer synthesis는 이 task의 범위가 아니다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 local RAG가 다른 장비에도 설치 가능한 기본값을 가지면서, 운영 장비별 env/registry는 ignored local config로 관리되는 것이다.

깨면 안 되는 invariant:

- committed defaults must not require a device-specific LLM endpoint.
- direct-network endpoint stays in untracked operation env only.
- operation code lives under the configured `LOCAL_RAG_SERVICE_ROOT`.
- operational scripts live under the configured operation bin directory.
- source docs for operational indexing come from registered local sources only.

## Committed Outcome

- Docker defaults use `http://host.docker.internal:11434`; direct-network endpoints remain untracked local overrides.
- Compose accepts an external config mount via `LOCAL_RAG_CONFIG_DIR`.
- `ops/service/local-rag` provides the operator command.
- The operator can run the stack from the configured operation checkout using an ignored operation env file.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Remove device endpoint hardcoding | tracked defaults use localhost or empty optional endpoint |
| G2 | Add operation command | `ops/service/local-rag` exists and can be installed under the configured operation bin directory |
| G3 | Move running operation to the operation code zone | service runs from the configured operation checkout |
| G4 | Keep device-specific endpoints as local config | ignored operation env contains device-only overrides |
| G5 | Verify runtime after migration | health, registry, index status, and search smoke pass |

## Scope

- tracked default cleanup
- external config mount support
- operation script
- Service code checkout and config
- running stack migration from dev checkout to operation checkout
- Codex MCP adapter reinstall from operation checkout

## Out Of Scope

- changing indexed source semantics
- changing embedding model selection
- managing remote CI providers
- mutating `project-alpha` or `project-beta` code

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Portable defaults | Done | 100% | Ollama default is localhost |
| W2 | External config support | Done | 100% | `LOCAL_RAG_CONFIG_DIR` mount |
| W3 | Operator script | Done | 100% | `ops/service/local-rag` |
| W4 | Operation checkout | Done | 100% | checkout path is configured by operation env |
| W5 | Runtime migration | Done | 100% | stack recreated from operation zone |
| W6 | Verification | Done | 100% | service smoke passed |

## Overall Progress

- 100%

## Completion Criteria

1. `docker compose --env-file .env.example config` passes without a device-specific LLM endpoint.
2. `ops/service/local-rag` passes shell syntax check.
3. The operator script is installed under the configured operation bin directory.
4. Operation checkout exists under the configured operation code directory.
5. Running containers use the operation checkout config path.
6. Health, index status, and representative search are successful after migration.

## Completion Evidence

- `docker compose --env-file .env.example config` passed and rendered no device-specific LLM endpoint.
- `bash -n ops/service/local-rag` passed.
- The operator command installed and reported operation paths under the configured service root.
- The operation checkout was created from the configured repository remote.
- Device-specific Ollama and source path overrides were stored only in the ignored operation env.
- The ignored operation registry registered operation-zone source roots.
- `local-rag deploy` rebuilt/recreated the stack from the operation checkout and reinstalled Codex integration.
- Post-migration force scan returned `sourcesScanned=3`, `documentsDetected=609`, `documentsIndexed=14`, `documentsDeleted=3`, `chunksIndexed=193`, `errors=[]`.
- Post-migration index status returned `documents=612`, `chunks=3432`, `documentsByStatus.indexed=609`, `documentsByStatus.removed=3`.
- Source-scoped hybrid searches returned citations for the registered project-docs source classes.

## Outputs / Handoff

- Operator command: `ops/service/local-rag`, installed under the configured operation bin directory.
- Operation checkout: configured by `LOCAL_RAG_CODE_DIR`.
- Operation config: configured by `LOCAL_RAG_CONFIG_DIR` and `LOCAL_RAG_ENV_FILE`.
- Operation data: configured by `LOCAL_RAG_DATA_DIR`.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| CONTRACT | Portable defaults and operation env define install behavior | config validation |
| HANDOFF | Operator can deploy/pull/status without remembering compose details | `local-rag` command |
| EVIDENCE | Migration must be proven by running service checks | health/index/search smoke |

## Completion Guardrails

- Do not commit the operation env file.
- Do not delete existing runtime data.
- Do not point operation config at dev-zone project docs when operation-zone checkout exists.
- Do not mutate unrelated Service projects.

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `.env.example`, `docker-compose.yml`, Java settings | device endpoint removed from defaults |
| G2 | Done | `ops/service/local-rag` and installed command | operator command installed |
| G3 | Done | configured operation checkout | operation checkout created from Git remote |
| G4 | Done | ignored operation env | local-only override |
| G5 | Done | health/index/search smoke | runtime verified after migration |

## Status

- 2026-05-24: Issued after clarifying that direct network endpoints are device-only and operation must run from an ignored local operation zone.
- 2026-05-24: Completed migration. The running stack used operation checkout/config mounts under the configured service root; tracked defaults remained portable and localhost-based.
