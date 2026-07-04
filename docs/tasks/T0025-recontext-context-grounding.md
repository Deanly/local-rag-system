---
type: task
doc_id: T0025
title: recontext-context-grounding
status: active
owner:
created: 2026-07-04
updated: 2026-07-04
current_focus: "ReContext-inspired answer evidence replay slice for retrieval-service"
completion_mode: functional
related_control_plane: docs/design/control-plane.md
related_umbrella_project: docs/projects/P0001-local-rag-system.md
related_project: docs/projects/P0001-local-rag-system.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/source-registry-and-project-ssot.md
  - docs/design/msa-runtime-and-storage.md
source_refs:
  - "ai-paper-product-fit-research:sources/papers/2607.02509-recontext/fulltext.md"
  - "ai-paper-product-fit-research:docs/research/papers/2607.02509-recontext/summary.md"
  - "ai-paper-product-fit-research:docs/research/papers/2607.02509-recontext/eval-card.md"
  - https://arxiv.org/abs/2607.02509
  - https://github.com/Yanjun-Zhao/ReContext
  - docs/design/control-plane.md
  - docs/design/local-rag-system-development-direction.md
  - docs/design/retrieval-quality-improvement-design.md
quality_axes:
  - WHOLE
  - SCOPE
  - HANDOFF
  - EVIDENCE
  - SECURITY
tags:
  - docs/task
  - local-rag-system
  - retrieval-quality
  - recontext
---

# T0025 recontext-context-grounding

