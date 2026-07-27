---
type: task
doc_id: T0019
title: indexer-failure-job-state-persistence
status: draft
owner:
created: 2026-06-16
updated: 2026-07-27
current_focus: "Persist scan/index/delete failures and job lifecycle evidence"
completion_mode: remediation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java
  - services/indexer-service/src/main/java/com/localrag/indexer/IndexerAutomation.java
  - database/postgres/ddl/001_core_schema.sql
  - services/indexer-service/README.md
  - source:conversation/2026-06-16-project-review
quality_axes:
  - EVIDENCE
  - HANDOFF
  - CONTRACT
  - WHOLE
tags:
  - docs/task
  - local-rag-system
  - indexer
  - observability
  - remediation
---

# T0019 indexer-failure-job-state-persistence

- Type: task
- Document ID: T0019
- Status: draft
- Completion Mode: remediation
- Owner:
- Created: 2026-06-16
- Updated: 2026-07-27
- Current Focus: Persist scan/index/delete failures and job lifecycle evidence
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 indexer failure와 job lifecycle이 PostgreSQL operational truth에 남지 않는 문제를 닫는다.

현재 DDL은 `index_job`과 `failure_record`를 정의하고, index status는 `index_job` status count를 읽는다. 하지만 `IndexerService.scan`은 실패를 response `errors`에만 누적하고 scheduled scan은 그 response를 버린다. 이 상태에서는 periodic scan, watcher-triggered scan, embedding/upsert/read 실패가 운영자가 볼 수 있는 durable evidence로 남지 않는다.

## Task Placement Check

- 이 작업은 P0001의 runtime/storage contract remediation이다.
- 새로운 retrieval quality project가 아니라 기존 indexer operational truth와 status API의 신뢰도를 복구하는 작업이다.
- 별도 project가 아니라 `P0001-local-rag-system` 아래 task로 발급한다.

## Whole-System Anchor

이 task는 `docs/design/local-rag-system-development-direction.md`의 "실패는 조용히 삼키지 않고 `FailureRecord`와 index status에 남긴다" invariant와 `docs/design/msa-runtime-and-storage.md`의 PostgreSQL control store 계약을 보존한다.

깨면 안 되는 조건:

- scan/watch/index/delete 실패는 durable state에 남아야 한다.
- status API는 운영자가 실패를 발견할 수 있는 충분한 정보를 제공해야 한다.
- source content 자체를 failure/audit table에 저장하면 안 된다.
- Weaviate derived index cleanup은 idempotent해야 하지만, 반복 실패는 보이지 않게 숨기면 안 된다.

## Completion Mode Notes

Completion mode는 `remediation`이다. 이미 존재하는 DDL/API 계약과 실제 runtime behavior의 mismatch를 닫고, 운영자가 실패를 진단할 수 있는 상태를 terminal condition으로 삼는다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- `scan-source`, `index-file`, `delete-file`, `force-reindex` 중 구현된 lifecycle이 `index_job`에 기록된다.
- file read, parse/chunk, embedding, Weaviate upsert/delete, source walk 실패가 `failure_record`에 기록된다.
- retryable/terminal 성격과 phase/error code/message가 private content 없이 남는다.
- `GET /api/index/status`가 실패 상태를 발견할 수 있는 count 또는 summary를 반환한다.
- scheduled scan과 watcher-triggered scan에서도 실패 evidence가 버려지지 않는다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Indexer job lifecycle is persisted for operator-visible scan/index/delete work. | Relevant scan/index/delete paths insert/update `index_job`, and status tests verify non-empty job counts. |
| G2 | Failure records are created for read, scan, embedding, and Weaviate phases without storing private content. | Focused tests simulate failures and assert `failure_record` rows with phase/error fields and no raw snippet content. |
| G3 | Scheduled/watch scans do not discard operational failure evidence. | Automation path calls produce durable failure/job evidence or delegates to a service method that does. |
| G4 | Index status remains backward-compatible while exposing enough failure information. | Existing status consumers still work, and new failure/job counts are covered by tests or smoke output. |

## Scope

- `IndexerService` job/failure persistence
- `IndexerAutomation` scheduled/watch handoff behavior where needed
- `IndexStatusResponse` extension if required
- focused tests around simulated failures
- README/design/task updates if public status contract changes

## Out Of Scope

- Full distributed job queue
- Async worker pool redesign
- User-facing dashboard UI
- Storing private file content or snippets in failure tables
- Replacing PostgreSQL schema

## References

- `docs/design/local-rag-system-development-direction.md`
- `docs/design/msa-runtime-and-storage.md`
- `database/postgres/ddl/001_core_schema.sql`
- `services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java`
- `services/indexer-service/src/main/java/com/localrag/indexer/IndexerAutomation.java`

