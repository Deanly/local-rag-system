---
type: design
title: ubiquitous-language
status: current
domain: ubiquitous-language
owner:
created: 2026-05-24
updated: 2026-05-24
retrieval_class:
  - term-excerpt
context:
  default_load: false
  section_load: true
  evidence_only: false
  size_tier: small
referenced_by:
  - docs/README.md
  - docs/design/control-plane.md
  - docs/design/local-rag-system-development-direction.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - source:planning/local-rag-system-project-note
  - source:planning/local-rag-system-design-note
  - source:planning/source-registry-ssot-strategy-note
tags:
  - docs/design
  - ubiquitous-language
  - local-rag-system
---

# ubiquitous-language

- Type: design
- Domain: ubiquitous-language
- Owner:
- Created: 2026-05-24
- Updated: 2026-05-24
- Referenced By:
  - `docs/README.md`
  - `docs/design/control-plane.md`
  - `docs/design/local-rag-system-development-direction.md`
  - `docs/design/source-registry-and-project-ssot.md`

## Purpose

이 문서는 `local-rag-system`에서 같은 대상을 같은 말로 부르기 위한 canonical term 기준이다.

새로운 design 문서가 추가되거나 핵심 개념, 상태, 경계가 바뀌면 같은 변경 셋에서 이 문서도 함께 갱신한다.

## Retrieval Rule

- 이 문서는 canonical term registry다.
- 일반 task/project execution에서 full document를 기본 로딩하지 않는다.
- 용어 판단이 필요할 때 관련 heading 또는 term section만 section-load 한다.
- terminology governance 또는 naming design 작업일 때만 전체 문서를 읽는다.

## Maintenance Rule

- 새로운 design 문서가 추가될 때 핵심 명사, 상태, 책임, 경계가 생기면 이 문서에 추가한다.
- 기존 design 문서가 변경될 때 용어의 의미나 범위가 바뀌면 이 문서를 함께 수정한다.
- `task`, `project`, `guide`는 이 문서의 용어를 우선 사용한다.
- 같은 대상을 가리키는 표현이 여러 개 생기면 canonical term 하나를 고정한다.

## Core Boundary Terms

### `local-rag-system`

registered local source folders를 대상으로 하는 1인용 local-only retrieval augmented generation system이다.

### `application services`

Spring Boot 기반 target runtime의 하위 서비스들이다. `api-gateway`, `source-registry-service`, `indexer-service`, `retrieval-service`, `mcp-bridge`로 나뉜다.

### `source registry`

장비별로 어떤 source root가 indexing/search 대상인지 선언하는 local control surface다. 등록되지 않은 폴더는 디스크에 있어도 RAG 대상이 아니다.

### `project registration`

`project_id`, optional repo path, primary SSOT source, default context source를 묶는 registry entry다.

### `project_id`

Codex/RAG skill, source registry, index metadata가 공유하는 stable project key다. 예: `project-alpha`, `project-beta`, `personal-notes`.

### `source_id`

source registry 안에서 indexing root를 식별하는 stable key다. 권장 형식은 `project_id.layer`다. 예: `project-alpha.docs`, `personal-notes`.

### `source root`

인덱싱 대상 root directory다. `SourceRoot` registry entry가 path, type, priority, sensitivity, include/exclude, read/write policy를 함께 정의한다.

### `SSOT source`

특정 project 또는 knowledge layer의 current truth로 취급되는 source root다. repo `docs/`는 해당 repo의 SSOT source이고, 별도 등록된 compiled knowledge source는 cross-project reference source다.

### `source folder`

인덱싱 대상 root directory다. 현재 설계에서는 장비별 source registry에 등록된 source roots만 read-only로 접근한다.

### `local source corpus`

macOS 로컬 파일 시스템에서 접근 가능한 Markdown 중심 개인/업무 corpus다. 특정 노트 앱 런타임이 아니라 file system source로 취급한다.

### `compiled knowledge source`

사용자가 등록한 장기 지식/요약 source다. cross-project synthesis와 장기 memory를 제공할 수 있지만, repo-local current truth를 대체하지 않는다.

### `planning-meta`

