---
type: report
title: recontext-answer-grounding-benchmark
status: done
owner:
created: 2026-07-04
updated: 2026-07-04
current_focus: "Decision-grade baseline vs ReContext answer evidence packing benchmark"
report_type: recontext-answer-grounding-benchmark
related_project: docs/projects/P0001-local-rag-system.md
related_task:
  - docs/tasks/T0025-recontext-context-grounding.md
  - docs/tasks/T0026-recontext-grounding-benchmark.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - services/retrieval-service/src/main/java/com/localrag/retrieval/AnswerEvidencePacker.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/AnswerGroundingBenchmarkTests.java
tags:
  - docs/report
  - local-rag-system
  - retrieval-quality
  - recontext
  - evaluation
---

# recontext-answer-grounding-benchmark

- Type: report
- Status: done
- Owner:
- Created: 2026-07-04
- Updated: 2026-07-04
- Current Focus: Decision-grade baseline vs ReContext answer evidence packing benchmark
- Report Type: recontext-answer-grounding-benchmark
- Related Project: docs/projects/P0001-local-rag-system.md

## Summary

`/api/answer`의 grounded evidence replay block에 대해 14개 대표 케이스로 baseline과 ReContext packing을 비교했다. 결론: **모든 결정 threshold PASS** — macro evidence hit 0.93(≥0.90), distractor exclusion 0.14→0.84(+0.70, 기준 +0.30), replay context 8657→3505 chars(−59.5%, 기준 −20%), budget(1600c/6개) 전 케이스 준수. 유일한 의도적 regression 케이스(paraphrase-only evidence)는 replay에서 빠지지만 retrieved context block에 보존됨을 별도 테스트로 확인했다. LLM은 채점에 사용하지 않았다.

식별자:

- Candidate: `feature/recontext-benchmark-grounding` (`AnswerEvidencePacker`, T0026).
- Baseline arm: T0025 replay-everything(`dc0492f` 동작을 `baselinePack`으로 고정 재현).
- Pre-candidate production baseline: `origin/main` `6e490ea` — replay block 자체가 없음(replay overhead 0, evidence 강조 없음). 두 arm의 replay chars는 main 대비 순수 additive prompt overhead다.

## Scope

- replay block 구성 품질만 측정한다. LLM 최종 답변 품질은 측정하지 않는다.
- Retrieved context block(전체 검색 결과)은 두 arm 모두 동일하게 보존된다.
- 케이스는 코드에 고정된 synthetic fixture로, 검색 스택(Weaviate/Ollama) 없이 재현된다.

## Inputs

- Benchmark 명령: `mvn -pl services/retrieval-service -am test -Dtest=AnswerGroundingBenchmarkTests -Dsurefire.failIfNoSpecifiedTests=false`
- Runtime artifact: `services/retrieval-service/target/answer-grounding-benchmark.md`
- ReContext config: `charBudget=1600`, `maxEvidence=6`

## Metric Definitions

- Evidence hit: 케이스별 expected citation 중 replay block에 포함된 비율(expected 없으면 1.0).
- Distractor exclusion: distractor citation 중 replay block에서 제외된 비율(distractor 없으면 1.0).
- First evidence pos: replay block에서 첫 expected evidence의 위치(1이 최선, −1은 없음).
- Selected: replay block에 선택된 evidence 수(count budget proxy).
- Replay chars: replay snippet 문자 수(token/context-size proxy; origin/main 대비 prompt overhead).
- Pack us: packing 소요 마이크로초(참고용, 채점·threshold 미사용).

## Case Categories

14 cases: direct fact retrieval / distractor-heavy(10 distractor) / multi-evidence answer / distractor-led ranking / long·noisy context(12 results) / Korean query / bilingual content / near-duplicate dedupe / conflicting near-duplicate evidence / budget overflow / no-answer·insufficient evidence(빈 결과) / no-lexical-overlap fallback / **regression risk: paraphrase drop(의도적 실패 허용)** / regression risk: lexical trap.

## Findings

2026-07-04 실행 결과 (`services/retrieval-service/target/answer-grounding-benchmark.md`):

