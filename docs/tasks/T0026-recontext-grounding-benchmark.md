---
type: task
doc_id: T0026
title: recontext-grounding-benchmark
status: done
owner:
created: 2026-07-04
updated: 2026-07-04
current_focus: "Done: query-aware budget-bounded answer evidence packing plus deterministic baseline-vs-ReContext grounding benchmark"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: docs/projects/P0001-local-rag-system.md
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/local-rag-system-development-direction.md
source_refs:
  - docs/tasks/T0025-recontext-context-grounding.md
  - "https://arxiv.org/abs/2607.02509"
  - "ai-paper-product-fit-research:sources/papers/2607.02509-recontext/fulltext.md"
  - services/retrieval-service/src/main/java/com/localrag/retrieval/AnswerEvidencePacker.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/AnswerGroundingBenchmarkTests.java
  - docs/reports/2026-07-04-recontext-answer-grounding-benchmark.md
quality_axes:
  - WHOLE
  - GOAL
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - retrieval-quality
  - recontext
  - evaluation
---

# T0026 recontext-grounding-benchmark

- Type: task
- Document ID: T0026
- Status: done
- Completion Mode: functional
- Owner:
- Created: 2026-07-04
- Updated: 2026-07-04
- Current Focus: Done: query-aware budget-bounded answer evidence packing plus deterministic baseline-vs-ReContext grounding benchmark
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: docs/projects/P0001-local-rag-system.md
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`
  - `docs/design/local-rag-system-development-direction.md`

## Purpose

`T0025`는 ReContext-inspired grounded evidence replay를 replay-everything prompt slice로 도입했다. 이 task는 그 slice를 실제 behavior change가 측정 가능한 production answer path 개선으로 확장한다: `/api/answer` replay block이 query-aware lexical grounding 점수로 evidence를 선택·순서화하고 char/count budget을 지키게 하며, baseline(replay-everything) 대비 ReContext packing을 LLM 없이 재현 가능하게 비교하는 deterministic benchmark harness를 추가한다.

## Task Placement Check

- `/api/answer` context packing은 P0001의 local-only RAG answer surface에 속하는 bounded improvement이므로 별도 project 없이 P0001 아래 task로 둔다.
- `T0025`의 후속으로, 동일한 evidence boundary(검색된 citation-bearing snippet만 사용)를 유지한다.

## Whole-System Anchor

보존하는 invariant:

- private source content는 hosted API로 전송하지 않는다.
- `rag_search`/`/api/search` public contract, ranking, ScoreGate default, source registry, indexing, Weaviate schema는 변경하지 않는다.
- Retrieved context block은 전체 최종 검색 결과를 그대로 보존한다(ReContext "emphasis, not exclusion"). replay block만 선택적이다.
- Benchmark는 LLM 호출 없이 rule-based proxy metric으로만 채점해 flaky하지 않다.

## Completion Mode Notes

Completion mode는 `functional`이다. 닫힌 상태는 `/api/answer` prompt가 query-aware budget-bounded replay block을 생성하고, focused tests와 benchmark tests가 baseline 대비 개선을 결정적으로 검증하는 것이다.

## Committed Outcome

- `AnswerEvidencePacker`: deterministic query-overlap 기반 evidence 선택 컴포넌트. grounded일 때 zero-overlap 후보를 replay에서 제외하고 score/rank 순으로 정렬하며 `charBudget=1600`, `maxEvidence=6`을 지킨다. lexical grounding이 전혀 없으면 dedupe된 retrieval order로 fail-open한다.
- `RetrievalService.answerPrompt`는 packer에 위임하고 retrieved context block은 그대로 유지한다.
- `AnswerGroundingBenchmarkTests`: 6개 대표 케이스(장문 다중 근거, distractor-led ranking, duplicate evidence, 한국어 질의, budget overflow, no-overlap fallback)에서 baseline vs recontext arm을 rule-based proxy metric으로 비교하고 `target/answer-grounding-benchmark.md` 리포트를 생성한다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | `/api/answer` replay block이 query-aware, budget-bounded evidence selection으로 동작하되 retrieved context 전체 보존과 fail-open fallback을 유지한다. | packer 단위 테스트가 ordering, dedupe, budget, fallback, 한국어 containment를 검증하고 기존 retrieval-service 테스트가 통과한다. |
| G2 | baseline(replay-everything) vs ReContext packing을 LLM 없이 재현 가능하게 비교하는 benchmark harness가 존재한다. | benchmark 테스트가 evidence hit, distractor exclusion, first evidence position, replay chars, pack latency를 산출·검증하고 리포트 파일을 생성한다. |
| G3 | 결과와 한계가 문서화되어 후속 worker가 rollback/확장 판단을 할 수 있다. | benchmark report 문서와 design doc 갱신, required validators 통과가 기록된다. |

## Scope

- `retrieval-service` answer prompt replay selection 개선(`AnswerEvidencePacker` 신설).
- Focused packer 단위 테스트와 deterministic grounding benchmark.
- Benchmark report 및 docs harness 갱신.

## Out Of Scope

- ReContext official code import, attention readout, training, KV-cache 조작, custom decoding.
- `rag_search` ranking/response 변경, Weaviate/PostgreSQL schema 변경, source registry/indexing 변경, MCP contract 변경.
- LLM 기반 answer quality 평가(비결정적이므로 이 harness에서 제외; evidence boundary는 prompt packing까지).
- 배포, 운영존 재시작, worknote release note.

## References

- `docs/tasks/T0025-recontext-context-grounding.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/reports/2026-07-04-recontext-answer-grounding-benchmark.md`
- `https://arxiv.org/abs/2607.02509`