## Dependencies

- Existing PostgreSQL DDL tables: `index_job`, `failure_record`, `document_state`, `chunk_state`
- Existing status API shape in `common/src/main/java/com/localrag/common/dto/IndexStatusResponse.java`

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Define minimal job/failure lifecycle mapping | In Progress | 50% | Added narrow file-level failure phase/code mapping for the current synchronous scanner. Full job lifecycle mapping remains open. |
| W2 | Persist jobs and failures in `IndexerService` | In Progress | 35% | Nonfatal file indexing failures now write failed `document_state` and `failure_record` evidence. Source walk, delete cleanup, and job records remain open. |
| W3 | Expose operator-visible status | Todo | 0% | Existing grouped document status can now surface `failed`; explicit failure/job summaries remain open. |
| W4 | Add focused failure tests | In Progress | 40% | Added unit coverage for invalid frontmatter tolerance and nonfatal file index failure persistence. |
| W5 | Run verification and update docs if contract changes | In Progress | 75% | Module tests, compose config, smoke script syntax, and document validators pass for this partial remediation. |

## Overall Progress

- 35%

## Completion Criteria

1. `index_job` no longer stays empty for implemented scan/index/delete work.
2. `failure_record` records failures that currently only appear in a transient `ScanResponse.errors`.
3. Scheduled scan failures remain discoverable after the scheduled method returns.
4. Failure records do not include private source snippets or credentials.
5. Tests and validators pass.

## Completion Evidence

Sufficient evidence:

- Unit/integration tests proving job/failure rows are written for representative success and failure paths.
- Status API smoke or test showing operator-visible counts.
- Docker Maven test pass and `docker compose --env-file .env.example config` pass.

Insufficient evidence:

- DDL presence alone.
- Logging only.
- Manual curl of `/api/index/status` when no failing path was exercised.

## Outputs / Handoff

- Durable indexing observability baseline.
- Clear phase/error-code conventions for future retry policy work.
- Updated status contract documentation if response shape changes.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| EVIDENCE | Operators need persistent proof of failures. | `index_job` and `failure_record` test evidence. |
| HANDOFF | Future retry/doctor tooling depends on durable failure records. | Phase/error mapping is documented in code or docs. |
| CONTRACT | Status API currently implies job counts are meaningful. | Status response tests or smoke prove the contract. |
| WHOLE | Freshness and deletion guarantees depend on seeing failed work. | Scan/watch paths no longer discard failure evidence. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Pending | | Full `index_job` lifecycle persistence remains open. |
| G2 | In Progress | `IndexerServiceTests.recordsNonFatalFileIndexFailuresAndContinuesScan` | Covers nonfatal file indexing failure persistence without raw source content. |
| G3 | Pending | | |
| G4 | In Progress | Existing document status grouping plus failed document state | Explicit status contract extension remains open. |

## Completion Guardrails

- Do not close this task with logs-only observability.
- Do not store raw document content, snippets, credentials, or private host paths beyond existing relative/source identifiers.
- Do not expand this into a full async queue unless the minimal synchronous lifecycle cannot satisfy the goals.
- Do not remove existing deletion cleanup behavior while adding job records.

## Risks / Open Questions

- The current synchronous scanner may not need a full job queue, but it still needs durable records. The implementation should avoid overbuilding.
- Weaviate delete failures are currently intentionally quiet for idempotence; this task must decide how to record repeated cleanup failures without making idempotent missing-object deletes noisy.

## Status

- 2026-07-27: Incident-driven partial remediation implemented. Missing local chat model during `rag_answer` now returns a structured 503 instead of an unhandled 500. Invalid markdown frontmatter is marked `status=invalid` and the body remains indexable. Nonfatal per-file indexing failures continue the source scan and write failed `document_state` plus `failure_record` evidence without storing raw source content. Smoke can now opt into `rag_answer` and optionally tolerate local profiles where answer generation is intentionally unavailable.
- 2026-07-27: Verification passed for partial remediation: `mvn -q -pl services/indexer-service,services/retrieval-service -am test`, `node --check integrations/codex/smoke-local-rag.mjs`, `docker compose --env-file .env.example config`, `git diff --check`, and document harness/closeout validators.
- 2026-07-27: Device application completed. Docker build context now excludes local env and local registry files. The local stack was rebuilt/recreated, the device-local chat model env was applied outside tracked files, a registered project force scan completed with no errors, and Codex smoke passed with `rag_answer`.
- 2026-06-16: task 문서 생성. Review finding captured from mismatch between DDL/status design and current `IndexerService` failure handling.
