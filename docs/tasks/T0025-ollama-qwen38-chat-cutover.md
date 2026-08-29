---
type: task
doc_id: T0025
title: ollama-qwen38-chat-cutover
status: done
owner: Dean
created: 2026-08-29
updated: 2026-08-29
current_focus: Local RAG의 active/generated chat model을 native Ollama qwen3.8:latest로 전환하고 versioned deployment와 실제 answer/search로 검증했습니다.
completion_mode: operational-baseline
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - owner directive on 2026-08-29
  - /Users/dean/Service/config/local-rag-system/local.env
quality_axes:
  - WHOLE
  - GOAL
  - EVIDENCE
  - OPERABILITY
  - CONTRACT
tags:
  - docs/task
  - local-rag-system
  - ollama
  - qwen3.8
---

# T0025 ollama-qwen38-chat-cutover

- Type: task
- Document ID: T0025
- Status: done
- Completion Mode: operational-baseline
- Owner: Dean
- Created: 2026-08-29
- Updated: 2026-08-29
- Current Focus: Local RAG active/generated chat model의 native Ollama `qwen3.8:latest` versioned cutover와 운영 검증 완료
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design: docs/design/local-rag-system-development-direction.md, docs/design/msa-runtime-and-storage.md

## Purpose

oMLX endpoint는 이미 제거됐지만 active Service config와 tracked bootstrap default에 남은 구형 Qwen chat model을 native Ollama `qwen3.8:latest`로 정렬합니다. 실제 retrieval/answer 요청, health와 MCP 검색을 검증한 tagged deployment로 닫습니다.

## Task Placement Check

- 이 작업은 P0001 Local RAG baseline의 local chat-provider version replacement입니다.
- 새 제품/owner 경계가 아니라 existing Ollama endpoint와 answer seam의 bounded operational update이므로 새 Project가 필요하지 않습니다.

## Whole-System Anchor

- private source content는 operator-owned local/LAN Ollama 밖으로 나가지 않습니다.
- embedding baseline `qwen3-embedding:4b`, retrieval ranking, source registry와 index state는 변경하지 않습니다.
- oMLX compatibility env는 empty를 유지하며 active fallback으로 복원하지 않습니다.

## Completion Mode Notes

`operational-baseline`입니다. tracked defaults만 바꾸는 것으로 닫지 않고 current Service config, Compose container env, actual answer generation과 health가 같은 qwen3.8 identity를 증명해야 합니다.

## Committed Outcome

- tracked `.env.example`과 generated operation config가 `qwen3.8:latest`를 기본 chat model로 사용합니다.
- active Service config와 retrieval container가 `qwen3.8:latest`를 사용합니다.
- exact tagged release가 중앙 deploy authority로 배포되고 answer/search/health smoke가 통과합니다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | active/generated Qwen chat model을 qwen3.8로 통일 | tracked config, Service config와 container env가 일치할 때 |
| G2 | local-only provider boundary 유지 | Ollama endpoint만 사용하고 oMLX env가 empty일 때 |
| G3 | versioned operational deployment | exact tag/commit이 배포되고 health/search/answer smoke가 통과할 때 |
| G4 | release/document evidence 정렬 | task, release note와 Worknote가 실제 배포를 기록할 때 |

## Scope

- chat model defaults, Maven/Docker patch version, current runtime design와 operator template
- targeted tests, Compose validation, tagged deployment와 answer smoke
- service/repository release notes

## Out Of Scope

- embedding model, retrieval/reranker/ScoreGate policy 변경
- source reindex 또는 registry 변경
- hosted LLM fallback

## References

- docs/projects/P0001-local-rag-system.md
- docs/design/local-rag-system-development-direction.md
- docs/design/msa-runtime-and-storage.md

## Dependencies

