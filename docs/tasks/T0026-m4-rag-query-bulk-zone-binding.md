---
type: task
doc_id: T0026
title: m4-rag-query-bulk-zone-binding
status: done
owner: dean
created: 2026-09-04
updated: 2026-09-04
current_focus: source/test candidate 완료; external P0033 actual M4 qualification으로 handoff
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: external:P0033
related_design:
  - docs/design/msa-runtime-and-storage.md
  - docs/design/ubiquitous-language.md
source_refs:
  - external:silverstone-pad/docs/tasks/T0010-rag-query-bulk-zone-binding.md
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
  - shadow-binding
---

# T0026 M4 RAG Query/Bulk Zone Binding

- Type: task
- Document ID: T0026
- Status: done
- Completion Mode: functional
- Owner: dean
- Created: 2026-09-04
- Updated: 2026-09-04
- Current Focus: source/test candidate 완료; external P0033 actual M4 qualification으로 handoff
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: external P0033
- Related Design: `msa-runtime-and-storage`, `ubiquitous-language`

## Purpose

Local RAG retrieval과 indexer가 M4 3존 Governor의 서로 다른 authenticated RAG query/bulk binding을 사용할 수
있게 하고, 4B/2560 vector 계약과 거절 시 재개 가능한 기존 index checkpoint를 source/test에서 보존한다.

## Task Placement Check

- Local RAG의 기존 embedding client와 index publication 경계를 확장하는 bounded task다.
- 제품 전체 M4 단계는 external P0033/T0010이 소유하며 이 저장소는 RAG consumer source만 소유한다.
- 새 Local RAG project나 8B evaluation project로 분리할 이유가 없다.

## Whole-System Anchor

- `LocalRagChunk` production vectors는 `qwen3-embedding:4b`, 2560 dimension을 유지한다.
- retrieval query와 bulk indexing은 별도 request alias, bearer token file, keep-alive와 lane provenance를 가진다.
- embedding batch 전체와 dimension을 먼저 검증하고 나서 기존 chunk 교체를 시작한다.
- 거절·인증 실패·dimension mismatch는 fallback vector나 partial publication으로 바뀌지 않는다.

## Completion Mode Notes

`functional`이다. 별도 profile request를 만들고 publication guard가 실행되며 focused/full local tests가 이를
재현할 때 닫힌다. actual M4 적용이나 vector migration은 이 task의 terminal condition이 아니다.

## Committed Outcome

- query/bulk 서비스가 exact candidate alias와 token file을 독립 구성할 수 있다.
- client는 Authorization, keep_alive, exact expected dimension을 enforce한다.
- indexer는 physical model, dimension, binding/lane provenance를 저장한다.
- embedding 실패 시 기존 Weaviate chunk 삭제가 시작되지 않고 다음 scan에서 재시도할 상태가 남는다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | query/bulk authenticated client profile | alias, token file, keep-alive와 endpoint가 서비스별로 독립 구성되고 request test가 통과 |
| G2 | 4B/2560 fail-closed contract | wrong vector count/dimension과 missing/wrong token이 exception으로 종료되고 fallback/publication이 없음 |
| G3 | checkpoint-safe publication order | embedding 검증 전에 old chunk delete/upsert가 없고 failed document가 다음 scan에서 재시도 가능 |
| G4 | provenance and portable handoff | schema/property/config/docs와 full Maven tests가 exact source에 일치 |

## Scope

- common `EmbeddingClient` authenticated request profile와 tests
- indexer/retrieval settings wiring과 Compose/example env
- `LocalRagChunk` embedding provenance properties
- indexer pre-publication validation/order와 focused tests
- current design, task ledger와 verification evidence

## Out Of Scope

- actual M4/Service endpoint, token, service 또는 runtime policy 변경
- production schema/vector mutation, force scan/reindex 또는 deployment
- 8B/4096 migration과 experimental class
- chat generation binding, Voice/Trade consumer와 combined-load qualification
- Weaviate의 모든 failure 지점을 포괄하는 cross-store atomic transaction 재설계

