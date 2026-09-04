---
type: task
doc_id: T0027
title: m4-rag-embedding-production-cutover
status: active
owner: dean
created: 2026-09-05
updated: 2026-09-05
current_focus: authenticated query/bulk release와 embedding-only 운영 profile을 배포하고 실제 search/index 경계를 검증
completion_mode: integration
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: external:P0033
related_design:
  - docs/design/msa-runtime-and-storage.md
  - docs/design/local-rag-system-development-direction.md
source_refs:
  - docs/tasks/T0026-m4-rag-query-bulk-zone-binding.md
  - external:silverstone-pad/docs/projects/P0033-m4-rag-voice-trade-three-zone-stabilization-and-cutover.md
  - external:silverstone-deploy/services/m4-ollama-platform/runtime-policy.json
quality_axes:
  - WHOLE
  - GOAL
  - EVIDENCE
  - SAFETY
tags:
  - docs/task
  - local-rag-system
  - m4
  - embedding
  - production-cutover
---

# T0027 M4 RAG Embedding Production Cutover

- Type: task
- Document ID: T0027
- Status: active
- Completion Mode: integration
- Owner: dean
- Created / Updated: 2026-09-05 / 2026-09-05
- Current Focus: authenticated query/bulk release와 embedding-only 운영 profile을 배포하고 실제 search/index 경계를 검증
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: external P0033
- Related Design: `msa-runtime-and-storage`, `local-rag-system-development-direction`

## Purpose

T0026의 source/test candidate를 실제 Local RAG runtime에 연결한다. 검색용 embedding은
`silverstone/rag-query:qwen3-4b-v2`, indexing용 embedding은 `silverstone/rag-bulk:qwen3-4b-v2`와 각자
private token을 사용한다. M4 RAG zone은 embedding-only이므로 기존 `rag_answer`가 Trade의 raw Qwen3.8을
사용하던 경로를 제거하고, answer generation이 없는 profile은 검색 전에 명시적 503으로 닫는다.

## Task Placement Check

- 기존 Local RAG의 runtime configuration과 deployment를 교정하는 bounded integration task다.
- 전체 M4 release/cutover는 external P0033이, 이 문서는 Local RAG consumer와 public API failure 의미를 소유한다.
- 새 Local RAG project나 별도 generation zone을 만들지 않는다.

## Whole-System Anchor

- production vector는 계속 `qwen3-embedding:4b`, 2560 dimension이며 reindex가 필요하지 않다.
- query와 bulk는 별도 authenticated identity, alias, lane과 keep-alive를 가진다.
- M4 RAG zone은 embedding capability만 가지며 Voice/Trade generation binding을 빌려 쓰지 않는다.
- token은 source·logs·evidence에 남기지 않고 Service secret file로만 전달한다.

## Completion Mode Notes

`integration`이다. source 구현만으로 닫지 않고 exact release, runtime configuration, 실제 search/index smoke와
rollback point가 함께 확인돼야 한다.

## Committed Outcome

- Local RAG query/bulk가 raw physical model name 대신 각 authenticated alias를 호출한다.
- 4B/2560 dimension과 기존 indexed data continuity를 보존한다.
- M4 profile의 `/api/answer`는 `ANSWER_GENERATION_DISABLED`로 fail closed하고 `rag_search` 사용을 안내한다.
- 이전 immutable application release와 Service config backup으로 복귀할 수 있다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | authenticated query/bulk runtime | 두 container가 서로 다른 alias/token을 사용하고 실제 search와 bounded index probe가 성공 |
| G2 | embedding-only M4 boundary | M4로 `/api/chat`을 보내지 않으며 `/api/answer`가 검색·generation 전에 명시적 503 |
| G3 | vector continuity | production model/dimension이 4B/2560으로 유지되고 partial publication 또는 강제 reindex가 없음 |
| G4 | versioned rollback and evidence | exact app release, config backup, smoke·negative evidence와 owner handoff가 기록됨 |

## Scope