- Type: task
- Document ID: T0025
- Status: active
- Completion Mode: functional
- Owner:
- Created: 2026-07-04
- Updated: 2026-07-04
- Current Focus: ReContext-inspired answer evidence replay slice for retrieval-service
- Related Control Plane: docs/design/control-plane.md
- Related Umbrella Project: docs/projects/P0001-local-rag-system.md
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Design:
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/retrieval-quality-improvement-design.md`
  - `docs/design/source-registry-and-project-ssot.md`
  - `docs/design/msa-runtime-and-storage.md`

## Purpose

이 task는 ReContext 논문의 "grounded evidence span을 선택해 질문 가까이에 replay하되 원본 context 접근을 보존한다"는 아이디어를 `local-rag-system`의 기존 answer path에 맞는 작은 제품 slice로 적용한다.

구현 목표는 모델 attention readout이나 논문 공식 코드 재현이 아니라, 검색 결과의 citation-bearing snippet을 grounded evidence replay block으로 중복 제거·순서화해 `/api/answer` prompt에서 먼저 재제시하는 것이다. 검색 API, source registry, indexing, ScoreGate default behavior는 바꾸지 않는다.

## Task Placement Check

- P0001 functional baseline은 이미 닫혔지만 `/api/answer`는 P0001의 local-only RAG answer surface에 속한다.
- 이 작업은 새 human-facing initiative가 아니라 retrieval answer context packing의 bounded improvement이므로 별도 `project`를 발급하지 않고 P0001 아래 task로 둔다.
- P0002/P0003의 retrieval-governance와 ScoreGate 선행 결정은 입력으로 읽되, 이 task는 local cross-encoder rollout이나 model reranker deployment를 재개하지 않는다.

## Whole-System Anchor

보존해야 하는 invariant:

- private source content는 hosted API로 전송하지 않는다.
- 등록된 source root와 검색 결과에 포함된 citation-bearing snippet만 answer prompt에 사용한다.
- `rag_search`/`/api/search` public contract, search modes, source registry resolution, Weaviate schema는 변경하지 않는다.
- ReContext 공식 코드가 현재 404이므로 코드 import나 training/evaluation 수치 주장을 제품 완료 근거로 쓰지 않는다.
- answer context 개선은 `retrieval-service` 내부 prompt construction에 한정해 rollback이 쉬워야 한다.

## Completion Mode Notes

Completion mode는 `functional`이다. 닫힌 상태는 `/api/answer`가 기존 검색 결과를 그대로 반환하면서, LLM에 보내는 answer prompt 안에 ReContext-inspired grounded evidence replay block을 포함하고 focused test가 그 prompt contract를 검증하는 것이다.

## Committed Outcome

- `retrieval-service` answer prompt가 검색 결과에서 grounded evidence snippets를 dedupe하고, citation과 source priority를 유지한 replay block을 질문 가까이에 배치한다.
- 기존 retrieved context block은 남겨 원본 검색 context 접근을 보존한다.
- API DTO, database schema, source registry, indexing, local reranker sidecar contract는 변경하지 않는다.
- Focused unit test가 replay block ordering, dedupe, citation preservation, no-context fallback을 검증한다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | ReContext 원문을 current source에서 읽고 `local-rag-system`에 맞는 minimal slice를 명시적으로 제한한다. | task source refs와 implementation notes가 attention readout/training/official code import를 제외하고 answer evidence replay slice를 잠근다. |
| G2 | `/api/answer` prompt에 grounded evidence replay block을 추가하되 검색·registry·indexing public contract를 변경하지 않는다. | retrieval-service focused tests가 prompt shape, citation, source priority, dedupe를 검증하고 existing search tests가 통과한다. |
| G3 | 문서 하네스와 검증 ladder를 통과해 후속 worker가 rollback/확장 판단을 할 수 있게 한다. | required validators, compose config, focused Java tests, `git diff --check` 결과가 Status에 기록된다. |

## Scope

- ReContext paper/source review and product slice translation.
- `retrieval-service` answer prompt context packing improvement.
- Focused Java unit tests for prompt construction.
- Task and active index updates required by docs harness.

## Out Of Scope

- ReContext official code import or reimplementation of model-internal attention readout.
- Model training, fine-tuning, KV-cache manipulation, custom decoding, or benchmark reproduction.
- `rag_search` result ranking changes, Weaviate schema changes, indexing changes, source registry changes, MCP tool contract changes.
- Deployment, operation-zone restart, worknote release note, or private source reindexing.

## Assumptions

- Research repo current `main` is the mandatory source-backed paper corpus for this task.
- Official `github.com/Yanjun-Zhao/ReContext` remains unavailable by HTTP 404 at issue time, so official-code behavior cannot be imported or claimed.
- Current `SearchResultItem.snippet()` is already a grounded, citation-bearing derived span from registered local sources.
- A prompt-only replay block is a conservative product adaptation of ReContext H3-style concept transfer, not a claim that the full paper method has been reproduced.

## Structural / Refactoring Plan

Work scale is small-to-medium. Blast radius is the `/api/answer` prompt only; `/api/search`, candidate ranking, ScoreGate selection, registry resolution, indexing, storage, and MCP request/response shapes are unchanged.

Maintainability plan:

- Reuse existing `SearchResultItem`, citation, snippet, and `sourcePriorityLine` data rather than introducing a new DTO.
- Keep replay formatting in small static helper methods near `answerPrompt` so future context-packing work has a readable local extension point.
- Deduplicate by normalized citation+snippet text to avoid repeated evidence without hiding the full retrieved context block.
- Preserve current retrieved context block as rollback and full-context access, matching ReContext's "emphasis, not exclusion" principle.

Migration and rollback risk:

- No schema, config, endpoint, or deployment migration.
- Rollback is removing the replay helper and reverting `answerPrompt` formatting.
- The only user-visible behavior change is answer generation quality/wording through different prompt context ordering.

Test plan:

- Unit tests assert replay block placement before retrieved context, citation preservation, source priority retention, snippet dedupe, and no-context fallback.
- Existing retrieval-service tests cover search mode validation, metadata parsing, ranking, ScoreGate opt-in/debug paths, audit support, and answer prompt source priority.

## References

- `ai-paper-product-fit-research:sources/papers/2607.02509-recontext/fulltext.md`
- `ai-paper-product-fit-research:docs/research/papers/2607.02509-recontext/summary.md`
- `ai-paper-product-fit-research:docs/research/papers/2607.02509-recontext/eval-card.md`
- `https://arxiv.org/abs/2607.02509`
- `https://github.com/Yanjun-Zhao/ReContext`
- `docs/design/control-plane.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/retrieval-quality-improvement-design.md`
- `docs/projects/P0001-local-rag-system.md`
- `docs/tasks/T0024-local-cross-encoder-sidecar-proof.md`

