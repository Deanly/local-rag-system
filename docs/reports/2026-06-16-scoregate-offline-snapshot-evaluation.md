---
type: report
title: scoregate-offline-snapshot-evaluation
status: done
owner: Codex
created: 2026-06-16
updated: 2026-06-16
current_focus: "Selector-level ScoreGate offline snapshot evaluation before local cross-encoder score sourcing"
report_type: scoregate-offline-snapshot-evaluation
related_project: docs/projects/P0003-scoregate-adaptive-context-selection.md
related_task:
  - docs/tasks/T0021-scoregate-offline-selector-experiment.md
  - docs/tasks/T0022-scoregate-offline-evaluation-fixture.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
source_refs:
  - https://arxiv.org/abs/2606.14269
  - ~/Workspace/personal-assistant-wiki/inbox/2026-06-15-agent-system-paper-scrap.md
  - docs/reports/2026-05-25-retrieval-quality-baseline.md
  - docs/evaluation/scoregate-offline-cases.json
  - docs/bin/validate-scoregate-offline.sh
  - services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java
  - services/retrieval-service/src/test/java/com/localrag/retrieval/ScoreGateOfflineEvaluationTests.java
tags:
  - docs/report
  - local-rag-system
  - retrieval-quality
  - scoregate
---

# scoregate-offline-snapshot-evaluation

- Type: report
- Status: done
- Owner: Codex
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Selector-level ScoreGate offline snapshot evaluation before local cross-encoder score sourcing
- Report Type: scoregate-offline-snapshot-evaluation
- Related Project: `docs/projects/P0003-scoregate-adaptive-context-selection.md`
- Related Task:
  - `docs/tasks/T0021-scoregate-offline-selector-experiment.md`
  - `docs/tasks/T0022-scoregate-offline-evaluation-fixture.md`
- Related Design:
  - `docs/design/retrieval-quality-improvement-design.md`

## Summary

T0022 added a repeatable offline ScoreGate snapshot evaluator. It verifies the selector contract and metric shape needed for the next phase, but it is not production calibration because `r_i` is still a normalized snapshot value rather than a real local cross-encoder score.

Snapshot result:

| Metric | Value |
| --- | ---: |
| cases | 5 |
| hit@1 | 80.0% |
| hit@5 | 100.0% |
| MRR | 0.900 |
| source accuracy@1 | 100.0% |
| fixed top-K miss rescue rate | 40.0% |
| multi-hop coverage | 100.0% |
| retained token estimate | 1,460 |
| fixed top-K token estimate | 1,810 |
| B3 rescue count | 2 |

The useful finding is not the absolute quality number; the fixture is too small for that. The useful finding is that Local RAG now has a concrete, runnable offline contract for B3 rescue, stale/B2 discard, fixed top-K comparison, and multi-hop coverage before any runtime behavior changes.

## Scope

Included:

- selector-level offline snapshots,
- Korean lexical mismatch case,
- English identifier case,
- primary-source preference case,
- stale/deprecated demotion case,
- broad/multi-hop coverage case,
- retained token estimate and fixed top-K comparison.

Excluded:

- real local cross-encoder model execution,
- threshold calibration from corpus logs,
- runtime `rag_search` integration,
- production default behavior changes.

## Inputs

Command:

```bash
LOCAL_RAG_SCOREGATE_OUTPUT=/tmp/local-rag-scoregate-offline-report.json ./docs/bin/validate-scoregate-offline.sh
```

Fixture:

- `docs/evaluation/scoregate-offline-cases.json`

Selector:

- `services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java`

Reference config used by the fixture:

| Parameter | Value |
| --- | ---: |
| `tau_s` | 0.70 |
| `tau_r` | 0.08 |
| `alpha` | 0.30 |
| `theta_B2` | 0.255 |
| `theta_B3` | 0.15 |
| `MAX-K` | 10 |

These are paper reference values for selector contract testing only. They are not Local RAG production thresholds.

## Findings

### F1. B3 Rescue Is Now A Testable Local Contract

The Korean lexical mismatch and broad/multi-hop cases both retain an expected low-similarity/high-reranker candidate as B3. In both cases, fixed top-K would miss the expected candidate under the snapshot ranks.

### F2. B2 Drop Expectations Are Numerically Guarded

The fixture includes high-similarity/low-reranker candidates that must be dropped because their fusion scores stay below the stricter B2 threshold. This prevents future regressions where lexical overlap is allowed to override cross-encoder rejection too easily.

### F3. Multi-Hop Coverage Is Explicit

The broad case requires both the retrieval-quality design and the 2026-05-25 baseline report to be retained. This keeps token reduction from becoming the only optimization objective.

### F4. Snapshot Evidence Is Not Calibration Evidence

The fixture uses normalized snapshots to prove evaluator shape and selector behavior. It does not answer whether Local RAG should run ScoreGate at runtime because the system still needs a local cross-encoder score source and corpus-specific threshold calibration.

## Recommendations

1. Treat the fixture schema as the handoff contract for future real `r_i` score snapshots.
2. Re-run this evaluator with real local cross-encoder scores before any debug/audit or runtime opt-in mode.
3. Add generated calibration reports only after score sourcing is real enough to compare thresholds.
4. Use the T0023 no-ship decision as the current runtime boundary.

## Follow-Up Promotion

- P0003 later closed runtime rollout as no-ship in T0023 because the current profile has no true local cross-encoder score source.
- `docs/design/retrieval-quality-improvement-design.md` should describe ScoreGate as offline-first/no-ship for the current profile until a future local cross-encoder proof exists.

## Status

- 2026-06-16: Report created from T0022 offline snapshot validator output.
- 2026-06-16: Follow-up updated after T0023 runtime no-ship decision.