- T0026 source를 immutable Local RAG release로 승격
- Service-zone query/bulk endpoint, alias, token mount와 answer-disabled configuration
- application tests, Compose render, runtime health/search/index and negative answer smoke
- exact release/config/evidence와 rollback handoff

## Out Of Scope

- 8B embedding, dimension migration 또는 full reindex
- M4 안의 RAG answer-generation lane 신설
- Voice/Trade policy, capital/order/Venue/LIVE effect
- source content나 production vector를 파괴하는 fault test

## References

- `docs/tasks/T0026-m4-rag-query-bulk-zone-binding.md`
- `docs/design/msa-runtime-and-storage.md`
- `docs/design/local-rag-system-development-direction.md`
- external P0033 M4 three-zone project

## Dependencies

- M4 v0.15.0 transition release의 authenticated RAG query/bulk binding
- private owner-only token files on both caller and M4
- current Local RAG v1.2.1 rollback release and configuration backup

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Consumer boundary and task issuance | Done | 100% | T0026와 actual runtime drift 대조 |
| W2 | Embedding-only failure contract and release verification | In Progress | 60% | explicit 503 source/test 추가 |
| W3 | Service config, deploy and actual smoke | Todo | 0% | platform transition 뒤 실행 |
| W4 | Rollback evidence and closeout | Todo | 0% | P0033 S10–S13과 함께 기록 |

## Overall Progress

- 35% — actual runtime drift를 확인하고 owner-local cutover task와 answer-disabled source 경계를 발급했다.

## Completion Criteria

1. G1–G4가 모두 Done이고 Maven/full Compose/document validation이 통과한다.
2. 실제 `rag_search`가 authenticated query binding audit와 2560 vector 계약을 남긴다.
3. bounded index probe 또는 normal scanner가 bulk binding으로 성공하고 기존 publication을 손상하지 않는다.
4. `/api/answer`는 M4 generation을 호출하지 않고 structured 503으로 닫힌다.

## Completion Evidence

- exact Git commit/tag and deployed release
- M4 audit의 content-free binding/lane/status, container env identity와 token mode
- health/search/index/answer-negative smoke와 before/after index continuity
- Service configuration backup and rollback/reapply result

## Outputs / Handoff

- external P0033에 Local RAG exact release와 G1–G4 evidence를 전달한다.
- `rag_answer` 소비자는 `rag_search` citations를 자신의 authorized reasoning boundary에서 합성한다.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | RAG가 Voice/Trade generation 자원을 침범하지 않음 | M4 route/audit와 answer-negative smoke |
| GOAL | 인증 전환이 vector 품질·연속성을 낮추지 않음 | 4B/2560 and index continuity |
| EVIDENCE | source와 실제 runtime을 구분 | exact release and actual smoke |
| SAFETY | secret·partial publication·hidden fallback 방지 | owner-only files, negative tests, rollback |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Pending | | platform transition 뒤 실제 smoke 필요 |
| G2 | In Progress | `RetrievalService.answer` fail-closed guard | tests/runtime negative smoke 잔여 |
| G3 | Pending | T0026 source contract | actual continuity 확인 잔여 |
| G4 | Pending | | release/deploy/rollback evidence 잔여 |

## Completion Guardrails

- 기존 vector를 삭제하거나 강제 full reindex하지 않는다.
- M4 RAG zone에 chat/generation capability를 추가하지 않는다.
- 인증 실패를 raw model, Voice, Trade 또는 hosted endpoint fallback으로 바꾸지 않는다.
- secret 값과 query/content는 evidence에 기록하지 않는다.

## Risks / Open Questions

- `rag_answer`의 기존 호출자는 503을 받는다. 공개 도구 설명과 owner handoff에서 `rag_search` 대체 경로를 명시한다.

## Status

- 2026-09-05: Dean의 service project 문서 발급과 caller correction 승인에 따라 T0027을 active로 발급했다.
- 2026-09-05: deployed Local RAG가 M4 raw Qwen3.8 chat과 raw embedding을 함께 사용 중임을 확인하고, M4 RAG zone을 embedding-only로 고정했다.
