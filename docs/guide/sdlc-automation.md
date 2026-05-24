---
type: guide
title: sdlc-automation
status: current
owner: Codex
created: 2026-05-24
updated: 2026-05-24
related_project:
  - docs/projects/P0001-local-rag-system.md
related_task:
  - docs/tasks/T0003-spring-boot-msa-skeleton.md
related_design:
  - docs/design/control-plane.md
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - docs/projects/P0001-local-rag-system.md
tags:
  - docs/guide
  - sdlc
  - automation
  - local-rag-system
---

# sdlc-automation

- Type: guide
- Created: 2026-05-24
- Updated: 2026-05-24
- Related Project: docs/projects/P0001-local-rag-system.md
- Related Task: docs/tasks/T0003-spring-boot-msa-skeleton.md
- Related Design: docs/design/control-plane.md; docs/design/local-rag-system-development-direction.md; docs/design/msa-runtime-and-storage.md; docs/design/source-registry-and-project-ssot.md

## Purpose

이 guide는 Codex SDLC 자동화가 `local-rag-system`을 설계 단계에서 멈추지 않고 기능적 baseline까지 순차적으로 밀고 가도록 실행 순서, gate, evidence, 문서 업데이트 규칙을 고정한다.

## Project Goal

`local-rag-system`의 목표는 등록된 macOS local source folders를 read-only로 감시/스캔하고, 변경된 Markdown 파일만 local Ollama embedding으로 chunk indexing하여 Weaviate BM25/vector hybrid search로 검색하며, Codex/CLI/future UI가 같은 Spring Boot MSA API surface를 통해 citation 포함 검색 결과와 index status를 사용할 수 있게 하는 1인용 local-only RAG system을 Docker Compose로 재현 가능하게 제공하는 것이다.

이 목표는 다음 상태가 모두 성립할 때 완료된다.

- `docker compose`로 PostgreSQL, Weaviate, `api-gateway`, `source-registry-service`, `indexer-service`, `retrieval-service`, `mcp-bridge`가 실행된다.
- source registry가 `project_id`, `source_id`, `ssot_role`, include/exclude, read/write policy를 검증한다.
- scanner/watcher가 샘플 source folder의 생성, 수정, 삭제를 감지하고 PostgreSQL state와 index job에 반영한다.
- Markdown parser/chunker가 heading-aware chunk를 만들고 Ollama embedding을 통해 Weaviate에 upsert/delete한다.
- `/api/search`, `/api/index/status`, force scan API 또는 MCP equivalent가 smoke test로 검증된다.
- 검색 결과는 source metadata, citation, score breakdown을 포함한다.
- private source content는 local/network Ollama endpoint 외부의 hosted LLM API로 전송되지 않는다.

## Automation Entry Context

SDLC 자동화는 새 세션을 시작할 때 아래 순서로 context를 읽는다.

1. `AGENTS.md`
2. `docs/design/control-plane.md`
3. `docs/guide/sdlc-automation.md`
4. `docs/projects/P0001-local-rag-system.md`
5. 현재 active task 문서
6. active task가 직접 참조하는 design 문서

전체 `docs/`를 broad load하지 않는다. 필요할 때 `docs/design/README.md`와 `docs/_indexes/active-docs.md`로 추가 문서를 선택한다.

## Critical Path

한 번에 하나의 critical implementation task만 active로 둔다. 보조 문서 정리나 fixture 정리는 병렬 가능하지만, critical path owner는 하나여야 한다.

