---
type: task
doc_id: T0008
title: portable-ops-zone-deployment
status: done
owner:
created: 2026-05-24
updated: 2026-05-24
current_focus: "Separate portable defaults from this device's Service operation zone"
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
- Current Focus: Separate portable defaults from this device's Service operation zone
- Related Control Plane: docs/design/control-plane.md
- Related Project: docs/projects/P0001-local-rag-system.md

## Purpose

이 task는 dev zone인 `Workspace/local-rag-dev`와 operation zone인 `~/Service`를 분리하고, tracked defaults에서 이 장비 전용 direct-network endpoint를 제거한다.

## Task Placement Check

- 이 작업은 새 product가 아니라 기존 `P0001`의 deployment hardening slice다.
- T0006은 이 장비에서 기능 baseline을 증명했고, T0008은 그 runtime을 운영 zone으로 옮겨 관리 가능하게 만든다.
- 검색 품질, reranker, answer synthesis는 이 task의 범위가 아니다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 local RAG가 다른 장비에도 설치 가능한 기본값을 가지면서, 이 장비에서는 `~/Service` 운영 규칙에 따라 관리되는 것이다.

깨면 안 되는 invariant:

- committed defaults must not require `local-llm-host`.
- direct-network endpoint stays in untracked Service env only.
- operation code lives under `~/Service/code/local-rag-system`.
- operational scripts live under `~/Service/bin`.
- source docs for operational indexing come from operation-zone checkouts where available.

## Committed Outcome

- Docker defaults use `http://host.docker.internal:11434`; direct-network endpoints remain untracked local overrides.
- Compose accepts an external config mount via `LOCAL_RAG_CONFIG_DIR`.
- `ops/service/local-rag` provides the operator command.
- This device can run the stack from `~/Service/code/local-rag-system` using `~/Service/config/local-rag-system/local.env`.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Remove device endpoint hardcoding | tracked defaults use localhost or empty optional endpoint |
| G2 | Add Service operation command | `ops/service/local-rag` exists and is installed to `~/Service/bin/local-rag` |
| G3 | Move running operation to Service code zone | service runs from `~/Service/code/local-rag-system` |
| G4 | Keep this device's direct endpoint as local config | `~/Service/config/local-rag-system/local.env` contains device-only overrides |
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
| W4 | Service operation checkout | Done | 100% | `~/Service/code/local-rag-system` |
| W5 | Runtime migration | Done | 100% | stack recreated from operation zone |
| W6 | Verification | Done | 100% | service smoke passed |

## Overall Progress

- 100%

## Completion Criteria

1. `docker compose --env-file .env.example config` passes without `local-llm-host`.
2. `ops/service/local-rag` passes shell syntax check.
3. The operator script is installed at `~/Service/bin/local-rag`.
4. Operation checkout exists at `~/Service/code/local-rag-system`.
5. Running containers use the operation checkout config path.
6. Health, index status, and representative search are successful after migration.

## Completion Evidence

- `docker compose --env-file .env.example config` passed and rendered no `local-llm-host` endpoint.
- `bash -n ops/service/local-rag` passed.
- `~/Service/bin/local-rag` installed and reports operation paths under `~/Service`.
- `~/Service/code/local-rag-system` cloned from the configured private GitHub remote.
- `~/Service/code/project-beta` cloned from the project's GitHub remote for operation-zone source indexing.
- `~/Service/config/local-rag-system/local.env` stores this device's `local-llm-host` Ollama/oMLX overrides and operation source paths.
- `~/Service/config/local-rag-system/source-registry.local.yaml` registers operation-zone source roots.
- `local-rag deploy` rebuilt/recreated the stack from `~/Service/code/local-rag-system` and reinstalled Codex integration from the operation checkout.
- Post-migration force scan returned `sourcesScanned=3`, `documentsDetected=609`, `documentsIndexed=14`, `documentsDeleted=3`, `chunksIndexed=193`, `errors=[]`.
- Post-migration index status returned `documents=612`, `chunks=3432`, `documentsByStatus.indexed=609`, `documentsByStatus.removed=3`.
- Source-scoped hybrid searches returned citations for `project-alpha.docs` and `project-beta.docs`.

## Outputs / Handoff

- Operator command: `~/Service/bin/local-rag`
- Operation checkout: `~/Service/code/local-rag-system`
- Operation config: `~/Service/config/local-rag-system/local.env`
- Operation data: `~/Service/runtime/local-rag-system`

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
| G2 | Done | `ops/service/local-rag`, `~/Service/bin/local-rag` | operator command installed |
| G3 | Done | `~/Service/code/local-rag-system` | operation checkout created from GitHub |
| G4 | Done | `~/Service/config/local-rag-system/local.env` | local-only override |
| G5 | Done | health/index/search smoke | runtime verified after migration |

## Status

- 2026-05-24: Issued after clarifying that `local-llm-host` is a device-only direct network endpoint and that operation must run from `~/Service`.
- 2026-05-24: Completed migration. The running stack now uses operation checkout/config mounts under `~/Service`; tracked defaults remain portable and localhost-based.
