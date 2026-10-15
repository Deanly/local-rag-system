---
type: project
doc_id: P0005
title: unified-grounding-gateway
status: draft
project_role: exception-branch
umbrella_initiative: unified-grounding-gateway
parent_umbrella_project: P0001-local-rag-system
completion_mode: functional
owner: Dean (pending human lock)
created: 2026-06-18
updated: 2026-06-18
current_focus: "Decouple search/grounding from the reasoning model into one MCP-compatible Unified Grounding Gateway with a governed output contract, then harden it with ingestion sanitizing, forgetting tests, redaction eval, and a runtime compliance monitor."
related_control_plane: docs/design/control-plane.md
related_design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - "arXiv:2606.18947 Decoupling Search from Reasoning: A Vendor-Agnostic Grounding Architecture for LLM Agents (submitted 2026-06-17) https://arxiv.org/abs/2606.18947"
  - "arXiv:2606.18829 GateMem: Benchmarking Memory Governance in Multi-Principal Shared-Memory Agents (submitted 2026-06-17) https://arxiv.org/abs/2606.18829"
  - "arXiv:2606.19235 CodeSentinel: A Three-Layer Defense Against Indirect Prompt Injection in Code Contexts (submitted 2026-06-17) https://arxiv.org/abs/2606.19235"
  - "arXiv:2606.19242 Runtime Compliance Verification for AI Agents (submitted 2026-06-17) https://arxiv.org/abs/2606.19242"
  - "arXiv:2606.18782 RedactionBench (submitted 2026-06-17) https://arxiv.org/abs/2606.18782"
  - "arXiv:2606.19063 PYPILINE: Malicious PyPI Package Detection via Suspicious API Knowledge and Agent Workflow (submitted 2026-06-17) https://arxiv.org/abs/2606.19063"
  - "arXiv:2606.19191 PhantomSkill: Malicious Code Injection in Agent Skill Ecosystems (submitted 2026-06-17) https://arxiv.org/abs/2606.19191"
  - docs/design/retrieval-quality-improvement-design.md
  - docs/projects/P0002-retrieval-governance-hardening.md
  - docs/projects/P0003-scoregate-adaptive-context-selection.md
quality_axes:
  - WHOLE
  - SCOPE
  - HANDOFF
tags:
  - docs/project
  - local-rag-system
  - grounding-gateway
  - mcp
  - security
---

# P0005 unified-grounding-gateway

- Type: project
- Document ID: P0005
- Status: draft
- Project Role: exception-branch
- Umbrella Initiative: unified-grounding-gateway
- Parent Umbrella Project: P0001-local-rag-system
- Completion Mode: functional
- Owner: Dean (pending human lock)
- Created: 2026-06-18
- Updated: 2026-06-18
- Current Focus: Decouple search/grounding from the reasoning model into one MCP-compatible Unified Grounding Gateway with a governed output contract, then harden it with ingestion sanitizing, forgetting tests, redaction eval, and a runtime compliance monitor.
- Related Control Plane: docs/design/control-plane.md
- Related Design:
  - docs/design/retrieval-quality-improvement-design.md
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md

## Source Verification Note

7개 참고 논문은 본 문서 작성 시점(2026-06-18)에 arXiv abstract 페이지를 직접 조회해 제목과 제출일을 확인했다. 7편 모두 `citation_date` = `2026/06/17`이며 제목이 요청서와 정확히 일치한다. 각 논문의 abstract 핵심 주장은 아래 `Security Model`/`Decision Summary`에 근거로 인용했다. (조회 방식: `https://arxiv.org/abs/<id>` HTTP 200, abstract 본문 파싱.)

## Purpose

