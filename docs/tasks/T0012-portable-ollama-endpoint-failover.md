---
type: task
doc_id: T0012
title: portable-ollama-endpoint-failover
status: done
owner:
created: 2026-05-29
updated: 2026-05-29
current_focus: "Completed ordered local/LAN Ollama endpoint failover for notebook-local and Mac mini profiles"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - README.md
  - docker-compose.yml
  - ops/service/local-rag
quality_axes:
  - WHOLE
  - HANDOFF
  - SECURITY
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - ollama
  - portability
---

# T0012 portable-ollama-endpoint-failover

- Type: task
- Document ID: T0012
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-05-29
- Updated: 2026-05-29
- Current Focus: Completed ordered local/LAN Ollama endpoint failover for notebook-local and Mac mini profiles
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 Local RAG runtime이 노트북-local Ollama를 기본으로 유지하면서, 사용자가 원할 때 같은 로컬망의 Mac mini M4 Pro Ollama도 사용할 수 있게 한다.

## Task Placement Check

- 이 작업은 새 product가 아니라 기존 `P0001` runtime의 LLM endpoint portability 보강이다.
- source registry, indexing, retrieval, answer API는 그대로 두고 Ollama endpoint 선택 계약만 확장한다.
- 별도 `project`를 발급하지 않는 이유는 기존 Local RAG umbrella의 local-only model invocation invariant를 보강하는 단일 기능 slice이기 때문이다.

## Whole-System Anchor

이 task가 보존하는 전체 목표는 private source content를 hosted LLM API로 보내지 않고, operator가 소유하거나 관리하는 notebook-local/LAN-local Ollama endpoint 안에서만 embedding/chat 호출을 수행하는 것이다.

깨면 안 되는 invariant:

- 등록 source root 밖의 파일을 indexing/search하지 않는다.
- fallback은 hosted provider나 외부 API가 아니라 명시된 local/LAN Ollama endpoint 목록 안에서만 일어난다.
- 실제 Mac mini hostname, IP, host path는 committed default가 아니라 local env에 둔다.
- embedding fallback은 production indexing에서 꺼진 상태를 유지한다.

## Completion Mode Notes

Completion mode는 `functional`이다. 닫힌 상태에서는 단일 Ollama URL 기존 설정이 계속 동작하고, optional ordered endpoint list가 indexer/retrieval answer path에서 검증되어야 한다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- `LOCAL_RAG_OLLAMA_BASE_URL` 단일 설정이 기존처럼 동작한다.
- `LOCAL_RAG_OLLAMA_BASE_URLS`가 설정되면 indexer embedding, retrieval query embedding, answer chat이 comma-separated endpoint를 순서대로 시도한다.
- Mac mini 우선 + notebook fallback, notebook 우선 + Mac mini fallback 운영 예시가 README와 install guide에 있다.
- unreachable endpoint가 오래 붙잡지 않도록 connection/read timeout이 local env로 조정 가능하다.
- focused tests and compose config verification pass.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Ordered local Ollama endpoints are implemented without breaking single endpoint compatibility. | common clients and Spring settings support both `LOCAL_RAG_OLLAMA_BASE_URL` and `LOCAL_RAG_OLLAMA_BASE_URLS`. |
| G2 | Notebook portability and Mac mini usage are operator-readable. | README/install/runtime design explain local-first and Mac mini-first profiles without committing real endpoint values. |
| G3 | Local-only security boundary is preserved. | docs say fallback remains local/LAN Ollama only and fallback embeddings remain disabled by default. |
| G4 | The change is verified. | Maven tests, Compose config, and docs validators pass or skips are explained. |

## Scope

- common Ollama embedding/chat client endpoint failover
- indexer/retrieval settings and Compose env propagation
- operation-zone env template defaults
- README, new-machine guide, and runtime design updates
- focused tests and standard validators

## Out Of Scope

- automatically discovering Mac mini hostnames
- synchronizing model installation across machines
- changing embedding model or vector dimensions
- adding hosted LLM provider fallback
- restarting the user's operation-zone stack

