# Design Retrieval Index

이 파일은 `docs/design/` 문서를 LLM/Codex가 통째로 읽지 않고 작업 성격에 맞게 선택하도록 돕는 retrieval index입니다.

## Rules

- `docs/design/control-plane.md`는 whole-system orientation용 `core-start` 문서입니다.
- `docs/design/ubiquitous-language.md`는 canonical term registry이지만 기본 전체 로딩 대상이 아닙니다. 용어 판단이 필요한 section만 읽는 section-load 문서입니다.
- `docs/design/local-rag-system-development-direction.md`는 Local RAG 개발 방향의 current truth입니다.
- `docs/design/source-registry-and-project-ssot.md`는 장비별 source registry, project id, SSOT source registration, Codex/RAG skill scope의 current truth입니다.
- `docs/design/msa-runtime-and-storage.md`는 MSA runtime, Docker Compose, PostgreSQL DDL, Weaviate schema의 current truth입니다.
- `docs/design/retrieval-quality-improvement-design.md`는 검색 품질 evaluation, source weighting, rerank, chunking 개선의 current truth입니다.
- 새 domain design 문서를 추가하면 이 index와 `docs/_indexes/design-map.md`도 함께 갱신합니다.
- 이 index는 design truth를 대체하지 않습니다. 실제 결정은 source design doc에서 합니다.

## Size Tiers

- `small`: 2,000 words 미만
- `medium`: 2,000 words 이상 5,000 words 미만
- `large`: 5,000 words 이상

## Retrieval Classes

| Retrieval Class | Meaning |
| --- | --- |
| `core-start` | 거의 모든 docs/runtime orientation에서 짧게 읽는 whole-system entry |
| `term-excerpt` | full document가 아니라 관련 heading/term section만 읽는 vocabulary source |
| `domain-current` | 특정 bounded context current truth |
| `context-map` | bounded context ownership과 shared boundary를 고를 때 읽는 map |

## Design Index

| Design Doc | Design Kind | Retrieval Class | Read When | Do Not Read When | Size Tier | Related Domain | Related Project/Task |
| --- | --- | --- | --- | --- | --- | --- | --- |
| [`control-plane.md`](control-plane.md) | control | `core-start` | whole-system outcome, active surfaces, validators, project/task handoff를 확인할 때 | 특정 domain의 상세 boundary를 대체하려 할 때 | small | whole-system control | all projects/tasks |
| [`ubiquitous-language.md`](ubiquitous-language.md) | term-registry | `term-excerpt` | canonical term, naming, status vocabulary, boundary vocabulary 판단이 필요할 때 | ordinary task work에서 full document를 기본 로딩할 때 | small | all domains | all term-linked docs |
| [`local-rag-system-development-direction.md`](local-rag-system-development-direction.md) | domain design | `domain-current` | Local RAG architecture, MVP scope, indexing/search/Codex boundary를 판단할 때 | 하네스 운영 규칙만 확인할 때 | medium | local-rag-system | `docs/projects/P0001-local-rag-system.md` |
| [`source-registry-and-project-ssot.md`](source-registry-and-project-ssot.md) | domain design | `domain-current` | 장비별 source registry, project SSOT registration, Codex/RAG skill scope를 구현할 때 | 일반 RAG 개념만 확인할 때 | medium | source-registry | `docs/tasks/T0001-source-registry-project-ssot-registration.md` |
| [`msa-runtime-and-storage.md`](msa-runtime-and-storage.md) | runtime design | `domain-current` | MSA service boundary, Docker Compose, PostgreSQL DDL, Weaviate schema, source mount contract를 구현할 때 | 순수 검색 ranking 알고리즘만 확인할 때 | medium | runtime/storage | `docs/tasks/T0002-msa-runtime-baseline.md`, `docs/tasks/T0009-host-local-ollama-rag-configuration.md` |
| [`retrieval-quality-improvement-design.md`](retrieval-quality-improvement-design.md) | domain design | `domain-current` | 검색 품질, evaluation harness, primary source weighting, rerank, chunking 개선을 구현할 때 | source registry 등록이나 Compose mount contract만 확인할 때 | medium | retrieval-quality | `docs/projects/P0002-retrieval-governance-hardening.md`, `docs/tasks/T0013-retrieval-chunking-and-document-authority-hardening.md`, `docs/tasks/T0014-search-filter-and-answer-context-governance.md`, `docs/tasks/T0015-answer-quality-and-staleness-evaluation.md`, `docs/tasks/T0016-retrieval-audit-observability-expansion.md`, `docs/tasks/T0017-local-reranker-evaluation.md` |

## Change Log

- 2026-05-24: Local RAG 개발 방향 design surface 추가.
- 2026-05-24: Source registry and project SSOT design surface 추가.
- 2026-05-24: MSA runtime and storage design surface 추가.
- 2026-05-25: MSA runtime source mount contract를 generic source slot 기준으로 갱신.
- 2026-05-25: Retrieval quality improvement design surface 추가.
- 2026-05-29: MSA runtime and Local RAG design surfaces updated for ordered local/LAN Ollama endpoint failover.
- 2026-05-29: Retrieval quality design surface linked to P0002 and T0013 for governed Hybrid RAG hardening.
- 2026-05-30: Retrieval quality and storage design surfaces updated for T0013 metadata-aware chunking, document authority indexing, and metadata reindex contract.
- 2026-05-30: Retrieval quality design surface updated for T0014/T0015 metadata filter, answer context, staleness, source-use, and citation evaluation governance.
- 2026-05-30: Retrieval quality design surface updated for T0016 search audit observability expansion.
- 2026-05-31: Retrieval quality design surface updated for T0017 local reranker deployment decision and P0002 closeout.