| Case | Category | Arm | Evidence hit | Distractor exclusion | First evidence pos | Selected | Replay chars |
| --- | --- | --- | --- | --- | --- | --- | --- |
| direct-fact | direct fact retrieval | baseline / recontext | 1.00 / 1.00 | 0.00 / 1.00 | 2 / 1 | 4 / 1 | 201 / 52 |
| distractor-heavy | distractor-heavy retrieval | baseline / recontext | 1.00 / 1.00 | 0.00 / 1.00 | 3 / 1 | 12 / 2 | 726 / 210 |
| multi-evidence-long-query | multi-evidence answer | baseline / recontext | 1.00 / 1.00 | 0.00 / 1.00 | 2 / 1 | 8 / 3 | 653 / 303 |
| distractor-led-ranking | distractor-led ranking | baseline / recontext | 1.00 / 1.00 | 0.00 / 1.00 | 4 / 1 | 5 / 2 | 448 / 216 |
| long-noisy-context | long/noisy context | baseline / recontext | 1.00 / 1.00 | 0.00 / 1.00 | 2 / 1 | 12 / 3 | 3635 / 962 |
| korean-query | Korean query | baseline / recontext | 1.00 / 1.00 | 0.00 / 1.00 | 2 / 1 | 4 / 2 | 156 / 99 |
| bilingual-evidence | bilingual content | baseline / recontext | 1.00 / 1.00 | 0.00 / 1.00 | 2 / 1 | 4 / 2 | 198 / 119 |
| duplicate-evidence | near-duplicate dedupe | baseline / recontext | 1.00 / 1.00 | 0.00 / 1.00 | 1 / 1 | 2 / 1 | 162 / 99 |
| conflicting-near-duplicate | conflicting evidence | baseline / recontext | 1.00 / 1.00 | 0.00 / 1.00 | 1 / 1 | 3 / 2 | 211 / 150 |
| budget-overflow | long/noisy (budget) | baseline / recontext | 1.00 / 1.00 | 0.00 / 0.75 | 1 / 1 | 6 / 3 | 1859 / 974 |
| no-results | insufficient evidence | baseline / recontext | 1.00 / 1.00 | 1.00 / 1.00 | - | 0 / 0 | 0 / 0 |
| no-lexical-overlap-fallback | fallback safety | baseline / recontext | 1.00 / 1.00 | 1.00 / 1.00 | - | 2 / 2 | 63 / 63 |
| paraphrase-evidence-regression | regression: paraphrase drop | baseline / recontext | 1.00 / **0.00** | 0.00 / 0.00 | 1 / -1 | 2 / 1 | 152 / 65 |
| lexical-trap-survival | regression: lexical trap | baseline / recontext | 1.00 / 1.00 | 0.00 / 0.00 | 3 / 1 | 3 / 3 | 193 / 193 |

Decision thresholds:

| Threshold | Target | Observed | Pass |
| --- | --- | --- | --- |
| T1 non-regression 케이스 evidence hit | 1.00 each | 12/12 non-regression 케이스 1.00 (per-case assert) | PASS |
| T2 macro recontext hit | >= 0.90 | 0.93 | PASS |
| T3 exclusion 개선 | >= +0.30 | +0.70 (0.14 → 0.84) | PASS |
| T4 replay chars 절감 | >= 20% | 59.51% (8657 → 3505) | PASS |
| T5 budget 준수 (1600c / 6개) | always | 전 케이스 assert | PASS |

주요 관찰:

- 관련 근거는 모든 non-regression 케이스에서 replay 1번 위치로 올라온다(baseline은 retrieval order 그대로라 distractor가 앞설 수 있음).
- Conflicting near-duplicate 케이스에서 상충하는 두 근거가 모두 replay되어 모델이 source priority(현행 vs draft)로 판별할 수 있다.
- No-results/no-overlap 케이스에서 recontext는 baseline과 동일하게 동작(fail-open)하며 "No replayable evidence." fallback이 유지된다.
- **Known regression**: paraphrase-only evidence(질의와 어휘가 전혀 겹치지 않는 근거)는 spurious 토큰을 가진 distractor가 있으면 replay에서 빠진다(hit 0.00). 별도 테스트로 해당 근거가 retrieved context block에는 항상 남는 것을 검증했다 — 모델 접근성은 origin/main 수준으로 보존되고, 잃는 것은 "강조"뿐이다.
- Pack 시간은 케이스당 수백 µs 수준으로 answer path의 Ollama 추론 시간 대비 무시 가능.

## Limitations

- Lexical overlap은 의미적 관련성의 근사치다. paraphrase evidence 강조 누락이 구조적 한계다(위 regression 케이스).
- 한국어는 token containment 기반이라 조사 변형(`문서를` vs `문서에`)에 약하다.
- 이 수치는 replay block 구성 품질이지 LLM 답변 품질이 아니다. 실제 answer quality A/B는 별도 task가 필요하다.
- 케이스는 synthetic fixture다. 실 코퍼스 분포와 다를 수 있다.

## Recommendations

- Merge 권고: threshold 전부 PASS이고 rollback이 `answerPrompt` 위임 제거 한 줄 수준이므로 candidate branch merge 가능.
- 후속: packer budget의 `RetrievalSettings` 노출, 한국어 bigram/형태소 매칭, paraphrase 보강(예: 후순위 fallback slot), 실제 LLM answer quality A/B task.

## Follow-Up Promotion

- 없음. 후속 후보는 `T0026` Outputs / Handoff에 기록했다.

## Status

- 2026-07-04: report 문서 생성.
- 2026-07-04: 6-케이스 초기 실행 결과 기록.
- 2026-07-04: decision-grade 확장 — 14 케이스(카테고리 8종), metric 정의 고정, 결정 threshold 5종 도입, 전부 PASS. regression 케이스(paraphrase drop)와 mitigation 검증 포함. status done.
