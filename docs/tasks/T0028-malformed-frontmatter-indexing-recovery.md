---
type: task
doc_id: T0028
title: malformed-frontmatter-indexing-recovery
status: active
owner: Codex
created: 2026-09-13
updated: 2026-09-13
current_focus: 잘못된 YAML 헤더로 중단되는 원문 색인 복구
completion_mode: remediation
related_control_plane: docs/design/control-plane.md
related_umbrella_project: P0001-local-rag-system
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - services/indexer-service/src/main/java/com/localrag/indexer/MarkdownChunker.java
  - services/indexer-service/src/test/java/com/localrag/indexer/MarkdownChunkerTests.java
quality_axes: [WHOLE, GOAL, EVIDENCE]
tags: [docs/task, indexing]
---

# T0028 malformed-frontmatter-indexing-recovery

- Type: task
- Document ID: T0028
- Status: active
- Completion Mode: remediation
- Owner: Codex
- Created: 2026-09-13
- Updated: 2026-09-13
- Current Focus: 잘못된 YAML 헤더로 중단되는 원문 색인 복구
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: P0001-local-rag-system
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design: source-registry-and-project-ssot; msa-runtime-and-storage

## Purpose

등록된 원문의 `current_focus: Confirmed: ...` 같은 인용되지 않은 콜론 때문에 YAML 파서가 예외를 던지고, 해당 저장소의 뒤쪽 문서 수집까지 중단된다. 사용자 요청으로 전체 판단용 소스를 등록하던 중 실제 문서에서 재현했다. 원문을 수정하지 않고 색인기가 형식 오류를 처리하도록 복구한다.

## Task Placement Check

기존 P0001의 등록 원문 읽기·청킹 기능을 복구하는 한 가지 작업이다. 별도 프로젝트나 제품 정책을 만들지 않는다.

## Whole-System Anchor

등록 소스의 읽기 전용 경계, 기존 모델·인증·벡터 계약, 정상 YAML의 authority/status 의미를 유지한다.

## Completion Mode Notes

`remediation`이다. 잘못된 헤더의 원문 보존, 보수적 메타데이터 기본값, 정상 문서 회귀와 운영 적용 증거가 종료 조건이다.

## Committed Outcome

YAMLException이 발생한 문서는 헤더를 포함한 원문 전체를 일반 텍스트로 청킹한다. 잘못된 헤더의 status/authority는 신뢰하지 않고 기존 unknown/source-default 규칙을 사용한다. 경고는 문서 경로만 남기고 YAML 내용과 예외 본문을 출력하지 않는다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | 원문 손실 없는 오류 복구 | 잘못된 YAML과 문서 본문이 청크에 보존되는 테스트 통과 |
| G2 | 정상 문서 의미 보존 | 정상 metadata/authority와 공통·서비스 회귀 검사 통과 |
| G3 | 실제 적용 근거 | 버전 고정 운영 배포와 malformed 문서 색인 확인 |

## Scope

- Markdown frontmatter 파싱의 YAMLException 처리
- 원문과 보수적 메타데이터 기본값에 대한 회귀 테스트
- 문서 계약과 버전 고정 운영 검증

## Out Of Scope

원문 문서·정책·검증 receipt 수정, 임베딩 모델/인증 변경, index_job 저장 기능, 전체 파서 재설계.

## References

- `docs/design/source-registry-and-project-ssot.md`
- `docs/design/msa-runtime-and-storage.md`

## Dependencies

현재 v1.3.1 baseline, 기존 query/bulk 서비스 설정과 등록 원문.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | YAML 오류 복구와 회귀 검사 | Done | 100% | Maven 51 tests, 실제 오류 문서 24개/506 chunks 복구 |
| W2 | 운영 적용과 실제 색인 근거 | Pending | 0% | 버전 고정 배포 후 확인 |

## Overall Progress

- 구현 및 검증 진행 중. 전체 소스의 최초 색인은 별도 운영 작업으로 계속 진행한다.

## Completion Criteria

G1–G3를 모두 증거로 검증한다. 원문이나 모델 정책 변경으로 오류를 숨기지 않는다.

## Completion Evidence

Maven 결과, 문서 validators, 정확한 배포 버전과 실제 문서 색인 상태.

## Outputs / Handoff

운영 원문은 그대로 사용한다. YAML 복구 경고는 원문 정비 후보를 알려주며 완료·승인 상태를 새로 부여하지 않는다.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | 여러 저장소 문서 수집 연속성 | 실제 malformed 문서 복구 |
| GOAL | 원문 보존 | 헤더·본문 청크 검증 |
| EVIDENCE | 정상 계약 보존 | 전체 테스트와 운영 근거 |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Done | malformed 원문 테스트와 실제 오류 문서 24개/506 chunks 파싱 통과 | 헤더·본문 보존, unknown/source-default |
| G2 | Done | Maven 51 tests, failures/errors/skipped 0; 문서 validators 통과 | 정상 metadata·인증·publication 검사 포함 |
| G3 | Pending | 운영 적용 대기 | 새 버전 |

## Completion Guardrails

원문·정책·모델·인증을 바꾸지 않는다. 정상 YAML 파싱 결과를 재해석하지 않는다.

## Risks / Open Questions

잘못된 YAML에서만 metadata가 unknown/source-default로 대체된다. 해당 원문의 명시적 상태가 필요하면 직접 읽어 확인한다.

## Status

- 2026-09-13: 사용자 RAG 전면 적용 요청의 실제 색인 장애 복구로 발급했다.

- 2026-09-13: Maven 51개 테스트와 문서 검증 통과. 실제 DeepMusic 오류 문서 24개가 506개 청크로 변환되며 보수적 metadata를 유지함을 읽기 전용 probe로 확인했다. 운영 적용은 아직 진행 중이다.
