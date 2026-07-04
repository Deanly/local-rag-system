---
type: report
title: recontext-answer-grounding-benchmark
status: done
owner:
created: 2026-07-04
updated: 2026-07-04
current_focus: "Baseline vs ReContext answer evidence packing benchmark results"
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
- Current Focus: Baseline vs ReContext answer evidence packing benchmark results
- Report Type: recontext-answer-grounding-benchmark
- Related Project: docs/projects/P0001-local-rag-system.md

## Summary

`/api/answer`의 grounded evidence replay block에 대해 baseline(T0025의 replay-everything, retrieval order, budget 없음)과 ReContext packing(T0026의 query-aware selection, char/count budget)을 deterministic rule-based proxy metric으로 비교했다. 6개 대표 케이스 전부에서 evidence hit 1.00을 유지하면서 평균 distractor exclusion이 0.17에서 0.96으로, 총 replay context가 3368 chars에서 1771 chars로 개선됐다. LLM은 채점에 사용하지 않았다.

## Scope

- replay block 구성 품질만 측정한다. LLM 최종 답변 품질은 측정하지 않는다.
- Retrieved context block(전체 검색 결과)은 두 arm 모두 동일하게 보존된다.
- 케이스는 코드에 고정된 synthetic fixture로, 검색 스택(Weaviate/Ollama) 없이 재현된다.

## Inputs

- Benchmark 명령: `mvn -pl services/retrieval-service -am test -Dtest=AnswerGroundingBenchmarkTests`
- Runtime artifact: `services/retrieval-service/target/answer-grounding-benchmark.md`
- ReContext config: `charBudget=1600`, `maxEvidence=6`

## Findings

Proxy metric 정의:

- Evidence hit: 케이스별 expected citation 중 replay block에 포함된 비율.
- Distractor exclusion: distractor citation 중 replay block에서 제외된 비율.
- First evidence pos: replay block에서 첫 expected evidence의 위치(1이 최선).
- Replay chars: replay block snippet 문자 수(token/context-size proxy).
- Pack us: packing 소요 마이크로초(참고용, 채점 미사용).

2026-07-04 실행 결과:

| Case | Arm | Evidence hit | Distractor exclusion | First evidence pos | Selected | Replay chars | Pack us |
| --- | --- | --- | --- | --- | --- | --- | --- |
| multi-evidence-long-query | baseline | 1.00 | 0.00 | 2 | 8 | 653 | 941 |
| multi-evidence-long-query | recontext | 1.00 | 1.00 | 1 | 3 | 303 | 463 |
| distractor-led-ranking | baseline | 1.00 | 0.00 | 4 | 5 | 448 | 200 |
| distractor-led-ranking | recontext | 1.00 | 1.00 | 1 | 2 | 216 | 300 |
| duplicate-evidence | baseline | 1.00 | 0.00 | 1 | 2 | 162 | 110 |
| duplicate-evidence | recontext | 1.00 | 1.00 | 1 | 1 | 99 | 214 |
| korean-query | baseline | 1.00 | 0.00 | 2 | 4 | 156 | 279 |
| korean-query | recontext | 1.00 | 1.00 | 1 | 2 | 99 | 288 |
| budget-overflow | baseline | 1.00 | 0.00 | 1 | 6 | 1886 | 407 |
| budget-overflow | recontext | 1.00 | 0.75 | 1 | 3 | 991 | 320 |
| no-lexical-overlap-fallback | baseline | 1.00 | 1.00 | -1 | 2 | 63 | 111 |
| no-lexical-overlap-fallback | recontext | 1.00 | 1.00 | -1 | 2 | 63 | 76 |

- 평균 distractor exclusion: baseline 0.17 vs recontext 0.96.
- 총 replay chars: baseline 3368 vs recontext 1771 (budget-overflow 케이스에서 baseline은 1886 chars로 budget 초과, recontext는 991 chars).
- Distractor-led ranking 케이스에서 recontext는 true evidence를 replay 1번 위치로 올린다(baseline은 4번).
- No-overlap 케이스에서 recontext는 dedupe된 retrieval order로 fail-open해 baseline과 동일하게 동작한다.

해석: replay block이 질문과 겹치는 근거를 앞세우고 무관한 근거와 중복을 제외하므로, 모델이 긴/다중 근거 컨텍스트에서 관련 근거를 더 잘 쓰도록 하는 ReContext의 "grounded span replay" 의도를 prompt packing 수준에서 구현한다. 전체 검색 결과는 retrieved context block에 남아 있어 잘못된 제외의 안전장치가 된다.

한계:

- Lexical overlap은 의미적 관련성의 근사치다. paraphrase-only evidence는 replay에서 빠질 수 있고 fallback/retrieved context로만 보존된다.
- 한국어는 token containment 기반이라 조사 변형에 약하다(예: `문서를` vs `문서에`).
- 이 수치는 replay 구성 품질이지 LLM 답변 품질이 아니다. 실제 answer quality A/B는 별도 task가 필요하다.

## Recommendations

- 후속: packer budget을 `RetrievalSettings`로 노출, 한국어 bigram/형태소 매칭, 실제 LLM answer quality A/B task.
- Merge 판단 시 이 리포트와 `T0026` completion evidence를 함께 볼 것.

## Follow-Up Promotion

- 없음. 후속 후보는 `T0026` Outputs / Handoff에 기록했다.

## Status

- 2026-07-04: report 문서 생성.
- 2026-07-04: benchmark 실행 결과와 해석, 한계 기록. status done.
