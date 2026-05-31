---
type: report
title: local-reranker-evaluation-decision
status: done
owner:
created: 2026-05-31
updated: 2026-05-31
current_focus: "P0002 local reranker deployment decision after metadata, governance ranking, evaluation, and audit hardening"
report_type: retrieval-reranker-decision
related_project: docs/projects/P0002-retrieval-governance-hardening.md
related_task:
  - docs/tasks/T0017-local-reranker-evaluation.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - docs/evaluation/retrieval-quality-cases.yaml
  - docs/bin/validate-retrieval-quality.sh
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalRanker.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
tags:
  - docs/report
  - local-rag-system
  - retrieval-quality
  - reranker
---

# local-reranker-evaluation-decision

- Type: report
- Status: done
- Owner:
- Created: 2026-05-31
- Updated: 2026-05-31
- Current Focus: P0002 local reranker deployment decision after metadata, governance ranking, evaluation, and audit hardening
- Report Type: retrieval-reranker-decision
- Related Project: `docs/projects/P0002-retrieval-governance-hardening.md`
- Related Task:
  - `docs/tasks/T0017-local-reranker-evaluation.md`
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`

## Summary

P0002 배포 범위에는 별도 local model reranker를 넣지 않는다.

현재 deterministic governance ranking은 P0002 portable fixture에서 default로 중요한 `hybrid`와 semantic fallback인 `vector` 모두 top1/top5 100%를 통과했다. 전체 top1 91.7%의 남은 gap은 Korean natural-language `keyword` 모드 2건이 rank 2로 밀린 것이다. 이는 default `hybrid` path의 배포 blocker가 아니며, cross-encoder나 LLM judge를 추가하기에는 운영 복잡도와 latency risk가 품질 이득보다 크다.

## Scope

이 보고서는 T0013-T0016 이후 상태에서 local cross-encoder, embedding-similarity, 또는 local LLM judge reranker를 P0002 배포 전에 추가해야 하는지 판단한다.

포함한 범위:

- portable retrieval fixture 재실행
- top1 miss class 확인
- T0016 `search_audit` phase latency 확인
- local-only/security boundary 확인
- P0002 closeout decision으로 승격할 결론

포함하지 않은 범위:

- 새 모델 다운로드 또는 모델 서버 추가
- hosted reranker 또는 hosted judge 평가
- fixture를 특정 machine-local source로 확장
- UI 구현

## Inputs

명령:

```bash
LOCAL_RAG_EVAL_OUTPUT=/tmp/local-rag-t0017-eval.json ./docs/bin/validate-retrieval-quality.sh
```

결과 요약:

| Scope | Observations | hit@1 | hit@5 | MRR | Source accuracy@1 | Staleness errors |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| overall | 24 | 91.7% | 100.0% | 0.958 | 100.0% | 0 |
| hybrid | 9 | 100.0% | 100.0% | 1.000 | 100.0% | 0 |
| vector | 7 | 100.0% | 100.0% | 1.000 | 100.0% | 0 |
| keyword | 8 | 75.0% | 100.0% | 0.875 | 100.0% | 0 |

Top1 misses:

| Case | Mode | Rank | Expected | Top Result |
| --- | --- | ---: | --- | --- |
| `rq-ko-001` | `keyword` | 2 | `design/source-registry-and-project-ssot.md` | `examples/T0001-bootstrap-source-ingest.md` |
| `rq-ko-002` | `keyword` | 2 | `design/retrieval-quality-improvement-design.md` | `tasks/T0011-retrieval-quality-hardening.md` |

Audit sample from `search_audit` for the same runtime window:

| Mode | Rows | Avg total ms | Avg embedding ms | Avg Weaviate ms | Avg weighting ms |
| --- | ---: | ---: | ---: | ---: | ---: |
| `hybrid` | 46 | 526.0 | 486.5 | 30.2 | 0.8 |
| `keyword` | 38 | 10.1 | 0.0 | 7.0 | 0.3 |
| `vector` | 33 | 394.3 | 376.9 | 13.5 | 0.2 |

## Findings

### F1. Default Retrieval Is Already At The P0002 Deployment Bar

`hybrid` is the default practical mode for Codex and operator use. It reached 100% top1, 100% top5, 100% source accuracy, and 0 staleness errors in the portable fixture.

### F2. Remaining Top1 Gap Is Keyword-Only Korean Lexical Mismatch

The only top1 misses are Korean natural-language `keyword` cases. The expected document is still rank 2, and `hybrid`/`vector` return the expected document at rank 1 for the same intents. This points to lexical mismatch, not a source-governance failure.

### F3. Model Reranker Would Add A New Operational Surface For Little Current Gain

Current deterministic weighting adds less than 1 ms on average in the audit sample. A cross-encoder or local LLM judge would add model memory, startup, timeout, and fallback policy. With default `hybrid` already at 100% top1 in the versioned fixture, P0002 should not introduce that surface before deployment.

### F4. The Evaluation Runner Needed Top1 Drift Visibility

Before T0017, the runner printed only complete misses. It now reports `top1_misses` so future reranker decisions can distinguish "not found" from "found but not first".

### F5. Default-Mode Drift Should Be Fixed Deterministically First

After P0002 closeout docs were indexed, `rq-ko-002` briefly exposed a `hybrid`/`vector` top1 drift where the glossary outranked the retrieval-quality design. That was corrected by adding Korean aliases for search/quality/evaluation/harness and a retrieval-quality path boost. The focused regression test is `RetrievalRankerTests.koreanRetrievalQualityQueryPrefersDomainDesignOverTermGlossary`.

## Decision

Do not ship a separate local model reranker in P0002.

The release path is:

- keep deterministic governance ranking inside `retrieval-service`,
- keep `hybrid` as the default mode,
- keep `vector` as the semantic fallback for Korean natural-language queries,
- keep `keyword` for identifier-heavy and exact-title lookup,
- revisit model reranker only when an expanded portable fixture shows repeated `hybrid` or `vector` top1/source/staleness regressions.

## Follow-Up Promotion

Promote this decision into:

- `docs/tasks/T0017-local-reranker-evaluation.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/projects/P0002-retrieval-governance-hardening.md`

Future reranker work should be a new task only if a broader fixture shows measurable default-mode regression or if an operator explicitly chooses the latency/memory tradeoff.

## Status

- 2026-05-31: report created from T0017 portable retrieval evaluation, top1 miss analysis, and `search_audit` phase latency evidence.
