---
type: task
doc_id: T0027
title: configuration-contract-centralization
status: done
owner:
created: 2026-07-11
updated: 2026-07-11
current_focus: Completed
completion_mode: implementation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - docker-compose.yml
  - .env.example
  - ops/service/local-rag
  - ops/launchagents/com.localrag.ollama.plist.template
quality_axes:
  - CONTRACT
  - PORTABILITY
  - OPERABILITY
  - EVIDENCE
tags:
  - docs/task
  - configuration
  - spring-boot
  - operations
---

# T0027 configuration-contract-centralization

- Type: task
- Document ID: T0027
- Status: done
- Completion Mode: implementation
- Created: 2026-07-11
- Updated: 2026-07-11
- Current Focus: Completed
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: `docs/projects/P0001-local-rag-system.md`

## Purpose

Java constructor 내부 `System.getenv`, Compose의 이중 `RAG_*`/`LOCAL_RAG_*` namespace, 장비 경로가 포함된 운영 기본값을 제거하고 Spring Boot `ConfigurationProperties`를 중심으로 하나의 portable configuration contract를 만든다.

## Task Placement Check

- 이 작업은 P0001 runtime의 configuration contract를 정리하는 구현 작업이며 새 project가 아니다.
- `docs/design/msa-runtime-and-storage.md`의 portable operation 원칙을 구체화하므로 기존 umbrella 아래 task가 맞다.

## Whole-System Anchor

- tracked code와 template에는 특정 사용자 홈, Homebrew prefix, local service root, endpoint를 고정하지 않는다.
- Java는 environment variable을 직접 읽지 않고 Spring externalized configuration만 소비한다.
- Compose와 operator/LaunchAgent는 동일한 canonical `LOCAL_RAG_*` namespace를 사용한다.
- machine-local 값은 ignored `.env`, operation config, 또는 LaunchAgent installation-time variables에만 둔다.

## Committed Outcome

- Java 서비스는 typed Spring properties만 주입받는다.
- Compose, local env, operator가 하나의 canonical `LOCAL_RAG_*` 이름을 사용한다.
- 머신별 경로와 실행 파일은 저장소 밖 bootstrap/local env에만 존재한다.
- 샘플 설정, 문서, 테스트, 실제 runtime이 같은 계약으로 검증된다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Java configuration을 `ConfigurationProperties`로 단일화한다 | application code의 `System.getenv`가 제거되고 shared properties가 indexer/retrieval/gateway/mcp/registry에 주입된다 |
| G2 | Compose environment contract를 하나로 만든다 | container와 host가 canonical `LOCAL_RAG_*` keys와 typed duration values를 사용한다 |
| G3 | host operation template를 portable하게 만든다 | service root, Ollama binary/host, log/runtime paths가 installation-time variables로 렌더링된다 |
| G4 | migration과 runtime evidence를 제공한다 | config validation, Maven tests, validators, Compose rebuild, health/search/answer smoke가 통과한다 |

## Scope

- shared Spring Boot properties for Ollama, Weaviate, registry, and internal service endpoints
- service-specific typed properties for indexer and retrieval behavior
- canonical properties defaults file and environment variable mapping
- Compose/env/operator/LaunchAgent template migration
- focused binding/default tests and runtime smoke

## Out Of Scope

- source registry contents or project/source identity changes
- model selection changes
- retrieval ranking or ReContext behavior changes
- hosted provider fallback

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Inspect configuration drift and issue task | Done | 100% | duplicate env namespaces and host-specific defaults confirmed |
| W2 | Implement shared typed properties | Done | 100% | shared defaults and typed groups are wired across Java services |
| W3 | Align Compose and operation templates | Done | 100% | canonical env namespace and LaunchAgent renderer implemented |
| W4 | Verify and deploy | Done | 100% | tests, validators, rebuild, health, self-index, search, answer, and Codex smoke passed |

## Overall Progress

- 100%

## Completion Criteria

1. Main Java code contains no direct `System.getenv` runtime configuration reads.
2. Shared and service-specific Spring properties bind defaults and typed overrides in tests.
3. Example and live Compose configs validate with the canonical environment contract.
4. Operator and rendered LaunchAgent work from machine-local bootstrap values.
5. Maven tests, document validators, live health, indexing, search, answer, and Codex smoke pass.

## Completion Evidence

- `ConfigurationPropertiesBindingTests` covers shared defaults and duration/endpoint overrides.
- `docker compose --env-file .env.example config --quiet` and the ignored `.env` config check pass.
- `bash -n ops/service/local-rag`, LaunchAgent template `plutil`, and `git diff --check` pass.
- `mvn -q test` passed, including 48 tests and focused configuration binding coverage.
- All four document validators passed; both example and ignored live Compose configs rendered successfully.
- The rendered LaunchAgent loaded with the machine-local Ollama binary, endpoint, working directory, and log paths; `launchctl print` reported `state = running`.
- The Compose stack rebuilt under the canonical properties. Gateway health reported every dependency and both required Ollama models `UP`.
- `local-rag-system.docs` force scan completed with 71 detected documents, 9 updated documents, 1 deleted document, 141 indexed chunks, and zero errors.
- Codex smoke passed seven-tool framing, health, registry, index status, search, document fetch, and unknown-project handling. A live hybrid `/api/answer` returned model output and citations.

## Outputs / Handoff

- Code: shared `local-rag.*` typed properties and service wiring.
- Config: `local-rag-defaults.properties`, canonical `.env.example`, and Compose mapping.
- Ops: XDG-aware operator bootstrap and rendered macOS LaunchAgent template.
- Docs: runtime design, install guide, README, and this migration task.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| CONTRACT | all Java services need the same property semantics | binding tests and code search |
| PORTABILITY | tracked files must not encode one machine | template/config inspection |
| OPERABILITY | existing operation state must migrate without data loss | live rebuild and smoke |
| EVIDENCE | closeout requires more than static refactoring | tests, validators, health/search/answer |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | shared `ConfigurationProperties`, binding test, no main-code `System.getenv` | |
| G2 | Done | `.env.example` and Compose use canonical names and Spring durations | |
| G3 | Done | operator bootstrap and tokenized LaunchAgent template | machine values remain outside git |
| G4 | Done | Maven tests, validators, live rebuild, gateway `UP`, force scan, Codex smoke, hybrid answer | |

## Completion Guardrails

- Do not commit a user home, package-manager prefix, private source path, or installed service root.
- Do not introduce a second alias namespace for new runtime settings.
- Do not close on unit tests alone; migrate the live ignored configuration and verify the runtime.

## Status

- 2026-07-11: Task issued after review found that recovery code mixed Spring properties with direct environment reads and committed host-specific LaunchAgent/operator defaults.
- 2026-07-11: Shared Spring properties, canonical env names, ISO-8601 duration values, portable operator/LaunchAgent templates, and machine-local bootstrap migration completed. Live runtime and answer-path verification passed; task closed.