## References

- `docs/design/control-plane.md`
- `docs/design/msa-runtime-and-storage.md`
- `docs/tasks/T0006-local-device-application-baseline.md`
- `docs/tasks/T0012-portable-ollama-endpoint-failover.md`
- external `silverstone-pad/docs/tasks/T0010-rag-query-bulk-zone-binding.md`

## Dependencies

- current `qwen3-embedding:4b`/2560 production invariant
- external T0009 M4 candidate aliases `rag-query-qwen3-v2`, `rag-bulk-qwen3-v2`
- dedicated worktree based on `origin/main@8a1b15e`

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Baseline/design/task boundary | Done | 100% | production 4B/2560와 delete-before-embed risk 확인 |
| W2 | Authenticated embedding request profiles | Done | 100% | alias/token-file/keep-alive service wiring |
| W3 | Dimension/provenance/publication guard | Done | 100% | full batch validation 뒤에만 replace |
| W4 | Focused/full tests와 documentation closeout | Done | 100% | Maven, Compose와 docs validators PASS |

## Overall Progress

- 100% — `100d10a` source candidate와 focused/full local tests를 완료했다. runtime/vector mutation은 없다.

## Completion Criteria

1. G1–G4가 모두 `Met`이고 Maven focused/full tests가 PASS한다.
2. query/bulk config는 기본 direct-Ollama compatibility를 깨지 않으면서 authenticated profile을 opt-in한다.
3. production model/dimension과 no-publication-on-mismatch invariant가 source와 tests에 고정된다.
4. actual endpoint/token/vector/deploy mutation이 없다.

## Completion Evidence

- exact branch/base/commit과 changed-file hashes
- `EmbeddingClientTests`, settings/index publication focused tests와 full `mvn test`
- Compose config render와 document validators
- external P0033/T0010 source-only evidence link

## Outputs / Handoff

- external P0033 S4에 Local RAG consumer source commit과 test result를 전달한다.
- actual M4 qualification task에는 query/bulk token mount, real endpoint preflight와 no-write shadow 절차만 남긴다.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | query latency와 bulk continuity를 함께 보호 | 별도 profile과 retry-safe order |
| GOAL | 4B/2560/auth/resume goals를 축소하지 않음 | G1–G4 verification |
| EVIDENCE | 실제 runtime을 바꾸지 않은 source 결과 | exact tests and hashes |
| SAFETY | secret과 vector corruption 방지 | token-file/no-secret fixture, dimension guard |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Met | `EmbeddingClientTests`, rendered query/bulk Compose profile | exact alias/token/keep-alive 분리 |
| G2 | Met | dimension/fallback/auth negative tests | 4B/2560 candidate는 mismatch 시 fail closed |
| G3 | Met | `IndexerPublicationGuardTests` | embedding contract failure 전에 JDBC/Weaviate interaction 0 |
| G4 | Met | `mvn -q test`, Compose and docs validators | exact source `100d10a` |

## Completion Guardrails

- actual M4에 닿지 않은 결과를 operational acceptance로 부르지 않는다.
- model/dimension 변경이나 reindex를 이 task에 포함하지 않는다.
- `done` 전 goal verification과 `./docs/bin/validate-closeout.sh`를 통과한다.

## Risks / Open Questions

- embedding 성공 뒤 Weaviate 개별 upsert 도중의 cross-store atomicity는 별도 remediation 대상이다.

## Status

- 2026-09-04: T0026 발급 및 active 전환. external P0033/T0010 S4 authority 아래 source/test-only 실행을 시작했다.
- 2026-09-04: authenticated query/bulk profiles, 4B/2560 provenance와 pre-publication guard를 구현했다. 전체 Maven tests, 기본/3존 Compose render와 document validators가 통과했으며 actual M4·Service·vector·secret mutation은 0이다.