Silverstone/local-rag 계열 에이전트의 **grounding(검색·근거 주입)을 reasoning model 밖으로 분리**하여, 단일 MCP-compatible **Unified Grounding Gateway**로 통합한다. 현재는 검색 정책, provider 선택, evidence 주입, cost/latency, 출력 형태가 모델-프로바이더 경계 뒤에 묶여 있어 검사·튜닝·재사용·이식이 어렵고, 엄격한 출력 계약을 깨는 Search-Induced Verbosity가 발생한다(arXiv:2606.18947). 이 project는 (1) 검증 가능한 출력 계약을 강제하는 게이트웨이를 1차로 세우고, (2) ingestion sanitizing·forgetting·redaction·runtime compliance를 2차로 얹어, 모든 에이전트가 동일한 근거 표면을 통해 안전하게 검색하도록 만든다.

게이트웨이의 모든 응답은 다음 출력 계약을 기본으로 한다.

```json
{ "answer_context": "...", "source_refs": [...], "visibility_scope": "...", "freshness": {...}, "risk_flags": [...] }
```

## Umbrella Lineage

- `Project Role: exception-branch`. Parent umbrella는 `P0001-local-rag-system`.
- P0002(검색 거버넌스), P0003(ScoreGate 선택), P0004(임베딩 모델 평가)가 각각 retrieval의 한 축을 다룬 것과 동격으로, 이 project는 retrieval을 **소비하는 경계(gateway)와 그 위의 안전·컴플라이언스 계층**을 새 delivery boundary로 소유한다. 기존 retrieval-service를 호출하되, 그 위에 provider routing·계약·캐시·정책 enforcement라는 별도 lifecycle을 가지므로 P0001의 단일 task로 묶지 않는다.

## Project Issuance Check

- 이 문서는 human-facing owner를 잠그는 surface이므로 `status: draft`로 발급했다. Silverstone 위임 요청(`idempotencyKey: df5f04c0-4790-4d84-adf5-471ef203b145`, `approvalMode: MANUAL_REVIEW`) 하에 에이전트가 초안을 준비했고, Dean의 명시 승인으로 `active` 전환과 owner lock을 기다린다.
- 새 `project`여야 하는 이유: (a) 새로운 런타임 표면(MCP grounding gateway)과 새 출력 계약을 도입하고, (b) 2차에서 sanitizer/forgetting/redaction/compliance라는 독립 평가·게이트가 필요하며, (c) 1차/2차로 나뉜 ship 결정 lifecycle을 가진다. 이는 P0001 단일 task의 범위를 넘는다.
- 예외 분기 project 근거: 기존 retrieval/governance 자산(P0002/P0003)을 재사용하지만, "검색을 모델 밖 게이트웨이로 분리"라는 아키텍처 결정은 human 입장에서 별도 project로 추적하는 편이 훨씬 이해하기 쉽다.

## Whole-System Anchor

- delivery boundary: "모든 grounding 응답은 검증 가능한 출력 계약(`source_refs`·`visibility_scope`·`freshness`·`risk_flags` 포함)을 만족한다"는 불변을 새로 소유한다.
- control-plane invariant 보존: 단일 embedding model/dimension per Weaviate class(P0004), source registry 등록 외 인덱싱 금지(`source-registry-and-project-ssot`), local-only Ollama 사용. 게이트웨이는 이 불변을 깨지 않고 그 위에서 동작한다.
- 깨면 안 되는 handoff: 기존 `retrieval-service`/`mcp-bridge`의 현 동작은 게이트웨이 도입 동안에도 default로 유지되며, 게이트웨이는 opt-in 경로로 먼저 들어온다.

## Completion Mode Notes

- `functional`. 이 project가 닫히려면 게이트웨이가 실제로 동작하고(1차) 안전 계층이 검증 게이트를 통과(2차)해야 한다.
- closed state: 1차 게이트웨이가 계약을 강제한 채 로컬에서 동작하고, 2차 보안 계층이 각자의 acceptance test를 통과해 ship/defer 결정이 리포트로 고정된 상태.

## Committed Outcome

