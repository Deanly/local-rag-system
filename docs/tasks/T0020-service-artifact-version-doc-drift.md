---
type: task
doc_id: T0020
title: service-artifact-version-doc-drift
status: done
owner:
created: 2026-06-16
updated: 2026-06-16
current_focus: "Service artifact documentation aligned with the 1.2.0 release line"
completion_mode: remediation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - pom.xml
  - services/README.md
  - services/api-gateway/Dockerfile
  - services/indexer-service/Dockerfile
  - services/mcp-bridge/Dockerfile
  - services/retrieval-service/Dockerfile
  - services/source-registry-service/Dockerfile
  - source:conversation/2026-06-16-project-review
quality_axes:
  - CONTRACT
  - HANDOFF
  - EVIDENCE
tags:
  - docs/task
  - local-rag-system
  - documentation
  - remediation
---

# T0020 service-artifact-version-doc-drift

- Type: task
- Document ID: T0020
- Status: done
- Completion Mode: remediation
- Owner:
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Service artifact documentation aligned with the 1.2.0 release line
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 service artifact version 문서가 실제 릴리즈 라인과 어긋난 문제를 닫는다.

리뷰 시점에 root `pom.xml`과 service Dockerfiles는 `1.1.0` artifact를 사용하지만 `services/README.md`는 여전히 `services/<service-name>/target/<service-name>-1.0.0.jar`를 expected artifact convention으로 설명했다. P0003 `1.2.0` release work에서 service README는 version-neutral artifact convention으로 바뀌었고, Maven/Docker artifact version은 `1.2.0`으로 정렬됐다.

## Task Placement Check

- 이 작업은 P0001/P0002 릴리즈 이후 남은 handoff 문서 drift remediation이다.
- 구현 변경이 아니라 operator-facing service documentation correction이므로 별도 project가 필요하지 않다.
- `P0001-local-rag-system` 아래 quick remediation task로 발급한다.

## Whole-System Anchor

이 task는 `docs/design/msa-runtime-and-storage.md`와 실제 Docker build artifact convention이 같은 릴리즈 버전을 말하게 한다.

깨면 안 되는 조건:

- root Maven version, Dockerfile artifact names, README handoff가 서로 같은 값을 말해야 한다.
- P0001 `1.0.0`, P0002 `1.1.0`, P0003 `1.2.0` release line의 역사적 구분은 보존해야 한다.
- 문서 수정과 실제 artifact version 변경은 같은 release evidence 안에서 일치해야 한다.

## Completion Mode Notes

Completion mode는 `remediation`이다. 이미 존재하는 release/documentation mismatch를 정리하고 검증하는 것이 terminal condition이다.

## Committed Outcome

`done` 상태에서 가능해야 하는 결과:

- `services/README.md` expected artifact convention이 current release artifact naming과 맞다.
- 필요한 경우 README가 version drift를 줄이는 방식으로 표현된다.
- `pom.xml`, service Dockerfiles, runtime design, service README 사이에 artifact version contradiction이 없다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | Service README artifact convention matches the current Maven/Docker artifact version. | `services/README.md` no longer hard-codes stale `1.0.0.jar` as the current expected artifact while Dockerfiles use `1.2.0.jar`. |
| G2 | Release lineage remains clear. | Documentation still distinguishes P0001 `1.0.0`, P0002 `1.1.0`, and P0003 `1.2.0` where relevant. |
| G3 | Documentation validators pass after the change. | Required docs validators pass. |

## Scope

- `services/README.md` correction
- Any directly related documentation wording if it repeats the stale artifact convention
- docs validators

## Out Of Scope

- Broad README rewrite
- Non-version-related service documentation cleanup

## References

- `pom.xml`
- `services/README.md`
- `docs/design/msa-runtime-and-storage.md`
- `services/*/Dockerfile`
- `docs/projects/P0002-retrieval-governance-hardening.md`

## Dependencies

- Current root Maven version is `1.2.0`.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Locate stale artifact version mentions | Done | 100% | Search found stale `1.0.0.jar` current-artifact wording in `services/README.md`. |
| W2 | Patch service documentation | Done | 100% | `services/README.md` now uses `<project-version>` convention and runtime design says `1.2.0`. |
| W3 | Run docs validators | Done | 100% | Docs validators passed during P0003 release verification. |

## Overall Progress

- 100%

## Completion Criteria

1. Current service artifact documentation matches `1.2.0`.
2. No nearby docs imply service Dockerfiles still use `1.0.0.jar`.
3. Documentation validators pass.

## Completion Evidence

Sufficient evidence:

- Diff showing corrected `services/README.md` wording.
- `rg '1.0.0.jar|1.1.0.jar|1.2.0' services docs README.md` review showing current artifact convention uses `1.2.0` or `<project-version>`.
- Docs validator output.

Insufficient evidence:

- Relying on root `pom.xml` alone while stale README text remains.
- Changing Dockerfiles without addressing documentation.

## Outputs / Handoff

- Corrected service handoff documentation.
- No runtime behavior change expected.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| CONTRACT | Build artifact paths are part of service handoff. | README and Dockerfiles agree. |
| HANDOFF | Future operators should not chase stale jar names. | Search evidence for stale current-artifact wording. |
| EVIDENCE | This is a small drift fix that should be directly provable. | Docs validators and targeted `rg` output. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | `services/README.md`; service Dockerfiles; root/service POM versions | Current artifact docs no longer point at `1.0.0.jar`; Dockerfiles use `1.2.0.jar`. |
| G2 | Done | README and project docs preserve P0001/P0002/P0003 release versions | Release lineage remains explicit. |
| G3 | Done | Docs validators passed during release verification | |

## Completion Guardrails

- Do not collapse P0001 and P0002 release history into a single version statement.
- Do not broaden into unrelated README cleanup.

## Risks / Open Questions

- Future releases may repeat this drift if artifact version is hard-coded in docs; consider wording the convention generically if that stays accurate.

## Status

- 2026-06-16: task 문서 생성. Review finding captured from mismatch between `services/README.md` and current `1.1.0` Maven/Docker artifact naming.
- 2026-06-16: task closed during P0003 `1.2.0` release preparation. Service artifact docs now use version-neutral convention and runtime artifact references are aligned to `1.2.0`.
