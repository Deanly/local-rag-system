# Tasks

이 파일은 현재 읽어야 하는 `task` 문서의 얇은 입구입니다.

## Rules

- `Status: active` 인 문서를 먼저 적고, blocked 문서는 별도 섹션에 둡니다.
- 가능하면 각 task가 어느 umbrella project에 속하는지 함께 적습니다.
- 각 항목은 링크, 한 줄 설명, `Updated` 날짜만 남깁니다.
- 문서를 닫으면 이 목록에서 제거하고 본문 `Status` 이력에 종료 근거를 남깁니다.

## Active

- _none_

## Blocked

- _none_

## Done

- [`T0009-host-local-ollama-rag-configuration.md`](T0009-host-local-ollama-rag-configuration.md): host-local Ollama answer runtime, portable source slots, global registration, indexing, search, and answer smoke를 완료. Updated: 2026-05-25.
- [`T0008-portable-ops-zone-deployment.md`](T0008-portable-ops-zone-deployment.md): tracked defaults를 portable하게 조정하고 `~/Service` 운영 zone 배포를 구성. Updated: 2026-05-24.
- [`T0007-codex-global-rag-integration.md`](T0007-codex-global-rag-integration.md): Codex global MCP adapter, skill, installer, and `rag_answer`를 이 장비에 설치. Updated: 2026-05-25.
- [`T0006-local-device-application-baseline.md`](T0006-local-device-application-baseline.md): 이 장비의 실제 source folders에 `qwen3-embedding:4b` baseline을 적용하고 full indexing/search smoke를 수행. Updated: 2026-05-24.
- [`T0001-source-registry-project-ssot-registration.md`](T0001-source-registry-project-ssot-registration.md): Source registry runtime contract, project scope resolution, metadata propagation을 구현. Updated: 2026-05-24.
- [`T0002-msa-runtime-baseline.md`](T0002-msa-runtime-baseline.md): MSA service boundary, Docker Compose, PostgreSQL DDL, Weaviate schema를 local runtime baseline으로 고정. Updated: 2026-05-24.
- [`T0003-spring-boot-msa-skeleton.md`](T0003-spring-boot-msa-skeleton.md): Spring Boot MSA skeleton, build, health endpoints, Compose runtime smoke를 구현. Updated: 2026-05-24.
- [`T0004-local-rag-functional-baseline.md`](T0004-local-rag-functional-baseline.md): watcher/scanner, indexing, retrieval, MCP REST bridge functional baseline을 구현. Updated: 2026-05-24.
- [`T0005-multi-source-application-hardening.md`](T0005-multi-source-application-hardening.md): 실제 로컬 문서 소스 적용 전 multi-source mount, glob enforcement, default context resolution을 보완. Updated: 2026-05-24.