- Phase 1: provider-routing·citation rendering·exact+semantic cache·scope/privacy filter를 갖춘 MCP-compatible grounding gateway가 출력 계약을 강제한 채 로컬에서 동작한다.
- Phase 2: ingestion sanitizer, deletion/forgetting test suite, prompt-injection regression set, contextual redaction 평가, runtime trace monitor가 각각 acceptance criteria를 충족하거나, 충족 못 한 축은 defer 근거와 함께 기록된다.

## Goal Inventory

| Goal ID | Locked Goal | Done When |
| --- | --- | --- |
| G1 | grounding을 모델 밖 MCP-compatible gateway로 분리하고 출력 계약을 강제 | 게이트웨이가 `{answer_context, source_refs, visibility_scope, freshness, risk_flags}`를 모든 응답에 강제하고, 계약 위반(Search-Induced Verbosity 포함)이 regression test에서 0건 |
| G2 | provider routing + fallback + retrieval-depth + exact/semantic cache를 1급 제어로 노출 | 동일 질의가 provider/depth/cache 설정에 따라 결정적으로 라우팅되고, 캐시 hit/miss·cost·latency가 응답 메타로 노출됨 |
| G3 | visibility scope/privacy filter를 retrieval의 1급 조건으로 적용 | owner/family/project/assistant-private/public scope가 retrieval 필터로 강제되고, scope 위반 leak이 GateMem-style 테스트에서 0건 |
| G4 | repo/code/docs ingestion sanitizer로 indirect prompt injection 노드 격리 | CodeSentinel-style sanitizer가 prompt-injection regression set에서 목표 node-level 탐지율을 달성하고, 격리/중화 전 위험 노드가 model-facing context에 미도달 |
| G5 | deletion/forgetting + contextual redaction + runtime compliance 검증 | 삭제 요청 후 retrieval 비노출(active forgetting) 테스트 통과, RedactionBench-style redaction 품질 지표 기록, C-Trace-style runtime monitor가 non-compliant tool call을 reject |

## Output Contract

기본 계약(요청서 고정):

```json
{
  "answer_context": "string  // 모델에 주입될 렌더된 근거 블록(인용 마커 포함)",
  "source_refs": [
    {
      "ref_id": "string",
      "source_type": "local_rag|wiki|repo_docs|repo_code|web|arxiv|vendor_docs",
      "uri": "string",
      "title": "string",
      "locator": "string  // chunk_id, line range, anchor",
      "score": 0.0,
      "indexed_at": "ISO-8601",
      "source_mtime": "ISO-8601|null"
    }
  ],
  "visibility_scope": "owner|family|project|assistant_private|public",
  "freshness": {
    "as_of": "ISO-8601",
    "max_source_age": "duration",
    "staleness": "fresh|aging|stale",
    "ttl": "duration|null"
  },
  "risk_flags": ["injection_suspected","redacted_pii","supply_chain_review","scope_downgraded","low_confidence","cache_stale"]
}
```

확장 제안(필드는 1차에서 optional, 2차에서 일부 required 승격):

- `trace_id` / `request_id`: runtime compliance trace 상관키(C-Trace, arXiv:2606.19242).
- `provider`: 실제 사용된 검색 provider와 `fallback_chain`.
- `retrieval`: `{ depth, k, fused, rerank_used }` — retrieval-depth 제어 가시화.
- `cache`: `{ tier: exact|semantic|none, hit: bool, key }`.
- `confidence`: 0–1 (GateMem confidence metadata와 정렬).
- `cost`: `{ tokens, latency_ms, provider_calls }`.
- `compliance`: `{ decision: allow|reject|redacted, policy_ids: [...] }`.
- `redaction`: `{ applied: bool, r_score: float|null, spans: int }` (RedactionBench R-Score, arXiv:2606.18782).
- `sanitizer`: `{ nodes_scanned, nodes_neutralized, families: [...] }` (CodeSentinel, arXiv:2606.19235).

## Input Source Scope

