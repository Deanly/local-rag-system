---
type: task
doc_id: T0006
title: local-device-application-baseline
status: done
owner:
created: 2026-05-24
updated: 2026-05-24
current_focus: "Completed local device application baseline with qwen3-embedding:4b and actual registered source indexing"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - .env.example
  - docker-compose.yml
  - config/source-registry.local.example.yaml
quality_axes:
  - CONTRACT
  - EVIDENCE
  - SECURITY
tags:
  - docs/task
  - local-rag-system
  - deployment-baseline
---

# T0006 local-device-application-baseline

- Type: task
- Document ID: T0006
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-24
- Current Focus: Completed local device application baseline with qwen3-embedding:4b and actual registered source indexing
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/msa-runtime-and-storage.md`
  - `docs/design/source-registry-and-project-ssot.md`

## Purpose

이 task는 `local-rag-system`을 이 장비의 실제 registered source folders에 적용 가능한 상태로 고정한다.

결정된 production embedding baseline은 `qwen3-embedding:4b` on Ollama `local-llm-host:11434`이며, fallback embedding은 꺼진다. 답변 생성 후보는 oMLX `qwen3.6-35b-a3b-oq4-fp16-mtp`지만, 현재 구현 범위는 retrieval/indexing baseline까지다.

## Task Placement Check

- 이 작업은 `P0001` functional baseline의 실제 local device application slice이므로 새 project가 아니라 기존 umbrella 아래 task가 맞다.
- model selection과 deployment config가 runtime contract에 영향을 주므로 task 기록을 남긴다.
- answer synthesis API, reranker, UI는 이 task의 완료 조건이 아니다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 "등록된 source roots를 read-only로 indexing하고, 실제 semantic embedding으로 Weaviate hybrid retrieval을 제공한다"는 local-only RAG 운영 경계다.

깨면 안 되는 invariant:

- fallback embeddings are disabled for production indexing.
- source folders are read-only mounts.
- registry source ids determine retrieval boundary.
- personal-notes compiled wiki remains support context, not project-current-truth.
- model selection must not require keeping multiple large chat models resident.

## Committed Outcome

이 task가 `done`일 때 가능해야 하는 것:

- tracked defaults are pinned to `qwen3-embedding:4b`.
- ignored local `.env` and `config/source-registry.local.yaml` are present for this machine.
- Docker Compose validates with local source paths.
- stack starts with fallback disabled.
- actual registered sources are indexed without embedding errors.
- representative searches return citation-bearing results from each source class.
- verification and SSH memory observations are recorded in status.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Pin production embedding model | Defaults and local env use `qwen3-embedding:4b` with fallback disabled |
| G2 | Create machine-local config | ignored `.env` and `source-registry.local.yaml` point to actual local source roots |
| G3 | Run actual indexing | force scan indexes registered target sources without errors |
| G4 | Verify retrieval | representative searches return citations across project docs and compiled wiki |
| G5 | Capture evidence | tests, validators, compose config, and SSH monitoring are reported |

## Scope

- model default update
- local ignored config creation
- compose config validation
- batch embedding for file chunk indexing
- full local source indexing run
- smoke searches and index status
- SSH-based model/memory observation
- tracked docs and commit

## Out Of Scope

- true answer synthesis API
- reranker and retrieval evaluation suite
- UI
- remote service reconfiguration outside Ollama/oMLX observation

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Pin defaults | Done | 100% | tracked config uses `qwen3-embedding:4b`; fallback disabled by default |
| W2 | Create local config | Done | 100% | ignored `.env` and `config/source-registry.local.yaml` created for this machine |
| W3 | Batch embedding path | Done | 100% | file chunk indexing uses Ollama `/api/embed` batch input with sequential compatibility fallback |
| W4 | Validate and deploy stack | Done | 100% | compose config validated; stack recreated with fallback disabled |
| W5 | Full indexing run | Done | 100% | actual registered sources indexed |
| W6 | Search verification | Done | 100% | project-scoped and source-scoped hybrid searches returned citations |
| W7 | Commit and closeout | Done | 100% | validators passed and changes prepared for commit |

## Overall Progress

- 100%

## Completion Criteria

1. `docker compose --env-file .env config` passes.
2. Maven tests and docs validators pass.
3. `POST /api/index/force` indexes actual registered sources with no errors.
4. `GET /api/index/status` reports indexed documents/chunks.
5. `POST /api/search` returns citations for `project-alpha`, `project-beta`, and `personal-notes`.
6. Remote Ollama `api/ps` and SSH memory snapshots show the selected embedding model behavior.

## Completion Evidence

- `docker compose --env-file .env config` passed with real local source mounts.
- `docker compose --env-file .env up -d --build --force-recreate` started the stack with fallback disabled.
- `POST /api/index/force` completed on 2026-05-24 with `sourcesScanned=3`, `documentsDetected=612`, `documentsIndexed=612`, `documentsDeleted=0`, `chunksIndexed=3463`, `errors=[]`.
- `GET /api/index/status` returned `documents=612`, `chunks=3463`, `documentsByStatus.indexed=612`.
- Source counts after indexing: `personal-notes=147`, `project-alpha.docs=332`, `project-beta.docs=133`.
- `POST /api/search` returned citation-bearing hybrid results for `project-alpha`, `project-beta`, and `personal-notes`.
- Source-scoped searches returned citations from each source:
  - `project-alpha.docs`: `projects/P0011-general-lobby-teacher-channel-delivery.md#Requirement Traceability Matrix`
  - `project-beta.docs`: `tasks/T0218-paper-trading-pre-promotion-gate.md#Task Placement Check`
  - `personal-notes`: `queries/rag-ssot-strategy.md`
