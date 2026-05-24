---
type: task
doc_id: T0005
title: multi-source-application-hardening
status: done
owner:
created: 2026-05-24
updated: 2026-05-24
current_focus: "Completed multi-source local application hardening for Personal Notes, Project Alpha, and Project Beta docs"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - docker-compose.yml
  - config/source-registry.local.example.yaml
  - services/indexer-service/src/main/java/com/localrag/indexer/SourcePathFilter.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
quality_axes:
  - CONTRACT
  - SECURITY
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - multi-source
---

# T0005 multi-source-application-hardening

- Type: task
- Document ID: T0005
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-24
- Current Focus: Completed multi-source local application hardening for Personal Notes, Project Alpha, and Project Beta docs
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/msa-runtime-and-storage.md`
  - `docs/design/source-registry-and-project-ssot.md`

## Purpose

이 task는 Khoj 제거 후 `local-rag-system`을 실제 로컬 문서 소스에 적용하기 전 필요한 안전 보완을 닫는다.

대상은 `personal-notes`의 compiled wiki layer, `project-alpha/docs`, `project-beta/docs`이며, 이번 작업은 배포가 아니라 코드/설정/문서 기준선을 정리하고 커밋 가능한 상태로 만드는 것이다.

## Task Placement Check

- 이 작업은 `P0001` functional baseline의 후속 hardening slice이므로 새 umbrella project가 아니라 `P0001` 아래 task가 맞다.
- 배포나 운영 인덱싱이 아니라 local source 적용 전 contract 보강이므로 기존 runtime/design boundary를 유지한다.
- retrieval quality, rerank, parser 확장은 이 task의 목적을 넘어서므로 별도 후속 task 후보로 둔다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 "등록된 local source folder만 read-only로 다루고, project docs를 primary truth로 유지하며, compiled wiki를 명시적 `default_context`일 때만 보조 source로 검색한다"는 경계다.

깨면 안 되는 invariant:

- unregistered folder는 indexing/search 대상이 아니다.
- source mount는 read-only다.
- registry의 include/exclude glob은 scanner에서 실제로 강제된다.
- cross-project support source는 source id filter로 검색되며 primary project truth로 오인되지 않는다.
- machine-local registry는 local config로 다루고 개인 경로를 필수 tracked truth로 만들지 않는다.

## Committed Outcome

이 task가 `done`일 때 가능해야 하는 것:

- Compose가 세 source root를 별도 read-only mount로 표현한다.
- local registry example이 Personal Notes wiki, Project Alpha docs, Project Beta docs를 등록한다.
- scanner가 include/exclude glob을 통과한 supported file만 indexing한다.
- retrieval이 `primary_source_id`, `default_context`, project-local active sources 순서로 source scope를 만든다.
- registry에서 사라진 project/source는 PostgreSQL에서 inactive가 된다.
- 배포 없이 Maven, Compose config, docs validators가 통과한다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Multi-source Compose와 registry contract를 적용한다. | 세 read-only mount와 local registry example이 존재한다. |
| G2 | Source filtering을 metadata가 아니라 실행 contract로 만든다. | scanner가 include/exclude glob을 실제 파일 방문 전에 적용한다. |
| G3 | `default_context` retrieval semantics를 구현한다. | project search가 cross-project support source ids를 포함할 수 있다. |
| G4 | Registry retirement drift를 줄인다. | registry에서 빠진 project/source row가 inactive 처리된다. |
| G5 | Commit 가능한 baseline evidence를 남긴다. | Maven, Compose config, docs validators가 통과한다. |

## Scope

- Docker Compose 다중 source read-only mount
- machine-local registry 예시
- scanner include/exclude glob 강제 적용
- `default_context` 기반 cross-project retrieval source resolution
- registry에서 사라진 project/source 비활성화
- Maven, Compose config, document harness validation

## Out Of Scope

- 실제 운영 인덱싱 배포
- Ollama model pull 또는 model server 변경
- PDF/OCR/canvas parser 확장
- retrieval reranker/evaluation set

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Multi-source Compose contract | Done | 100% | three read-only source mounts |
| W2 | Local registry example | Done | 100% | personal-notes wiki, project-alpha docs, project-beta docs |
| W3 | Source glob enforcement | Done | 100% | include/exclude filters run before indexing |
| W4 | Default context retrieval | Done | 100% | cross-project support source ids are resolved before query |
| W5 | Registry retirement behavior | Done | 100% | missing project/source rows become inactive |
| W6 | Verification | Done | 100% | tests and validators passed |

## Completion Criteria

1. `.env.example` and Compose render a valid multi-source configuration.
2. `config/source-registry.local.example.yaml` registers the three target source ids.
3. `SourcePathFilter` is tested for root-level `**/*.md` includes and root/nested directory excludes.
4. Retrieval source resolution honors active primary/default-context/project sources.
5. Registry synchronizer deactivates missing rows.
6. Verification commands pass without deployment.

## Completion Evidence

- `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -q test`
- `docker compose --env-file .env.example config`
- `./docs/bin/validate-codex-readiness.sh`
- `./docs/bin/validate-harness-foundation.sh`
- `./docs/bin/validate-doc-retrieval.sh`
- `git diff --check -- .`

## Outputs / Handoff

- Multi-source Compose source mount contract.
- `config/source-registry.local.example.yaml` as the target local registry example.
- Scanner glob enforcement and focused tests.
- Retrieval source scope resolution aligned with `default_context`.
- Next operator step: install/confirm the Ollama embedding model on `local-llm-host`, then run an actual local indexing smoke with fallback disabled.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| CONTRACT | registry fields, compose env vars, and retrieval source semantics are integration contracts. | README/design updates and compose config |
| SECURITY | private local source roots must not be over-indexed by accident. | read-only mounts and enforced exclude globs |
| EVIDENCE | source filtering and build health must be mechanically checked. | Maven tests, compose config, validators |

## Completion Guardrails

- Do not start a new deployment or force-index the real target folders in this task.
- Do not commit machine-local private registry files; commit examples and ignore local overrides.
- Do not claim production retrieval quality until a real embedding model is installed and evaluated.
- Do not treat compiled wiki support sources as project-current-truth.

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `docker-compose.yml`, `.env.example`, `config/source-registry.local.example.yaml` | Compose config validation passed |
| G2 | Done | `SourcePathFilter` and `SourcePathFilterTests` | root and nested include/exclude cases covered |
| G3 | Done | `RetrievalService.resolveSources` | source id filtering allows `default_context` cross-project support |
| G4 | Done | `RegistrySynchronizer` missing-row inactive updates | applies on registry synchronization |
| G5 | Done | verification commands in Completion Evidence | no deployment performed |

## Status

- 2026-05-24: task issued and completed. Multi-source application hardening is ready for a later local deployment pass after the operator confirms the Ollama embedding model.