- `local_rag`: 기존 Weaviate hybrid 인덱스(현 `retrieval-service` 경유). 1급 소스.
- `wiki` (assistant wiki): assistant 메모리/위키 표면. GateMem governance metadata(owner/family/project/assistant_private/source_refs/confidence/privacy)를 1급 retrieval 조건으로.
- `repo_docs` / `repo_code`: 레포 문서·소스. 2차 sanitizer 대상(comment/string/identifier/fixture/README/issue/AGENTS.md).
- `web` / `arxiv`: 외부 검색·arXiv. recency-sensitive 질의에서만 우선(arXiv:2606.18947의 FreshQA 관찰 반영).
- `vendor_docs`: 벤더 공식 문서. provider routing 대상.

## Phase 1 Scope (게이트웨이 코어)

- search gateway: MCP-compatible tool 표면 1개로 모든 grounding 요청 수렴.
- provider routing + fallback + retrieval-depth 제어(DSG의 1급 제어, arXiv:2606.18947).
- source/citation renderer: source-aware context rendering, 인용 마커, Search-Induced Verbosity 억제(엄격 출력 계약 강제).
- cache: exact(정규화 질의 해시) + semantic(임베딩 근접) 2-tier.
- privacy/scope filter: visibility_scope를 retrieval 필터의 1급 조건으로 강제(GateMem access control, arXiv:2606.18829).

## Phase 2 Scope (안전·컴플라이언스)

- repo/code ingestion sanitizer: Tree-sitter/CST 기반 위험 노드 탐지·격리·중화(CodeSentinel, arXiv:2606.19235).
- deletion/forgetting tests: 명시적 삭제 후 active forgetting 검증(GateMem, arXiv:2606.18829).
- prompt-injection regression set: indirect injection 공격군에 대한 회귀 스위트.
- contextual redaction evaluation: indexing 전 PII redaction 품질을 R-Score로 평가(RedactionBench, arXiv:2606.18782).
- runtime trace monitor: tool call/model output을 execution trace로 남기고 policy predicate로 non-compliant action reject(C-Trace, arXiv:2606.19242).
- supply-chain checks: skill/plugin auxiliary resource 및 의존 패키지 리뷰(PhantomSkill arXiv:2606.19191, PYPILINE arXiv:2606.19063) — security knowledge RAG로 반영.

## Architecture Sketch

```text
[reasoning model / agent]
        │  MCP tool call (grounding.search / grounding.fetch)
        ▼
┌───────────────────────────── Unified Grounding Gateway (MCP server) ─────────────────────────────┐
│  PEP-1 Request Policy  →  Provider Router  →  Cache(exact, semantic)  →  Source Adapters          │
│       (scope, compliance)        │ fallback chain        │ hit/miss            │                  │
│                                  ▼                        ▼                    ▼                  │
│                          retrieval-depth ctl       cache store        local_rag | wiki | repo |   │
│                                                                        web/arxiv | vendor_docs    │
│  PEP-2 Scope/Privacy Filter (visibility_scope, GateMem metadata)  ◄───────────────┘               │
│  PEP-3 Context Renderer + Citation (source-aware, anti-verbosity)                                 │
│  PEP-4 Output Contract Validator  →  emits {answer_context, source_refs, visibility_scope, ...}   │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
        │                                                  ▲
        ▼                                                  │ trace
[runtime compliance monitor: C-Trace]  ──── reject non-compliant ────┘

[ingestion path — offline / index-time]
  source → CodeSentinel sanitizer (CST) → RedactionBench redaction → supply-chain review → indexer → Weaviate/PG
```

