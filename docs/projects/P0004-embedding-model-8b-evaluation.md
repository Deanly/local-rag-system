---
type: project
doc_id: P0004
title: embedding-model-8b-evaluation
status: active
project_role: exception-branch
umbrella_initiative: embedding-model-8b-evaluation
parent_umbrella_project: P0001-local-rag-system
completion_mode: decision-lock
owner: Dean
created: 2026-06-17
updated: 2026-06-17
current_focus: "Gate whether qwen3-embedding:8b should replace operational 4b, via an isolated quality A/B that never touches the operational 4b index."
related_control_plane: docs/design/control-plane.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - services/common/src/main/java/com/localrag/common/embedding/EmbeddingClient.java
  - services/common/src/main/java/com/localrag/common/weaviate/WeaviateClient.java
  - services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java
quality_axes:
  - WHOLE
  - SCOPE
  - HANDOFF
tags:
  - docs/project
  - local-rag-system
  - embedding
  - evaluation
---

# P0004 embedding-model-8b-evaluation

- Type: project
- Document ID: P0004
- Status: active
- Project Role: exception-branch
- Umbrella Initiative: embedding-model-8b-evaluation
- Parent Umbrella Project: P0001-local-rag-system
- Completion Mode: decision-lock
- Owner: Dean
- Created: 2026-06-17
- Updated: 2026-06-17
- Current Focus: Gate whether qwen3-embedding:8b should replace operational 4b, via an isolated quality A/B that never touches the operational 4b index.
- Related Control Plane: docs/design/control-plane.md
- Related Design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/msa-runtime-and-storage.md

## Purpose

운영 임베딩 모델을 `qwen3-embedding:4b`(2560-dim)에서 `qwen3-embedding:8b`(4096-dim)로 바꿀지 여부를, **운영 인덱스를 건드리지 않는 격리 실험 + 품질 게이트**로 증거 기반 결정한다. 속도는 이미 4b≈8b(정상 운영)로 확인됐고, 8b의 비용은 1회성 대량 재인덱싱과 Mac mini Ollama 러너 안정성이다. 따라서 채택은 "검증된 검색 품질 이득"에만 의존한다.

## Umbrella Lineage

- `Project Role: exception-branch`. Parent umbrella는 `P0001-local-rag-system`.
- P0003(ScoreGate)이 retrieval *selection* 품질 실험이었던 것과 동격으로, 이 프로젝트는 retrieval *embedding* 모델 실험이다. P0001의 단일 task로 묶기에는 별도 모델·별도 Weaviate 컬렉션·별도 실행 환경·ship/no-ship 결정이라는 독립 lifecycle을 가지므로 예외 분기 project로 분리한다.

## Project Issuance Check

- Human(Dean)이 8b 전환을 시도했다가 운영 부적합(러너 크래시)을 확인한 뒤, "8b는 테스트 환경을 만들어 별도로 진행"하라고 명시 승인하여 발급.
- 에이전트는 이 요청·승인 하에 초안을 준비.
- 별도 project 이유: 운영 인덱스와 분리된 실험 컬렉션/스택, 8b 안전 색인 로직, 그리고 ship/no-ship 결정이라는 decision-lock 종료 상태가 필요.

## Whole-System Anchor

- delivery boundary: "운영 RAG는 4b로 정상 동작을 유지"한다는 불변을 깨지 않는다. 이 project의 어떤 단계도 운영 `LocalRagChunk`(2560) 클래스·운영 4b 색인·`LOCAL_RAG_EMBEDDING_MODEL` 운영값을 변경하지 않는다.
- control-plane invariant: 단일 embedding model/dimension per Weaviate class (차원 혼합 금지). 8b 실험 벡터는 별도 클래스에만 둔다.

## Completion Mode Notes

- `decision-lock`. closed state = 8b adopt / no-adopt 결정이 증거(품질 A/B + 필요 시 런타임 비교)와 함께 리포트로 고정된 상태.
- `functional`이 아닌 이유: 산출물의 본질이 "동작하는 기능"이 아니라 "증거 기반 채택 결정"이기 때문.

## Committed Outcome

