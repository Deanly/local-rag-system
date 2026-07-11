---
type: task
doc_id: T0028
title: ollama-dependency-health-and-launchagent-hardening
status: done
owner:
created: 2026-06-28
updated: 2026-07-10
current_focus: Completed
completion_mode: remediation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - docs/design/control-plane.md
  - docs/design/msa-runtime-and-storage.md
  - README.md
quality_axes:
  - WHOLE
  - GOAL
  - EVIDENCE
tags:
  - docs/task
---

# T0028 ollama-dependency-health-and-launchagent-hardening

- Type: task
- Document ID: T0028
- Status: done
- Completion Mode: remediation
- Owner:
- Created: 2026-06-28
- Updated: 2026-07-10
- Current Focus: Completed
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: `docs/projects/P0001-local-rag-system.md`
- Related Design:
  - docs/design/msa-runtime-and-storage.md

## Purpose

반복 보고된 Local RAG 500 원인이 host-local Ollama 비가동 또는 필수 모델 누락일 때, 장애가 `hybrid` 검색이나 `/api/answer` 실패로만 드러나지 않도록 health surface와 macOS 자동 기동을 보강한다.

## Task Placement Check

- 이 작업은 P0001 functional baseline의 운영 신뢰성 보강이며 새 사용자-facing project가 아니다.
- runtime/storage design에 이미 Ollama를 local/LAN prerequisite으로 정의했으므로, 별도 project보다 기존 umbrella 아래 remediation task가 맞다.

## Whole-System Anchor

- Local RAG는 local 또는 LAN-local Ollama embedding/chat만 사용한다.
- hard dependency인 Ollama 장애는 gateway health에서 조기 확인 가능해야 한다.
- Compose, source registry, indexing semantics, retrieval ranking 자체는 이 task 범위에서 바꾸지 않는다.

## Completion Mode Notes

`remediation` mode다. 종료 상태는 반복 500 원인이 health에서 `DOWN`으로 드러나고, 이 장비에서 Ollama가 user LaunchAgent로 자동 기동되는 것이다.

## Committed Outcome

- `indexer-service`와 `retrieval-service` `/api/health`가 configured Ollama endpoint/model readiness를 포함한다.
- `api-gateway` `/api/health`가 downstream `DOWN`을 자신의 `DOWN`으로 전파한다.
- macOS LaunchAgent 템플릿과 이 장비 설치 상태가 준비된다.
- 테스트와 runtime smoke로 정상/장애 관측 경로를 확인한다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Ollama endpoint 또는 필수 모델 장애가 service/gateway health에서 `DOWN`으로 보인다 | unit test와 live `/api/health` smoke가 증명한다 |
| G2 | 이 장비에서 Ollama가 shell session에 종속되지 않고 자동 기동된다 | LaunchAgent가 bootstrap되고 `launchctl print`와 `/api/tags`가 증명한다 |
| G3 | 운영자가 같은 장애를 빠르게 해석할 수 있다 | README/design/task 문서가 health 의미와 운영 경계를 설명한다 |

## Scope

- Ollama `/api/tags` 기반 dependency health client 추가
- indexer/retrieval health response에 Ollama details 포함
- gateway health aggregation의 downstream status propagation
- macOS LaunchAgent template 및 local installation
- focused unit tests, Compose config validation, live runtime smoke

## Out Of Scope

- Ollama를 Docker Compose 서비스로 편입
- hosted model/provider fallback 추가
- embedding/chat 모델 교체 또는 재색인 정책 변경
- ScoreGate/reranker 품질 개선

## References

- `docs/design/control-plane.md`
- `docs/design/msa-runtime-and-storage.md`
- `README.md`

## Dependencies

- Host-local Ollama binary configured by `LOCAL_RAG_OLLAMA_BIN`
- Docker Compose runtime and existing `.env`
- Existing `qwen3-embedding:4b` embedding model and configured local chat model

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Add Ollama dependency health client and service/gateway propagation | Done | 100% | common/indexer/retrieval/gateway implementation added |
| W2 | Add focused tests and env/compose wiring | Done | 100% | Maven test pass required for closeout |
| W3 | Install macOS LaunchAgent and restart runtime | Done | 100% | LaunchAgent loaded and running; Compose stack rebuilt from the configured operation zone |
| W4 | Update docs and close evidence | Done | 100% | runtime health, force scan, search, answer, and Codex smoke evidence recorded |