- modules: Gateway(MCP server), Provider Router, Source Adapters, Cache, Scope/Privacy Filter, Context Renderer, Output Contract Validator, Ingestion Sanitizer, Redactor, Supply-chain Reviewer, Compliance Monitor.
- trust boundaries: 외부/레포 소스 콘텐츠는 **untrusted**, model-facing context로 들어가기 전 반드시 sanitizer/redactor를 통과한다. 게이트웨이 내부는 정책 enforcement 후의 신뢰 영역.
- policy enforcement points (PEP): PEP-1 요청 정책(scope/compliance), PEP-2 retrieval scope 필터, PEP-3 렌더/인용, PEP-4 출력 계약 검증, 그리고 index-time sanitizer/redactor. PEP-1·PEP-4 결과는 모두 trace에 기록.
- data flow: 요청 → 정책 → 라우팅/캐시 → 어댑터 검색 → scope 필터 → 렌더 → 계약 검증 → 응답. 모든 tool call/output은 compliance monitor로 미러링.

## Data Model / API Contracts

retrieval request (MCP `grounding.search`):

```json
{
  "query": "string",
  "scope": "owner|family|project|assistant_private|public",
  "principal": { "id": "string", "roles": ["..."] },
  "sources": ["local_rag","wiki","repo_docs","web","arxiv","vendor_docs"],
  "depth": "shallow|standard|deep",
  "k": 8,
  "freshness_required": "any|fresh|<=duration",
  "provider_hint": "string|null",
  "cache": "use|bypass|refresh",
  "trace_id": "string"
}
```

retrieval response: 위 `Output Contract` 형태 + `cache`/`provider`/`retrieval`/`cost`/`compliance` 메타.

- source_refs: `ref_id`는 `{source_type}:{stable_locator}` 결정적 생성.
- visibility scopes: `owner > family > project > assistant_private > public` 의 strict ordering; downgrade 발생 시 `risk_flags:["scope_downgraded"]`.
- freshness: `indexed_at`, `source_mtime`로 `staleness` 산출(fresh/aging/stale); `ttl` 만료 시 `cache_stale` 플래그.
- risk_flags: 닫힌 enum(위 계약). 새 플래그 추가는 design 문서 갱신 필요.
- cache keys: exact = `sha256(normalize(query) + scope + provider + depth + k + source_set)`; semantic = `(embedding_cluster_id, scope, source_set)` + 임계 유사도. scope/principal이 키에 포함되어 cross-scope cache leak을 구조적으로 차단.

## Security Model

- access control / forgetting (GateMem, arXiv:2606.18829): owner/family/project/assistant_private/public scope와 confidence/privacy/source_refs를 메모리·wiki retrieval의 1급 필터로. 명시 삭제 요청 후 active forgetting을 retrieval 비노출로 검증. (논문 관찰: utility·access control·active forgetting을 동시에 만족하는 단일 baseline이 없음 → 세 축을 분리 측정.)
- ingestion sanitizer (CodeSentinel, arXiv:2606.19235): Tree-sitter로 high-risk model-facing CST 노드(comment/string/identifier/decoy code) 추출 → syntax 사전필터 + CST-guided Dynamic Min-K% + node perturbation → 격리/중화 후 downstream 도달. 목표: 논문의 node-level F1(평균 0.80) 수준을 회귀 스위트 기준선으로.
- runtime compliance monitor (C-Trace, arXiv:2606.19242): consent/purpose limitation/data minimization/right-to-erasure 류 정책을 execution trace 위 formal predicate로 표현, 모든 tool invocation·model output을 가로채 non-compliant action을 reject. 게이트웨이 PEP-1/PEP-4가 trace 소스.
- contextual PII (RedactionBench, arXiv:2606.18782): contextual integrity 기반 — "누가/왜/어떤 맥락에서 보유했는가"로 위반 여부 판단(단순 NER 아님). indexing 전 redaction 품질을 R-Score(char-level, 형식 차이 무효화)로 평가. over-redaction은 별도 risk로 추적.
- supply-chain (PhantomSkill arXiv:2606.19191, PYPILINE arXiv:2606.19063): skill의 textual description뿐 아니라 auxiliary resource(VulMask류 vulnerability-shaped code)까지 리뷰 대상. suspicious API knowledge base + agent workflow(AST→API call graph→구조화 평가)를 security knowledge RAG 설계로 차용.

## Integration Points In Current Repo