- 8b를 운영에 채택할지에 대한 evidence-backed 결정이 리포트로 고정된다.
- adopt면: 안전 색인 로직 + 마이그레이션 경로가 후속 task로 명시된다.
- no-adopt면: 4b 유지 근거와, 8b 재시도가 가능한 조건이 기록된다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | 운영 4b 인덱스/모델을 건드리지 않고 8b를 격리 측정할 수 있는 실험 하니스 확보 | 별도 Weaviate 클래스 `LocalRagChunk8bExp`로 샘플 색인 + 4b vs 8b A/B 리포트가 생성됨 |
| G2 | 8b의 검색 품질 이득 유무를 판정 | 동일 질의셋·동일 청크 기준 4b vs 8b top-k 비교 결과가 `docs/reports`에 고정됨 |
| G3 | 채택/비채택 결정과 다음 행동(마이그레이션 또는 4b 유지) 고정 | decision-lock 리포트 + 후속 task 발급/종료 근거 기록 |

## Scope

- 격리된 8b A/B 게이트(T0025).
- 게이트 positive 시: 안전 청킹/배치 로직 구현(T0026)과 격리 전체 8b 재인덱싱 + 최종 결정(T0027).

## Out Of Scope

- 운영 인덱스/모델 변경(게이트 통과 + 별도 결정 전까지).
- ScoreGate/reranker(P0003 소관).
- 새 임베딩 백엔드/하드웨어 조달.

## References

- docs/reports/2026-05-25-retrieval-quality-baseline.md
- docs/reports/2026-06-16-scoregate-before-after-comparison.md (A/B 리포트 형식 참고)
- docs/projects/P0003-scoregate-adaptive-context-selection.md (실험 project 구조 참고)

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| T0025 | 8B embedding A/B gate harness + report | Active | 0% | 게이트. 운영 불간섭, 별도 클래스 |
| T0026 | Safe chunking/batch in services (conditional) | Draft | 0% | T0025 positive 시에만 발급 |
| T0027 | Isolated full 8B reindex + ship/no-ship decision (conditional) | Draft | 0% | T0026 이후, 한가한 Mac mini 시점 |

## Planned Task Candidates

- T0026, T0027은 게이트(T0025) 결과가 positive일 때만 정식 발급한다. (SDLC 위생: 조건부 task는 게이트 전 발급 안 함)

## Overall Progress

- 0%

## Milestones

- M1: T0025 A/B 게이트 리포트 고정 → adopt 여부 1차 판정

## Exit Criteria

1. 8b adopt/no-adopt 결정이 A/B(필요 시 런타임) 증거와 함께 decision-lock 리포트로 고정됨.
2. 필수 task(T0025, 그리고 발급된 경우 T0026/T0027)가 done 또는 종료 근거와 함께 정리됨.
3. 운영 4b 인덱스/모델이 이 project로 인해 변경되지 않았음을 확인할 수 있음.

## Completion Evidence

- A/B 리포트(top-k 비교, 질의 유형별 결과), 실험 클래스 객체수/차원, 안전 색인 파라미터, (해당 시) 런타임 지연·품질 비교.
- 운영 `LocalRagChunk` 불변 확인(객체수/차원/모델 변경 없음).

## Outputs / Handoff

- adopt: T0026/T0027 발급(안전 청킹 구현 + 격리 전체 재인덱싱 + 마이그레이션 계획).
- no-adopt: 4b 유지 결정 + 8b 재시도 조건(러너 안정화/하드웨어/청크 cap) 기록.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | 운영 RAG(4b)가 실험으로 깨지지 않아야 함 | 운영 클래스/모델 불변 확인 |
| SCOPE | "측정·결정"에 한정, 운영 변경으로 번지지 않음 | 별도 클래스/스택만 사용한 흔적 |
| HANDOFF | 결정 후 다음 행동이 명확해야 함 | decision-lock 리포트 + 후속 task 상태 |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Pending | | |
| G2 | Pending | | |
| G3 | Pending | | |

## Completion Guardrails

- 운영 인덱스/모델 변경을 이 project의 done 처리로 위장하지 않는다.
- 게이트 결과가 미정이면 `done`이 아니라 `active`/`blocked`로 둔다.
- 조건부 task(T0026/T0027)는 게이트 전 발급하지 않는다.

## Status

- 2026-06-17: project 문서 생성. T0025를 active로 발급, 운영 4b 정상화 완료 직후 격리 8b A/B 게이트 착수.