## Dependencies

- `T0025` answer evidence replay slice(`feature/recontext-context-grounding`, commit `dc0492f`).

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Implement `AnswerEvidencePacker` and integrate into `answerPrompt` | Done | 100% | replay-only change; retrieved context block untouched |
| W2 | Add focused packer unit tests | Done | 100% | 8 tests: ordering, dedupe, budget, oversized-first-keep, fallback, Korean, blank snippets, baseline arm |
| W3 | Add deterministic baseline-vs-ReContext benchmark harness | Done | 100% | 6 cases, rule-based proxy metrics, report artifact |
| W4 | Record benchmark report and update design/docs harness | Done | 100% | see completion evidence |
| W5 | Run validators and focused Maven tests | Done | 100% | see completion evidence |

## Overall Progress

- 100%

## Completion Criteria

1. `mvn -pl services/retrieval-service -am test`가 packer/benchmark 테스트 포함 전부 통과한다.
2. Benchmark가 baseline 대비 distractor exclusion 개선과 replay context 축소를 결정적으로 보이고 evidence hit 1.0을 유지한다.
3. Required validators와 `docker compose --env-file .env.example config`, `git diff --check`가 통과한다.
4. 배포와 worknote release note는 수행하지 않는다.

## Completion Evidence

- Focused tests: `mvn -pl services/retrieval-service -am test` — common 7/0 failures, retrieval-service 32/0 failures (packer 8, benchmark 2 포함).
- Benchmark: `mvn -pl services/retrieval-service -am test -Dtest=AnswerGroundingBenchmarkTests` — avg distractor exclusion baseline 0.17 vs recontext 0.96, total replay chars 3368 vs 1771, evidence hit 1.00 유지. 리포트: `services/retrieval-service/target/answer-grounding-benchmark.md`, 고정 사본 `docs/reports/2026-07-04-recontext-answer-grounding-benchmark.md`.
- Validators: `./docs/bin/validate-codex-readiness.sh`, `./docs/bin/validate-harness-foundation.sh`, `./docs/bin/validate-doc-retrieval.sh`, `./docs/bin/validate-closeout.sh --all`, `docker compose --env-file .env.example config`, `git diff --check` 결과는 Status에 기록한다.
- LLM 기반 품질 측정은 이 evidence에 포함되지 않는다. proxy metric은 replay block 구성 품질만 증명한다.

## Outputs / Handoff

- Code: `services/retrieval-service/src/main/java/com/localrag/retrieval/AnswerEvidencePacker.java`
- Tests: `AnswerEvidencePackerTests.java`, `AnswerGroundingBenchmarkTests.java`
- Report: `docs/reports/2026-07-04-recontext-answer-grounding-benchmark.md`
- Residual debt: packer budget/limit이 코드 상수라 `RetrievalSettings` 노출이 후속 후보; Korean은 token containment 기반이라 조사(particle) 불일치 시 매칭이 약함(bigram/형태소 후속 후보); 실제 LLM answer quality A/B는 별도 task 필요.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | answer path 개선이 local-only, citation, 검색 contract 불변 원칙을 지켜야 한다 | search contract/DTO/schema 변경 없음; retrieved context 전체 보존 |
| GOAL | prompt-only slice를 측정 가능한 production behavior change로 확장한다는 발급 목표 유지 | packer가 실제 `/api/answer` prompt를 바꾸고 benchmark가 그 차이를 계량한다 |
| EVIDENCE | 개선 주장은 재현 가능한 결정적 측정으로만 한다 | rule-based benchmark + focused tests + validators |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `AnswerEvidencePackerTests` 8건, `RetrievalServiceTests` 기존 prompt 계약 테스트 통과 | fallback이 이전(replay-in-retrieval-order) 동작을 보존 |
| G2 | Done | `AnswerGroundingBenchmarkTests` 2건 통과, 리포트 생성 | LLM 미사용, 케이스·채점 모두 결정적 |
| G3 | Done | benchmark report 문서, design doc 갱신, validator 결과 기록 | 배포 없음 |

## Completion Guardrails

- Retrieved context block 축소나 검색 contract 변경으로 scope를 넓히지 않는다.
- Benchmark proxy metric을 LLM answer quality 증거로 과장하지 않는다.
- 배포, 운영존 재시작, worknote release note를 수행하지 않는다.

## Risks / Open Questions

- Lexical overlap은 의미적 관련성의 근사치다. paraphrase evidence는 fallback 경로로만 보존된다.
- replay 제외가 잘못돼도 retrieved context block에 전체 결과가 남아 모델이 접근할 수 있다(안전장치).
- 실제 답변 품질 개선량은 LLM A/B 없이는 단정할 수 없다.

## Status

- 2026-07-04: task 문서 생성.
- 2026-07-04: `AnswerEvidencePacker` 구현, `answerPrompt` 통합, packer 단위 테스트 8건 추가.
- 2026-07-04: baseline-vs-ReContext deterministic benchmark harness와 리포트 추가. focused Maven tests 32/0 통과.
