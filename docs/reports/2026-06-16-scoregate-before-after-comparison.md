---
type: report
title: scoregate-before-after-comparison
status: done
owner: Codex
created: 2026-06-16
updated: 2026-06-16
current_focus: "Before/after comparison for ScoreGate selector evidence versus deployed runtime behavior"
report_type: scoregate-before-after-comparison
related_project: docs/projects/P0003-scoregate-adaptive-context-selection.md
related_task:
  - docs/tasks/T0021-scoregate-offline-selector-experiment.md
  - docs/tasks/T0022-scoregate-offline-evaluation-fixture.md
  - docs/tasks/T0023-local-cross-encoder-score-source.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - docs/evaluation/scoregate-offline-cases.json
  - docs/bin/validate-scoregate-offline.sh
  - docs/reports/2026-06-16-scoregate-offline-snapshot-evaluation.md
  - docs/reports/2026-06-16-scoregate-runtime-no-ship-decision.md
  - services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateOfflineEvaluationTests.java
tags:
  - docs/report
  - local-rag-system
  - retrieval-quality
  - scoregate
  - evaluation
---

# scoregate-before-after-comparison

- Type: report
- Status: done
- Owner: Codex
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Before/after comparison for ScoreGate selector evidence versus deployed runtime behavior
- Report Type: scoregate-before-after-comparison
- Related Project: `docs/projects/P0003-scoregate-adaptive-context-selection.md`

## Summary

ScoreGate does not require reindexing for this comparison. It is a final context-selection algorithm over already retrieved candidate chunks, not an indexing, embedding, schema, or source-data migration.

No old-version redeploy was required. The before state is represented by fixed top-K selection over the same offline candidate snapshots, and the after state is represented by `ScoreGateCandidateSelector` over those same candidates.

The important conclusion is split:

- Offline selector evidence is positive: ScoreGate improves expected candidate retention, ranking, B3 rescue, and known-token filtering on the small fixture.
- Deployed runtime quality is not yet improved by ScoreGate, because P0003 intentionally did not wire ScoreGate into live `rag_search`; current runtime still has no true local cross-encoder `r_i` source.

## Data Freshness / Reindex Decision

No reindex was needed.

Reasons:

- The fixture operates on candidate snapshots in `docs/evaluation/scoregate-offline-cases.json`.
- ScoreGate consumes normalized `s_i` and `r_i` candidate scores after retrieval.
- The deployed Local RAG index was left intact.
- Runtime `rag_search` behavior remains unchanged by P0003.

## Offline Before / After

Fixture command:

```bash
LOCAL_RAG_SCOREGATE_OUTPUT=/tmp/local-rag-scoregate-after.json ./docs/bin/validate-scoregate-offline.sh
```

Before metric definition:

- fixed top-K over the same fixture candidates,
- `MAX-K = 10`,
- expected rank is the candidate's original first-stage rank when it is below 10,
- known-token counts only include fixture candidates, not a full production top-10 window.

After metric definition:

- `ScoreGateCandidateSelector`,
- paper-reference selector thresholds used only for fixture contract testing,
- expected rank is rank among retained ScoreGate candidates.

| Metric | Fixed top-K before | ScoreGate after | Delta |
| --- | ---: | ---: | ---: |
| Cases | 5 | 5 | - |
| hit@1 | 20.0% | 80.0% | +60.0 pp |
| hit@5 | 60.0% | 100.0% | +40.0 pp |
| MRR | 0.307 | 0.900 | +0.593 |
| Expected candidate retained | 60.0% | 100.0% | +40.0 pp |
| Multi-hop coverage | 80.0% | 100.0% | +20.0 pp |
| Known retained token estimate | 1,810 | 1,460 | -350 (-19.3%) |
| Must-drop candidates retained | 3 | 0 | -3 |
| B3 rescue cases | 0 | 2 | +2 |

## Case-Level Notes