개인 기획/진행 기록 layer의 SSOT role이다. repo 문서의 current truth가 아니라 planning/context evidence로 취급한다.

### `derived index`

Weaviate와 state store에 저장되는 재생성 가능한 검색용 데이터다. 원본 truth가 아니다.

### `Codex bridge`

Codex가 `local-rag-system`을 검색할 수 있게 하는 MCP tool 또는 REST adapter다.

### `RAG skill`

Codex나 다른 agent가 `project_id` 기반으로 `rag_search`, `rag_get_document`, `rag_index_status` 같은 retrieval tools를 호출하는 integration surface다.

## Runtime Terms

### `watcher`

Java NIO `WatchService` 등으로 source folder 변경 이벤트를 받는 best-effort signal 처리자다.

### `scanner`

주기적으로 source folder를 순회하며 freshness를 보장하는 authority다. watcher 이벤트 누락을 보정한다.

### `index job`

신규, 변경, 삭제 파일에 대해 indexer가 처리해야 하는 작업 단위다.

### `state store`

`DocumentState`, `IndexJob`, `FailureRecord`를 저장하는 local durable store다. target runtime은 PostgreSQL을 사용한다.

### `operator prerequisite`

운영자가 먼저 준비해야 하는 외부 전제다. 예: source folder 경로, Ollama endpoint, embedding model 설치, Docker volume 경로.

## Retrieval Terms

### `hybrid retrieval`

BM25/keyword score와 dense vector similarity를 함께 사용하는 retrieval 방식이다.

### `exact hint`

티켓 번호, 파일명, path fragment, wiki link, code identifier처럼 lexical match를 강하게 boost해야 하는 query signal이다.

### `rerank`

hybrid retrieval 후보를 local reranker 또는 local LLM judge로 재정렬하는 후속 단계다. MVP에서는 optional이다.

### `graph expansion`

Markdown link와 backlink를 사용해 검색 결과 주변 문서를 낮은 가중치로 확장하는 후속 단계다.

### `citation`

검색 결과가 어느 source path와 heading에서 왔는지 사용자와 Codex가 확인할 수 있게 하는 출처 표기다.

## Indexing Terms

### `SourceDocument`

source folder 아래의 개별 원본 파일이다.

### `DocumentState`

path, size, mtime, sha256, last indexed status를 저장하는 freshness 비교 기준이다.

### `Chunk`

검색과 embedding의 기본 단위다. Markdown은 heading-aware chunking을 기본으로 한다.

### `ContentHash`

파일 또는 chunk content의 sha256 fingerprint다. 변경 판단과 idempotent upsert에 사용한다.

### `FailureRecord`

읽기, parsing, embedding, search-index upsert 실패를 재시도 가능하게 남기는 canonical failure record다.

## Domain Terms

### `raw source`

LLM이 해석하기 전의 원문 파일, clipping, transcript, image, PDF, dataset을 뜻한다.

### `source ref`

생성 문서가 근거로 읽은 raw source나 외부 문서 경로를 뜻한다. markdown properties에서는 `source_refs` key를 사용한다.

### `markdown properties`

문서 상단 YAML frontmatter에 적는 machine-readable metadata다.

### `wiki surface`

LLM이 유지하는 persistent markdown artifact다. 이 하네스에서는 `design`, `guide`, `project`, `task`, `report`가 wiki surface다.

### `done criteria`

Codex 또는 사람이 작업을 닫기 전에 참이어야 하는 검증 가능한 완료 조건이다.

## Out-Of-Scope Terms

### `note app plugin`

특정 노트 앱 내부에서만 동작하는 extension이다. MVP 범위가 아니다.

### `multi-user SaaS`

다중 사용자, tenant, cloud 권한 모델을 포함하는 SaaS 형태다. 이 project의 범위가 아니다.

### `attachment deep parsing`

PDF/OCR/canvas/Excalidraw의 심화 추출이다. MVP 이후 task로 다룬다.

## Change Log

- 2026-05-24: `local-rag-system` 전용 canonical term registry로 초기화.
- 2026-05-24: source registry, project registration, project/source identifiers, SSOT source, compiled knowledge source, planning-meta, RAG skill 용어 추가.
