# Projects

이 파일은 현재 읽어야 하는 `project` 문서의 얇은 입구입니다.

## Rules

- `Status: active` 인 문서만 적습니다.
- umbrella project를 먼저 적습니다.
- 예외 분기 project가 있다면 umbrella lineage와 parent umbrella를 함께 적습니다.
- 각 항목은 링크, 한 줄 설명, `Updated` 날짜만 남깁니다.
- 문서를 닫으면 이 목록에서 제거하고 본문 `Status` 이력에 종료 근거를 남깁니다.

## Active

- 현재 active project 문서가 없습니다.

## Done

- [`P0003-scoregate-adaptive-context-selection.md`](P0003-scoregate-adaptive-context-selection.md): ScoreGate selector와 offline snapshot evaluator는 남기고, current profile의 true local cross-encoder `r_i` 부재로 runtime rollout은 no-ship으로 닫은 exception branch. Official release version: `1.2.0`. Updated: 2026-06-16.
- [`P0002-retrieval-governance-hardening.md`](P0002-retrieval-governance-hardening.md): P0001 functional baseline 이후 governed Hybrid RAG 검색 거버넌스 hardening exception branch. Official release version: `1.1.0`. Updated: 2026-05-31.
- [`P0001-local-rag-system.md`](P0001-local-rag-system.md): 장비별 source roots 대상 1인용 local-only RAG functional baseline. Official baseline version: `1.0.0`. Updated: 2026-05-29.