## References

- `docs/projects/P0001-local-rag-system.md`
- `docs/design/control-plane.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/msa-runtime-and-storage.md`
- `README.md`
- `docker-compose.yml`
- `ops/service/local-rag`

## Dependencies

- Existing T0009 host-local Ollama answer runtime.
- Operator-managed Ollama model installation on whichever endpoint is used.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Implement endpoint list support | Done | 100% | common clients and service settings support ordered endpoint lists |
| W2 | Update operator config/docs | Done | 100% | README, guide, design, Compose, and operation env updated |
| W3 | Verify and close | Done | 100% | Maven tests, Compose config, and diff check passed |

## Overall Progress

- 100%

## Completion Criteria

1. Common embedding/chat clients try configured local Ollama endpoints in order.
2. Indexer and retrieval services receive `LOCAL_RAG_OLLAMA_BASE_URLS` and timeout env settings through Compose.
3. Operator docs describe notebook-local and Mac mini profiles.
4. `mvn test`, `docker compose --env-file .env.example config`, and docs validators pass.

## Completion Evidence

- `mvn -q test` passed.
- `docker compose --env-file .env.example config` passed and showed `RAG_OLLAMA_BASE_URLS`, `RAG_OLLAMA_CONNECT_TIMEOUT_MILLIS`, and `RAG_OLLAMA_READ_TIMEOUT_MILLIS` on indexer and retrieval services.
- `git diff --check` passed.
- Focused common client tests cover single embedding failover, batch embedding failover, and chat failover from an unavailable primary endpoint.
- `./docs/bin/validate-codex-readiness.sh`, `./docs/bin/validate-harness-foundation.sh`, `./docs/bin/validate-doc-retrieval.sh`, and `./docs/bin/validate-closeout.sh --all` passed.

## Outputs / Handoff

- Runtime config: `LOCAL_RAG_OLLAMA_BASE_URLS`, `LOCAL_RAG_OLLAMA_CONNECT_TIMEOUT_MILLIS`, `LOCAL_RAG_OLLAMA_READ_TIMEOUT_MILLIS`.
- Operator handoff: put real Mac mini hostname/IP only in `.env` or operation-zone `local.env`.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | endpoint fallback must serve the same Local RAG indexing/retrieval/answer paths | common clients and service settings |
| HANDOFF | user needs a clear portable profile for desk and travel use | README/install guide examples |
| SECURITY | private snippets must stay within local/LAN Ollama endpoints | docs and default env keep hosted fallback absent |
| EVIDENCE | endpoint behavior must be tested, not only documented | Maven tests and Compose config |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `EmbeddingClientTests`, `OllamaChatClientTests`, service settings wiring, and `mvn -q test` | single URL compatibility is preserved by falling back to `RAG_OLLAMA_BASE_URL` |
| G2 | Done | README and install guide show Mac mini-first and notebook-first ordered endpoint profiles | real hostnames remain local env values |
| G3 | Done | control-plane, runtime design, local-rag design, and SECURITY state local/LAN-only endpoint fallback | hosted fallback was not added |
| G4 | Done | `mvn -q test`, `docker compose --env-file .env.example config`, docs validators, and `git diff --check` passed | operation-zone runtime was not restarted |

## Completion Guardrails

- Do not commit real Mac mini hostname, IP, or private LAN details.
- Do not turn on fallback embeddings for production evidence.
- Do not add hosted API fallback.
- Do not claim operation-zone runtime has been restarted unless that command is explicitly run.

## Risks / Open Questions

- If the Mac mini endpoint uses a different embedding model or vector dimension than the notebook, existing indexed vectors may need a force reindex.

## Status

- 2026-05-29: task 문서 생성.
- 2026-05-29: Implemented ordered local/LAN Ollama endpoint failover in common embedding/chat clients, wired indexer/retrieval settings and Compose env, documented notebook-local and Mac mini profiles, and verified Maven tests plus Compose config.