| Case | Intent | Fixed top-K before | ScoreGate after | Interpretation |
| --- | --- | --- | --- | --- |
| `sg-ko-b3-001` | Korean lexical mismatch | Expected candidate missed; known B2 false positive retained. | Expected B3 candidate retained at rank 1. | This is the strongest fit for the paper: low vector similarity but high `r_i` relevance. |
| `sg-en-id-001` | English task-id lookup | Expected candidate already rank 1; extra candidate retained. | Expected candidate retained; extra low-reranker B2 candidate dropped. | Improvement is token/noise reduction rather than recall. |
| `sg-gov-001` | Primary source preference | Expected candidate retained but behind support-context B2 candidate. | Expected B1 candidate retained at rank 1; support-context B2 dropped. | Improves context precision and source authority. |
| `sg-stale-001` | Stale/deprecated demotion | Expected current policy retained but behind deprecated B2 candidate. | Current B1 candidate retained at rank 1; deprecated B2 dropped. | Fixture-level evidence for stale/noise suppression. This case uses a synthetic expected path not present in the live repo. |
| `sg-multi-001` | Broad/multi-hop Korean query | ScoreGate design candidate missed and required coverage incomplete. | Baseline report plus ScoreGate design retained; expected B3 candidate rank 2. | Keeps multi-hop coverage while still filtering unrelated B4 material. |

## Current Deployed Runtime Sanity

Current deployment:

- commit: `9b99c5f2672556d1495002aec0d06bd072553ac0`
- deployment authority: `personal-deploy`
- health: `UP`
- index status at test time: `documents=2085`, `chunks=29861`, `indexed=2052`, `removed=33`

Live `rag_search` checks against real current documents:

| Query Class | Expected Real Path | Live Rank | Notes |
| --- | --- | ---: | --- |
| Korean release-location style query | `design/retrieval-quality-improvement-design.md` | not in top 10 | Current runtime still misses this lexical mismatch case. ScoreGate runtime is not wired. |
| English task-id lookup | `tasks/T0010-codex-rag-utilization-hardening.md` | 1 | Current deterministic retrieval works well for identifier-heavy queries. |
| Source registry / SSOT query | `design/source-registry-and-project-ssot.md` | 1 | Current retrieval works well for explicit English design queries. |
| Broad Korean ScoreGate + baseline query | `design/retrieval-quality-improvement-design.md` and `reports/2026-05-25-retrieval-quality-baseline.md` | 4 and 6 | Current retrieval finds both within top 10, though not as compactly as a selector could make it. |

## Interpretation

ScoreGate's expected benefit for Local RAG is credible, but not yet realized in live runtime.

What improved in the released codebase:

- The algorithmic selector exists and is tested.
- Before/after fixture metrics can now be generated without changing source data or rebuilding the index.
- The fixture proves B3 rescue for the exact class of Korean/English lexical mismatch Local RAG struggles with.
- The fixture also proves B2/B4 filtering can reduce known context noise and known token count.

What did not improve yet:

- User-facing `rag_search` output is unchanged.
- Current `rerankScore` is still deterministic governance/source/path weighting, not cross-encoder relevance.
- The system still lacks true local cross-encoder `r_i`, so production thresholds cannot be calibrated.
- The live Korean release-location query still misses the desired source in top 10.

## Decision

Do not report P0003 as a live retrieval-quality improvement.

Report it as:

- a positive offline before/after selector result,
- a strong reason to pursue a local cross-encoder score-source proof,
- a non-runtime release that intentionally avoids unsafe threshold promotion.

## Follow-Up

The next evidence-producing task should be `T0024-local-cross-encoder-sidecar-proof`:

- add a local-only cross-encoder score source,
- collect real `s_i` and `r_i` snapshots from current Local RAG candidates,
- rerun this before/after comparison using real `r_i`,
- only then consider debug/audit or opt-in runtime ScoreGate.

## Status

- 2026-06-16: Before/after comparison run. No reindex or old-version redeploy was needed because the comparison operates on candidate snapshots. Current live runtime was sanity checked and confirmed not to include ScoreGate behavior.
