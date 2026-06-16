---
type: task
doc_id: T0018
title: source-root-symlink-boundary-hardening
status: draft
owner:
created: 2026-06-16
updated: 2026-06-16
current_focus: "Close symlink and real-path escape paths for registered source reads"
completion_mode: remediation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java
  - services/indexer-service/src/main/java/com/localrag/indexer/DocumentFetchService.java
  - services/indexer-service/src/main/java/com/localrag/indexer/SourcePathFilter.java
  - services/indexer-service/src/test/java/com/localrag/indexer/DocumentFetchServiceTests.java
  - source:conversation/2026-06-16-project-review
quality_axes:
  - SECURITY
  - CONTRACT
  - EVIDENCE
  - WHOLE
tags:
  - docs/task
  - local-rag-system
  - security
  - source-registry
  - remediation
---

# T0018 source-root-symlink-boundary-hardening

- Type: task
- Document ID: T0018
- Status: draft
- Completion Mode: remediation
- Owner:
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Close symlink and real-path escape paths for registered source reads
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 registered source root 밖 파일이 symlink 또는 real-path mismatch를 통해 indexing/search/document-fetch 경계 안으로 들어오는 문제를 닫는다.

리뷰에서 확인한 현재 위험은 lexical path check가 `..` traversal은 막지만 symlink target의 real path가 registered source root 밖인지 검증하지 않는다는 점이다. Local RAG의 핵심 security invariant는 등록된 source root만 읽는 것이므로, scanner와 `rag_get_document` 모두 같은 real-path boundary를 적용해야 한다.

## Task Placement Check

- 이 작업은 새 제품이나 별도 project가 아니라 P0001 functional baseline의 source registry security invariant remediation이다.
- P0002 retrieval governance 품질과 별개로, source boundary 자체가 깨지면 모든 indexing/search/answer 결과의 신뢰 경계가 약해진다.
- 따라서 기존 `P0001-local-rag-system` 아래의 보안 hardening task로 발급한다.

## Whole-System Anchor

이 task는 `docs/design/local-rag-system-development-direction.md`의 "원본 source folder는 read-only로 취급한다"와 "등록되지 않은 repository나 archive는 indexing/search 대상이 아니다" invariant를 보존한다.

깨면 안 되는 조건:

- registered source root 밖 파일은 indexing, chunking, Weaviate upsert, `rag_get_document` 어디에서도 읽히면 안 된다.
- include/exclude glob enforcement는 유지되어야 한다.
- source registry가 허용한 active source만 대상이어야 한다.
- 기존 정상 source 문서의 indexing/search/document fetch 계약은 backward-compatible해야 한다.

## Completion Mode Notes

Completion mode는 `remediation`이다. 이미 동작하는 source scanning/document fetch 기능의 보안 경계 결함을 닫고, 회귀 테스트로 닫힌 상태를 증명하는 것이 terminal condition이다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- scanner가 symlink file/directory 또는 real path escape를 index하지 않는다.
- `POST /api/documents/get`과 `rag_get_document`가 symlink escape 문서를 거부한다.
- root path 자체도 canonical/real path 기준으로 비교된다.
- path traversal, excluded path, symlink escape, 정상 registered file fetch가 모두 테스트된다.
- 운영 문서 또는 code comment가 필요한 경우 source-boundary 정책을 짧게 남긴다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Indexer scan path가 registered source root real path 밖 파일을 읽거나 index하지 않는다. | symlink file/directory escape fixture가 scan 후 `document_state`와 Weaviate chunk에 들어가지 않는 테스트 또는 runtime smoke가 있다. |
| G2 | Source-safe document fetch가 symlink escape와 real-path mismatch를 거부한다. | `DocumentFetchServiceTests`가 symlink escape를 400/403 계열로 검증하고 정상 파일 fetch는 유지한다. |
| G3 | Source path filtering, include/exclude, deletion cleanup 기존 계약이 회귀하지 않는다. | 기존 indexer tests와 Docker Maven test suite가 통과한다. |

## Scope

- `IndexerService` source traversal/read boundary hardening
- `DocumentFetchService` real-path containment hardening
- symlink escape regression tests
- existing path traversal/exclude tests 유지 또는 보강

## Out Of Scope

- multi-user permission model
- non-filesystem source provider support
- archive/historical retrieval ranking policy 변경
- source registry schema 대규모 변경

## References

- `docs/design/local-rag-system-development-direction.md`
- `docs/design/source-registry-and-project-ssot.md`
- `docs/design/msa-runtime-and-storage.md`
- `services/indexer-service/src/main/java/com/localrag/indexer/IndexerService.java`
- `services/indexer-service/src/main/java/com/localrag/indexer/DocumentFetchService.java`
- `services/indexer-service/src/test/java/com/localrag/indexer/DocumentFetchServiceTests.java`

## Dependencies

- P0001 functional baseline
- Existing source registry include/exclude contract

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Define real-path containment policy for scan and document fetch | Todo | 0% | Decide whether to reject all symlinks or allow symlinks whose target remains inside the registered root. |
| W2 | Harden `DocumentFetchService` | Todo | 0% | Use canonical/real path containment before opening files. |
| W3 | Harden `IndexerService` traversal/read path | Todo | 0% | Ensure scanner cannot index real-path escape files. |
| W4 | Add regression tests and run verification | Todo | 0% | Cover symlink file, symlink directory, traversal, excluded path, and normal fetch. |

## Overall Progress

- 0%

## Completion Criteria

1. A symlink inside a registered source pointing outside the source root cannot be fetched through `rag_get_document`.
2. The same symlink cannot be indexed by scan/watch fallback.
3. Normal registered files still index and fetch.
4. Exclude/include globs still apply after the real-path change.
5. Relevant Maven tests and document validators pass.

## Completion Evidence

Sufficient evidence:

- Focused unit tests for symlink escape rejection in document fetch and scan/index behavior.
- Full Docker Maven test pass.
- If runtime smoke is practical, a registered source fixture with an outside symlink does not produce searchable citations.

Insufficient evidence:

- Lexical `startsWith` checks without symlink tests.
- Manual assertion that Docker read-only mounts are enough.
- Passing existing path traversal tests only.

## Outputs / Handoff

- Hardened source-boundary implementation.
- Regression tests that future source registry changes must keep green.
- Short implementation note if the chosen policy is "reject all symlinks" vs "allow only internal symlinks."

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| SECURITY | Registered source roots are the private-content boundary. | Symlink escape tests prove outside files are not read. |
| CONTRACT | `rag_get_document` advertises source-safe reads. | Fetch contract tests cover positive and negative paths. |
| EVIDENCE | The risk is filesystem-behavior specific. | Tests must construct actual filesystem symlinks when supported. |
| WHOLE | Retrieval quality is invalid if source admission is unsafe. | Scanner and fetch paths share the same boundary policy. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Pending | | |
| G2 | Pending | | |
| G3 | Pending | | |

## Completion Guardrails

- Do not close this task by documenting a limitation while leaving symlink escape possible.
- Do not harden only `rag_get_document`; scanner/indexing must be covered too.
- Do not remove include/exclude enforcement while changing path checks.
- Do not rely on hosted or external services for verification.

## Risks / Open Questions

- Filesystem symlink behavior may differ across macOS, Linux containers, and Docker bind mounts; tests should be explicit about skipped cases if symlink creation is unavailable.
- The implementation must choose whether internal symlinks are allowed or all symlinks are rejected for simplicity.

## Status

- 2026-06-16: task 문서 생성. Review finding captured from code inspection of `IndexerService`, `DocumentFetchService`, and existing document fetch tests.