| Order | Task | Gate | Status |
| ---: | --- | --- | --- |
| 1 | `T0003-spring-boot-msa-skeleton` | five services build, test, and answer health checks through Compose | Active |
| 2 | `T0001-source-registry-project-ssot-registration` | registry load/validate/scope resolution covered by tests | Blocked until T0003 |
| 3 | `T-candidate-04-source-state-indexer` | scanner/watcher/state diff handles create/update/delete | Candidate |
| 4 | `T-candidate-05-markdown-weaviate-indexing` | Markdown chunks embed and upsert/delete in Weaviate | Candidate |
| 5 | `T-candidate-06-hybrid-search-api` | `/api/search` returns hybrid results with citation and filters | Candidate |
| 6 | `T-candidate-07-codex-mcp-bridge` | Codex-facing tools call search/status/force-scan path | Candidate |
| 7 | `T-candidate-08-retrieval-quality-baseline` | evaluation set and regression checks exist for retrieval quality | Candidate |
| 8 | project closeout | all P0001 goals verified with evidence | Candidate |

## Stage Gates

### Gate 1: Service Skeleton

The first implementation gate opens when:

- root build system exists,
- five Spring Boot service subprojects exist,
- each service has a testable health endpoint,
- `docker compose up --build` starts application and infra services,
- `api-gateway` can proxy or aggregate basic health/status.

### Gate 2: Registry

The registry gate opens when:

- `config/source-registry.example.yaml` can be loaded and validated,
- invalid duplicate ids, missing active paths, invalid primary source, invalid glob policies fail deterministically,
- `project_id` resolves the ordered source scope,
- registry snapshot is persisted or explicitly handed off to PostgreSQL in a documented way.

### Gate 3: Freshness

The freshness gate opens when:

- scanner computes file path, size, mtime, sha256, and deletion state,
- watcher events enqueue best-effort jobs,
- scanner remains the freshness authority,
- sample create/update/delete smoke leaves PostgreSQL evidence.

### Gate 4: Indexing

The indexing gate opens when:

- Markdown frontmatter and headings are parsed,
- chunks carry registry metadata,
- Ollama embedding is called through configured local endpoint,
- Weaviate upsert/delete is idempotent,
- changed file reindex touches only affected document/chunks.

### Gate 5: Retrieval

The retrieval gate opens when:

- keyword/vector/hybrid modes exist,
- metadata filters apply `project_id`, `source_id`, `ssot_role`, `sensitivity`,
- result citation includes source path and heading,
- score breakdown is returned,
- stale deleted chunks do not appear.

### Gate 6: Codex Integration

The integration gate opens when:

- `rag_list_projects`, `rag_list_sources`, `rag_search`, `rag_get_document`, `rag_index_status`, `rag_force_scan` are available through MCP or a documented REST bridge,
- Codex smoke can query sample content,
- operator-facing setup notes are accurate.

## Documentation Update Rule

Every SDLC task must update these surfaces in the same change set:

- task `Goal Verification`,
- parent project WBS and Overall Progress,
- `docs/tasks/README.md`,
- `docs/_indexes/active-docs.md`,
- relevant design doc if a contract changes,
- `README.md` if operator commands change.

## Verification Ladder

Use the strongest verification available at the current stage.

| Stage | Minimum Verification |
| --- | --- |
| docs only | `./docs/bin/validate-codex-readiness.sh` and `./docs/bin/validate-closeout.sh --all` |
| compose/storage | `docker compose --env-file .env.example config` and DDL smoke if DDL changed |
| Spring build | Docker Maven build, or local `./mvnw test` if a wrapper is added later |
| runtime | `docker compose up --build` plus health/status checks |
| indexing | sample create/update/delete smoke |
| retrieval | search response with citation, filters, and stale-delete check |
| integration | Codex MCP or REST bridge smoke |

## Closeout Rule

Do not close `P0001` until every `Goal Inventory` row in the project has `Goal Verification: Done` with concrete evidence. Moving unfinished scope into future tasks is not enough to close the project.

## References

- `docs/projects/P0001-local-rag-system.md`
- `docs/design/control-plane.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/msa-runtime-and-storage.md`
- `docs/design/source-registry-and-project-ssot.md`
- `docs/tasks/T0003-spring-boot-msa-skeleton.md`

## Change Log

- 2026-05-24: SDLC automation critical path, project goal, gates, and verification ladder added.
