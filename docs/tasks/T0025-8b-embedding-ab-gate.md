---
type: task
doc_id: T0025
title: 8b-embedding-ab-gate
status: active
owner: Dean
created: 2026-06-17
updated: 2026-06-17
current_focus: "Build and run an isolated 4b-vs-8b retrieval A/B on a personal-core design/ sample to decide if 8b quality justifies adoption."
completion_mode: investigation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0004-embedding-model-8b-evaluation
related_project: docs/projects/P0004-embedding-model-8b-evaluation.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - docs/bin/run-8b-embedding-ab.py
  - services/common/src/main/java/com/localrag/common/embedding/EmbeddingClient.java
  - services/common/src/main/java/com/localrag/common/weaviate/WeaviateClient.java
quality_axes:
  - WHOLE
  - GOAL
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - embedding
  - evaluation
---

# T0025 8b-embedding-ab-gate

- Type: task
- Document ID: T0025
- Status: active
- Completion Mode: investigation
- Owner: Dean
- Created: 2026-06-17
- Updated: 2026-06-17
- Current Focus: Build and run an isolated 4b-vs-8b retrieval A/B on a personal-core design/ sample to decide if 8b quality justifies adoption.
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0004-embedding-model-8b-evaluation
- Related Project: docs/projects/P0004-embedding-model-8b-evaluation.md
- Related Design:
  - docs/design/retrieval-quality-improvement-design.md

## Purpose

운영 4b 인덱스를 건드리지 않고, 동일 청크·동일 질의 기준으로 4b와 8b 임베딩의 검색 품질을 비교해 "8b 채택 가치 유무"를 게이트한다.

## Task Placement Check

- P0004(8b 평가) umbrella 아래 첫 실행 task. 별도 project 불필요.
- 게이트 결과가 P0004의 G1/G2를 직접 닫는다.

## Whole-System Anchor

- 운영 `LocalRagChunk`(2560)·운영 4b 색인·`LOCAL_RAG_EMBEDDING_MODEL` 운영값을 변경하지 않는다.
- 실험 벡터는 별도 클래스에만(차원 혼합 금지 invariant 보존).

## Completion Mode Notes

- `investigation`. closed state = 4b vs 8b A/B 결과가 리포트로 고정되고, P0004가 다음 단계(T0026 발급 또는 4b 유지)를 결정할 수 있는 상태.

## Committed Outcome

- 격리 A/B 하니스(`docs/bin/run-8b-embedding-ab.py`)와, 4b vs 8b top-k 비교 리포트(`docs/reports/2026-06-17-8b-embedding-ab-gate.md`)가 생성된다.
- 리포트는 "8b가 의미 있는 품질 이득이 있는가"에 yes/no/unclear로 답한다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | 운영 불간섭 격리 A/B 하니스 확보 | 스크립트가 별도 클래스 2개(`LocalRagChunk4bRef`, `LocalRagChunk8bExp`)에 동일 캡 청크를 색인하고 비교를 출력 |
| G2 | 4b vs 8b 검색 품질 비교 증거 고정 | 질의셋 결과(top-k 겹침/랭킹/정성)와 판정이 리포트에 기록 |

## Scope

- 샘플: personal-core `design/` 청크(운영 클래스에서 텍스트 추출, 공정 비교 위해 동일 텍스트 사용).
- 8b 안전 색인: chunk 토큰 cap ~1200, batch=1, concurrency=1, HTTP 500 시 재시도 대신 분할/트렁케이트.
- 공정성: 4b와 8b 모두 동일한 캡 청크 텍스트로 별도 ref/exp 클래스에 색인 → 모델 효과만 격리.
- 질의셋: 한/영, 어휘불일치형, 식별자형, 복합근거형 등 대표 10~20개.

## Out Of Scope

- 운영 인덱스/모델 변경.
- 실제 서비스 코드(MarkdownChunker/EmbeddingClient) 수정(→ T0026, 조건부).
- 전체 코퍼스 8b 색인(→ T0027, 조건부).

## References

- docs/reports/2026-06-16-scoregate-before-after-comparison.md (A/B 형식)
- docs/projects/P0004-embedding-model-8b-evaluation.md

## Dependencies

- 운영 4b 정상화 완료(2026-06-17, 2146 docs / 31936 chunks).
- Mac mini Ollama가 8b 색인을 돌릴 만큼 한가한 시간대(Codex 등 미사용) — 색인 단계 실행 전제.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | A/B 하니스 스크립트 작성 | Done | 100% | docs/bin/run-8b-embedding-ab.py |
| W2 | 한가한 시점 8b/4b-ref 샘플 색인 실행 | Todo | 0% | Mac mini 부하 확인 후 |
| W3 | A/B 비교 + 리포트 고정 + 판정 | Todo | 0% | docs/reports/2026-06-17-8b-embedding-ab-gate.md |

## Overall Progress

- 20%

## Completion Criteria

1. 스크립트가 운영 클래스를 변경하지 않고 별도 클래스에 4b-ref/8b-exp를 색인한다(객체수/차원 확인).
2. 동일 질의셋으로 4b vs 8b top-k 비교 결과가 리포트에 기록된다.
3. "8b 채택 가치" 판정(yes/no/unclear)과 P0004 다음 단계 권고가 적힌다.

## Completion Evidence

- 실험 클래스 객체수/차원(4096 vs 2560), 안전 색인 파라미터, 질의별 top-k 비교, 운영 클래스 불변 확인.

## Outputs / Handoff

- positive → P0004가 T0026(안전 청킹 구현) 발급.
- negative/unclear → 4b 유지, 8b 재시도 조건 기록.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | 운영 4b가 실험으로 안 깨짐 | 운영 클래스 객체수/차원/모델 불변 |
| GOAL | "품질 비교로 채택 게이트" 목표 유지 | A/B 리포트 존재 |
| EVIDENCE | 결정의 근거 | top-k 비교 + 판정 |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Pending | | 스크립트 작성됨, 실행 대기 |
| G2 | Pending | | |

## Completion Guardrails

- 운영 인덱스/모델 변경을 이 task의 done으로 위장하지 않는다.
- 8b 색인은 Mac mini 한가한 시점에만 실행한다(러너 BPT trap 회피 위해 cap/batch=1/split 준수).

## Risks / Open Questions

- 토큰 카운트: 정확한 tokenizer 없이 문자 기반 휴리스틱으로 cap → 보수적으로 설정(러너 크래시 회피 우선).
- 샘플 범위(design/)가 전체 코퍼스 품질을 대표하지 못할 수 있음 → 신호가 약하면 범위 확대.

## Status

- 2026-06-17: task 문서 생성, A/B 하니스 스크립트(`docs/bin/run-8b-embedding-ab.py`) 작성 완료. Mac mini 한가한 시점에 색인 실행 + 리포트 고정 예정.