## Overall Progress

- 100%

## Completion Criteria

1. `mvn test` passes.
2. `docker compose --env-file .env.example config` passes.
3. LaunchAgent is installed and loaded for the current macOS user.
4. Live gateway `/api/health` shows Ollama readiness details and reports `UP` when Ollama/models are ready.
5. Live `hybrid` search and `/api/answer` smoke return 200 after redeploy.

## Completion Evidence

- `mvn test` passed on 2026-06-28 after rerunning outside the network sandbox for JDK local-port test servers.
- `docker compose --env-file .env.example config --quiet` passed on 2026-06-28.
- `docker compose --env-file .env config --quiet` passed on 2026-06-28.
- `./docs/bin/validate-codex-readiness.sh` passed on 2026-06-28.
- `./docs/bin/validate-harness-foundation.sh` passed on 2026-06-28.
- `./docs/bin/validate-doc-retrieval.sh` passed on 2026-06-28.
- `./docs/bin/validate-closeout.sh --all` passed on 2026-06-28.
- `launchctl print gui/501/com.localrag.ollama` reported `state = running` with PID `73887` after replacing the manual shell-owned Ollama process.
- `local-rag rebuild` rebuilt and recreated the full Compose stack while preserving the configured PostgreSQL and Weaviate data directory.
- Live gateway `/api/health` reported `UP` and included Ollama endpoint readiness plus `qwen3-embedding:4b` and `qwen3.5:9b` model availability.
- `local-rag-system.docs` force scan completed with 69 documents, 1,144 chunks, zero errors; project-scoped hybrid search and answer returned citations from this repository.
- `local-rag codex-smoke` passed all seven tool, registry, index, search, document fetch, and unknown-project checks.

## Outputs / Handoff

- Code: common Ollama health client, service health propagation, gateway aggregation.
- Ops: rendered `ops/launchagents/com.localrag.ollama.plist.template`.
- Docs: README and runtime/control-plane design notes.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | Local-only RAG dependency boundary remains explicit | runtime design and health smoke |
| GOAL | 500 root cause becomes observable before user path failure | G1/G2/G3 goal verification |
| EVIDENCE | Remediation must be proven with tests and live runtime | Maven, Compose, launchctl, curl smoke |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | live gateway health includes reachable endpoint and required embedding/chat model readiness; hybrid search and answer passed | gateway reports downstream Ollama readiness before user-path failure |
| G2 | Done | `launchctl print gui/501/com.localrag.ollama` reports `state = running`; `/api/tags` returns required models | manual terminal-owned process was replaced by the user LaunchAgent |
| G3 | Done | README, runtime design, control-plane, and this task updated; validators passed | |

## Completion Guardrails

- Do not close this task with only keyword search working; hybrid and answer depend on Ollama.
- Do not treat a green Docker container as enough if Ollama dependency details are absent from health.
- Do not commit machine-local source paths or private source names as part of this remediation.

## Risks / Open Questions

- If Homebrew Ollama path differs on another Mac, operators must adapt the LaunchAgent locally.
- If `LOCAL_RAG_OLLAMA_CHAT_MODEL` names a model that is not pulled, retrieval health should intentionally report `DOWN`.

## Status

- 2026-06-28: task 문서 생성.
- 2026-06-28: Ollama dependency health propagation and LaunchAgent hardening implementation started after repeated 500 reports.
- 2026-06-28: Code tests, compose config checks, and doc validators passed. Runtime redeploy and `launchctl bootstrap` are pending because required escalated commands are blocked by current Codex approval usage limit.
- 2026-07-10: Recovery completed. LaunchAgent is loaded and running, the Compose stack was rebuilt, gateway health exposes Ollama/model readiness, `local-rag-system.docs` was restored and reindexed, and Codex/search/answer smoke passed.