## Dependencies

- Clean `origin/main` task issuance and push must complete before feature branch development.
- Current Spring Boot retrieval-service test harness must run locally.
- No deployment or operation-zone access is required.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| W1 | Issue and push clean-main task doc | In Progress | 50% | `new-doc.sh` issued this task from clean `origin/main`; main commit/push pending |
| W2 | Review ReContext source and lock minimal product slice | In Progress | 70% | paper source and research summary reviewed; official code 404 verified |
| W3 | Implement answer evidence replay helper and prompt integration | Todo | 0% | feature branch only |
| W4 | Add focused retrieval-service tests | Todo | 0% | feature branch only |
| W5 | Run required validators and closeout updates | Todo | 0% | feature branch only |

## Overall Progress

- 20%

## Completion Criteria

1. Task issuance was committed and pushed on `main` before the feature branch was created.
2. Feature branch is based on the updated main commit containing this task doc.
3. `retrieval-service` answer prompt includes a ReContext-inspired replay block built only from retrieved, citation-bearing snippets.
4. Existing retrieved context remains present after the replay block.
5. Focused Java tests and required document/runtime validators pass or have exact blocker evidence.
6. No deployment or worknote release note is produced.

## Completion Evidence

- Main doc issuance commit hash and pushed branch evidence.
- Feature branch commit hash for implementation.
- Focused test command output for touched service.
- Validator outputs:
  - `./docs/bin/validate-codex-readiness.sh`
  - `./docs/bin/validate-harness-foundation.sh`
  - `./docs/bin/validate-doc-retrieval.sh`
  - `./docs/bin/validate-closeout.sh --all`
  - `docker compose --env-file .env.example config`
  - `git diff --check`

## Outputs / Handoff

- New task: `docs/tasks/T0025-recontext-context-grounding.md`
- Expected code surface: `services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java`
- Expected test surface: `services/retrieval-service/src/test/java/com/localrag/retrieval/RetrievalServiceTests.java`
- Follow-up only if future evidence justifies it: true model-internal ReContext evaluation with official code or a separately approved reimplementation task.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | The answer path must stay aligned with local-only RAG, citation, and source registry boundaries. | No hosted API, source registry, index, or public search contract change. |
| SCOPE | ReContext must not expand into training, attention readout, or broad architecture rewrite. | Out Of Scope and implementation diff stay limited to answer prompt packing. |
| HANDOFF | Future retrieval workers need to see why this is a prompt-only adaptation. | Source refs, assumptions, and structural plan explain the paper-to-product translation. |
| EVIDENCE | Improvement claims need runnable tests, not paper metrics transfer. | Focused tests plus required validators. |
| SECURITY | Private local snippets must remain within existing local answer generation path. | Prompt uses only existing retrieved snippets; no new external provider or source read path. |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | In Progress | ReContext source refs captured; official code 404 checked at issue time. | Full completion waits on implementation notes and final status. |
| G2 | Pending | | |
| G3 | Pending | | |

## Completion Guardrails

- Existing Purpose must not be reduced to a documentation-only review.
- If prompt replay cannot be implemented without changing search/ranking/source contracts, stop and record the blocker instead of broadening scope silently.
- Do not treat paper-reported benchmark gains as local product evidence.
- Do not deploy, restart operation-zone services, write worknote release notes, or index unregistered folders.

## Risks / Open Questions

- Prompt-only replay may improve grounding less than true attention-based ReContext; this task only ships the conservative, testable adaptation.
- If answer prompts become too long in real usage, a later task should add budget-aware context packing with evaluation fixtures.

## Status

- 2026-07-04: Issued from clean `origin/main` worktree with `./docs/bin/new-doc.sh task recontext-context-grounding`.
- 2026-07-04: Reviewed ReContext source corpus from `ai-paper-product-fit-research` current `main`; official code URL returned HTTP 404 through `gh api repos/Yanjun-Zhao/ReContext`.
