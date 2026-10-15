---
type: report
title: M4 RAG Embedding Production Cutover
status: verified
owner: dean
created: 2026-09-05
updated: 2026-09-05
task_id: T0027
source_refs:
  - docs/tasks/T0027-m4-rag-embedding-production-cutover.md
  - external:silverstone-pad/docs/evidence/P0033-S9-S13-PRODUCTION-CUTOVER-2026-09-05.md
tags: [docs/report, local-rag, m4, embedding, cutover]
---

# M4 RAG Embedding Production Cutover

Local RAG `v1.3.1` (`4782041`)을 배포해 query와 bulk가 각각
`silverstone/rag-query:qwen3-4b-v2`, `silverstone/rag-bulk:qwen3-4b-v2`와 별도 private token-file을
사용하도록 전환했다. M4 `v0.15.1`에서 두 binding은 `PRODUCTION_ACTIVE`이고 raw
`qwen3-embedding:4b` compatibility request는 `409 WORKLOAD_BINDING_NOT_ACTIVE`로 닫힌다.

Actual validation은 health/search 200, bulk embedding 200, 2560 dimensions와 content-free M4 audit를
확인했다. 기존 index는 10,357 documents, 82,175 chunks를 유지했고 destructive full reindex를 수행하지
않았다. `/api/answer`는 M4 generation을 빌려 쓰지 않고 `503 ANSWER_GENERATION_DISABLED`와 `rag_search`
대체 계약을 반환한다. v0.15.1→v0.15.0 rollback 중에도 authenticated search가 200이었고 final platform
reapply 뒤 health가 유지됐다.

Rollback point는 Local RAG의 이전 immutable release, `local.env.p0033-pre-cutover-20260905T0146`과
`docker-compose.override.yaml.p0033-pre-cutover-20260905T0146`이다. Secret 값, query content와 vector는
evidence에 기록하지 않았다.