후속 구현에서 손볼 가능성이 높은 표면(이번 작업에서 구현하지 않음):

- `services/mcp-bridge/.../McpBridgeController.java`, `McpSettings.java`: MCP tool 표면을 grounding gateway 계약으로 확장하는 진입점.
- `services/api-gateway/.../GatewayController.java`: 외부 호출 경계, scope/principal 전달과 출력 계약 노출.
- `services/retrieval-service/.../RetrievalService.java`, `RetrievalRanker.java`, `RetrievalController.java`, `ScoreGateCandidateSelector.java`: provider/소스 어댑터의 첫 백엔드, retrieval-depth/k 제어, 렌더 입력.
- `services/retrieval-service/.../HttpLocalRerankerClient.java`, `reranker-sidecar`: depth=deep 경로의 옵션 rerank.
- `services/indexer-service`(MarkdownChunker 등): index-time sanitizer/redactor 삽입 지점.
- `database/postgres/ddl`, `database/weaviate`: source_refs/scope/freshness/risk_flags 메타 컬럼·클래스 속성, cache 저장.
- `docs/design/retrieval-quality-improvement-design.md`: 게이트웨이 도입 후 retrieval governance truth 갱신 필요.

planning registry 항목(이 repo는 index 등록을 요구함): `docs/projects/README.md`, `docs/_indexes/active-docs.md`, `docs/_indexes/design-map.md`에 P0005 등록 — 본 작업에서 함께 갱신.

## WBS

| ID | Work Item | Status | Progress | Notes |
| --- | --- | --- | --- | --- |
| T0026 | Grounding gateway contract + MCP surface (Phase 1) | Planned | 0% | 출력 계약 강제, mcp-bridge/api-gateway 경로, opt-in |
| T0027 | Provider routing + fallback + retrieval-depth + 2-tier cache | Planned | 0% | retrieval-service 백엔드, cost/latency 메타 |
| T0028 | Scope/privacy filter (GateMem metadata) | Planned | 0% | visibility_scope 1급 필터, leak test |
| T0029 | Ingestion sanitizer (CodeSentinel CST) + prompt-injection regression set | Planned | 0% | Phase 2 |
| T0030 | Deletion/forgetting tests + contextual redaction eval (R-Score) | Planned | 0% | Phase 2 |
| T0031 | Runtime compliance monitor (C-Trace) + supply-chain review | Planned | 0% | Phase 2, trace/predicate reject |

## Planned Task Candidates

- Phase 1: T0026–T0028. Phase 2: T0029–T0031.
- task 정식 발급은 Dean의 project active 전환 후. 조건부 task는 게이트 전 미발급(SDLC 위생).

## Overall Progress

- 0% (planning draft)

## Milestones

- M1: Phase 1 게이트웨이가 출력 계약을 강제한 채 로컬 동작 + 계약 위반 0건 회귀.
- M2: Phase 2 sanitizer/forgetting/redaction/compliance 각 acceptance test 결과 고정 및 ship/defer 결정 리포트.

## Exit Criteria

1. G1–G5가 각자의 Done When 기준으로 검증되거나, 미충족 축은 defer 근거와 함께 후속 task로 명시됨.
2. 필수 task(T0026–T0031)가 done 또는 종료 근거와 함께 정리됨.
3. 게이트웨이 도입이 기존 `retrieval-service`/`mcp-bridge` default 동작과 control-plane 불변(단일 dim/class, registry-gated indexing, local-only)을 깨지 않았음을 확인.

## Completion Evidence

- 출력 계약 강제·위반 0건 회귀 테스트 로그, provider routing/cache hit·cost·latency 메타 샘플.
- scope leak test, deletion/forgetting test, prompt-injection node-level 탐지 지표, R-Score redaction 평가, C-Trace reject 사례.
- 기존 retrieval default 불변 및 control-plane 불변 확인.

## Outputs / Handoff