- M4 Ollama `qwen3.8:latest` readiness
- GitHub Actions disabled
- Local RAG deploy authority와 current stack health

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Source/runtime residual audit | Done | 100% | active config qwen3.6, tracked default qwen3.5 확인 |
| W2 | Config/version/design alignment | Done | 100% | source, generated ops template, Service config와 container env가 `qwen3.8:latest`로 일치 |
| W3 | Tests and actual qwen3.8 answer smoke | Done | 100% | targeted Maven/docs/Compose PASS, answer 209 chars with 3 citations, hybrid search 3 results |
| W4 | Commit, tag, deploy and release evidence | Done | 100% | `f8a365754aed8a30963c40a25fed8b64430e3a0d`, `v1.2.1`, deploy job `20260829-211220-local-rag-system-2f8e1694` |

## Overall Progress

- 100%

## Completion Criteria

1. source/Service/container chat identity가 `qwen3.8:latest`로 일치합니다.
2. targeted Maven tests, docs validators와 Compose config가 통과합니다.
3. actual answer, search와 health가 exact deployed tag에서 통과합니다.
4. oMLX env는 empty이며 no hosted fallback 경계를 유지합니다.

## Completion Evidence

- source/config/container residual scan
- Maven test, Compose config, answer/search/health response
- commit/tag/deploy job와 release notes

## Outputs / Handoff

- Local RAG `v1.2.1` qwen3.8 chat baseline
- embedding/retrieval behavior는 기존 baseline을 유지합니다.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | answer generation이 retrieval과 같은 local-only system 경계에 남아야 함 | container/provider runtime evidence |
| GOAL | 단순 oMLX off가 아니라 Qwen 최신 baseline까지 닫아야 함 | exact model identity |
| EVIDENCE | config-only 성공 주장 방지 | actual answer smoke |
| OPERABILITY | fresh install과 current Service가 같은 default를 가져야 함 | example + operator template + runtime env |
| CONTRACT | versioned Docker artifacts와 Maven release가 일치해야 함 | `v1.2.1` build/deploy |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `.env.example`, generated ops config, `/Users/dean/Service/config/local-rag-system/local.env`, `local-rag-retrieval` container env | 모두 `qwen3.8:latest` |
| G2 | Done | container env: `RAG_OLLAMA_BASE_URL=http://10.10.10.2:11434`, `RAG_OMLX_BASE_URL=` | native Ollama only; oMLX slot empty |
| G3 | Done | product commit `f8a365754aed8a30963c40a25fed8b64430e3a0d`; tag `v1.2.1`; deploy job `20260829-211220-local-rag-system-2f8e1694` | health HTTP 200; source registry 24 projects/24 sources; answer/search smoke PASS |
| G4 | Done | `docs/releases/v1.2.1.md`; Worknote release note `2026-08-29-2118-sandbox-local-rag-system.md` | actual deployment evidence recorded |

## Completion Guardrails

- embedding model이나 retrieval policy를 함께 바꾸지 않습니다.
- actual answer smoke 없이 config-only로 닫지 않습니다.
- GitHub Actions를 생성·사용하지 않습니다.

## Risks / Open Questions

- first qwen3.8 answer smoke는 약 27초가 걸렸습니다. 정확성·인용은 통과했으며 latency는 후속 운영 관찰 대상입니다.

## Status

- 2026-08-29: final cross-service audit에서 Local RAG active chat model의 qwen3.6 잔여를 확인하고 T0025를 발급했습니다.
- 2026-08-29: targeted Maven test, document validators, Compose validation과 source/config alignment를 통과하고 product commit `f8a365754aed8a30963c40a25fed8b64430e3a0d` 및 tag `v1.2.1`을 발행했습니다.
- 2026-08-29: deploy authority job `20260829-211220-local-rag-system-2f8e1694`가 exact tag를 배포했습니다. 전체 health UP, source registry 24 projects/24 sources, `qwen3.8:latest` answer 209 chars/3 citations와 hybrid search 3 results를 확인해 task를 닫았습니다.