- `POST /api/mcp/rag_search` returned a citation-bearing response through the REST bridge.
- SSH monitoring on `local-llm-user@local-llm-host` showed only `qwen3-embedding:4b` resident in Ollama for indexing, with `size_vram=11653994016` and `Pages throttled=0`.
- Service logs after the final verification window had no `error`, `exception`, `failed`, or `warn` entries.

## Outputs / Handoff

- Local device runtime baseline for actual registered source folders.
- Model decision: `qwen3-embedding:4b` for embeddings, oMLX Qwen3.6 MTP as answer-generation candidate.
- Follow-up: retrieval quality eval and optional answer synthesis service.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| CONTRACT | model/env/source ids become the local deployment contract | config and compose validation |
| EVIDENCE | actual indexing must be observed, not assumed | force scan and search smoke |
| SECURITY | local source roots remain read-only and scoped | registry and compose evidence |

## Completion Guardrails

- Do not enable fallback embeddings for the production index.
- Do not index unregistered folders.
- Do not keep multiple large chat models resident as part of this task.
- Do not treat sample-only smoke as actual local application evidence.

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `.env.example`, `docker-compose.yml`, service defaults | `qwen3-embedding:4b`; fallback disabled |
| G2 | Done | ignored `.env`, `config/source-registry.local.yaml` | machine-local files are not tracked |
| G3 | Done | force scan indexed 612 documents and 3463 chunks with `errors=[]` | real registered sources |
| G4 | Done | hybrid searches returned citations from `project-alpha.docs`, `project-beta.docs`, and `personal-notes` | project and source scope verified |
| G5 | Done | Maven tests, docs validators, compose config, SSH monitoring, service log check | closeout evidence captured |

## Status

- 2026-05-24: task issued after selecting `qwen3-embedding:4b` as the performance/memory balanced embedding baseline.
- 2026-05-24: completed local device application. Indexed 612 documents and 3463 chunks from `personal-notes`, `project-alpha.docs`, and `project-beta.docs` using `qwen3-embedding:4b` with fallback disabled. Representative hybrid and MCP bridge searches returned citation-bearing results.