- Phase 1 done: 게이트웨이가 다른 에이전트의 grounding 진입점이 됨(operator note + MCP 계약 문서).
- Phase 2 done: 보안 게이트 결과 리포트와, defer된 축의 재시도 조건.
- design truth 갱신 handoff: `docs/design/retrieval-quality-improvement-design.md` 또는 신규 grounding-gateway design 문서.

## Quality Axes In Scope

| Axis | Why It Matters Here | Required Evidence |
| --- | --- | --- |
| WHOLE | grounding 분리가 기존 RAG·control-plane 불변을 깨지 않아야 함 | retrieval default·dim/class·registry 불변 확인 |
| SCOPE | "게이트웨이 + 안전 계층"에 한정, 무관한 retrieval 리팩터로 번지지 않음 | 변경 표면이 통합 지점에 국한된 흔적 |
| HANDOFF | 다른 에이전트가 안전하게 호출할 단일 표면을 넘김 | MCP 계약 문서 + operator note + 보안 게이트 리포트 |

## Goal Verification

| Goal ID | Status | Evidence | Notes |
| --- | --- | --- | --- |
| G1 | Pending | | |
| G2 | Pending | | |
| G3 | Pending | | |
| G4 | Pending | | |
| G5 | Pending | | |

## Risks / Open Questions

- cost/latency: provider routing/fallback이 deep depth에서 비용·지연을 키움 → `cost` 메타와 depth 상한, 캐시로 완화. 미해결: provider별 예산 정책 위치(게이트웨이 vs 호출자).
- cache invalidation: source_mtime/ttl 기반 무효화의 정확성, semantic cache의 false hit. 미해결: scope 변경 시 semantic 캐시 무효화 트리거.
- source freshness: web/arXiv recency 우위(arXiv:2606.18947 FreshQA) vs local 캐시 staleness 균형. 미해결: freshness_required 미충족 시 fail vs degrade 정책.
- privacy leakage: scope 키가 캐시·로그·trace에 새지 않아야 함. 미해결: trace 보존 기간·접근 제어(C-Trace trace 자체가 PII일 수 있음).
- over-redaction: contextual redaction이 답변 유용성을 훼손. 미해결: R-Score 목표선과 utility 트레이드오프 임계.
- citation rendering: anti-verbosity와 충분한 근거 노출의 균형, 인용 마커 표준.
- deletion semantics: "삭제"가 index 제거인지 retrieval 차단인지, 캐시·trace까지 전파되는지. 미해결: hard delete vs tombstone.
- evaluation data: GateMem/CodeSentinel/RedactionBench는 외부 벤치마크 — 로컬 도메인용 fixture를 어디까지 자체 구축할지. 미해결: 평가셋 라이선스·재현 환경.

## References

- arXiv:2606.18947, 2606.18829, 2606.19235, 2606.19242, 2606.18782, 2606.19063, 2606.19191 (frontmatter `source_refs`, 2026-06-18 검증).
- docs/design/retrieval-quality-improvement-design.md
- docs/projects/P0002-retrieval-governance-hardening.md, P0003-scoregate-adaptive-context-selection.md, P0004-embedding-model-8b-evaluation.md

## Completion Guardrails

- Purpose를 더 작은 조각으로 축소해 `done` 처리하지 않는다(Phase 2 보안 축을 빼고 닫지 않음).
- 남은 축을 후속으로 넘겼다면 `done`이 아니라 `active`/`blocked`/`superseded` 중 하나로 둔다.
- 게이트웨이 도입이 기존 retrieval default나 control-plane 불변을 깨면 release 경로에서 제외한다.
- 새 `project` 남발 금지 원칙 하에서, Phase 내부 분해는 task로 처리한다.

## Status

- 2026-06-18: project 초안 생성(draft). Silverstone 위임 요청 하에 에이전트가 기획. 7개 arXiv 논문 메타데이터(제목·2026-06-17 제출일) 직접 검증 완료. Dean의 active 전환·owner lock 대기. 코드/배포/외부 mutation 없음.
