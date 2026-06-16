---
type: report
title: scoregate-application-result-summary
status: done
created: 2026-06-16
updated: 2026-06-16
current_focus: "Information-first summary of applying ScoreGate to Local RAG"
report_type: implementation-result
related_project: docs/projects/P0003-scoregate-adaptive-context-selection.md
related_tasks:
  - docs/tasks/T0021-scoregate-offline-selector-experiment.md
  - docs/tasks/T0022-scoregate-offline-evaluation-fixture.md
  - docs/tasks/T0024-local-cross-encoder-sidecar-proof.md
source_refs:
  - https://arxiv.org/abs/2606.14269
  - docs/reports/2026-06-16-scoregate-before-after-comparison.md
  - docs/reports/2026-06-16-scoregate-sidecar-proof-smoke.md
  - docs/evaluation/scoregate-offline-cases.json
  - docs/evaluation/scoregate-runtime-probes.json
  - services/retrieval-service/src/main/java/com/localrag/retrieval/ScoreGateCandidateSelector.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/RetrievalService.java
  - services/retrieval-service/src/main/java/com/localrag/retrieval/HttpLocalRerankerClient.java
  - services/reranker-sidecar/app/main.py
tags:
  - docs/report
  - local-rag-system
  - retrieval-quality
  - scoregate
  - implementation-result
---

# ScoreGate 적용 결과 요약

- Type: report
- Status: done
- Created: 2026-06-16
- Updated: 2026-06-16
- Current Focus: Local RAG에 ScoreGate 논문 아이디어를 적용한 개발 및 검증 결과
- Report Type: implementation-result
- Related Project: `docs/projects/P0003-scoregate-adaptive-context-selection.md`
- Related Tasks:
  - `docs/tasks/T0021-scoregate-offline-selector-experiment.md`
  - `docs/tasks/T0022-scoregate-offline-evaluation-fixture.md`
  - `docs/tasks/T0024-local-cross-encoder-sidecar-proof.md`

## 요약

ScoreGate 논문 아이디어를 Local RAG의 final context selection 계층에 적용했다.

이번 적용으로 구현된 범위는 다음과 같다.

- Java 기반 ScoreGate selector
- selector-level offline evaluator
- optional local cross-encoder sidecar
- retrieval-service의 명시적 debug/opt-in ScoreGate 경로
- runtime snapshot 수집용 probe 및 collector

익명화된 5건 fixture 기준 selector-level before/after 결과는 개선 방향이 뚜렷했다. hit@1은 20.0%에서 80.0%, hit@5는 60.0%에서 100.0%, MRR은 0.307에서 0.900으로 상승했다. 알려진 retained token estimate는 1,810에서 1,460으로 19.3% 감소했다.

local cross-encoder sidecar도 직접 smoke에서 정상 동작했다. `BAAI/bge-reranker-v2-m3` 모델이 `/rerank` API를 통해 normalized relevance score를 반환했다. CPU warm 상태의 2-candidate 요청은 약 5.983초였고 sidecar memory는 약 1.9 GiB 수준이었다.

## 논문 적용 방식

검토 대상은 arXiv `2606.14269`, `ScoreGate: Adaptive Chunk Selection for Retrieval-Augmented Generation via Dual-Score Statistical Fusion`이다.

논문의 핵심은 fixed top-K로 chunk를 고정 개수 선택하는 대신 두 점수를 함께 보는 것이다.

| Score | 의미 | Local RAG 적용 |
| --- | --- | --- |
| `s_i` | first-stage similarity | Weaviate/embedding 계열 후보 점수에서 normalized similarity로 사용 |
| `r_i` | cross-encoder relevance | local reranker sidecar가 query와 candidate text를 함께 읽고 산출 |
| fusion score | disagreement zone 판단 점수 | ScoreGate selector 내부에서 bucket/fusion/MAX-K 판단에 사용 |

Local RAG에서는 indexing, source registry, embedding generation, Weaviate schema, source data를 변경하지 않았다. 적용 범위는 retrieval 이후 context selection과 score audit에 한정했다.

## 작업 장비 및 실행 환경

작업은 Local RAG 개발/검증에 사용하는 로컬 장비에서 수행했다.

| 항목 | 값 |
| --- | --- |
| CPU architecture | `x86_64` |
| Memory | 64 GiB |
| Container runtime | Docker Compose v5.0.2 |
| Host Python | 3.9.6 |
| Application runtime | Java/Spring Boot MSA |
| Retrieval backend | Weaviate hybrid/vector/keyword |
| Embedding profile | `qwen3-embedding:4b` |
| Cross-encoder proof model | `BAAI/bge-reranker-v2-m3` |
| Cross-encoder serving | optional local FastAPI sidecar |

기존 Local RAG stack은 유지한 상태에서 sidecar만 별도 profile로 기동해 직접 smoke를 수행했다. smoke 종료 후 sidecar container는 정지했다.

## 개발 산출물

| 영역 | 산출물 | 설명 |
| --- | --- | --- |
| Selector | `ScoreGateCandidateSelector` | normalized `s_i`/`r_i` 기반 B1-B4 bucket, fusion, retained decision, MAX-K 처리 |
| Offline evaluation | fixture 및 validator | fixed top-K와 ScoreGate 결과 비교 |
| Sidecar | `services/reranker-sidecar` | `/health`, `/rerank` API 제공 |
| Retrieval integration | `LocalRerankerClient`, `HttpLocalRerankerClient` | timeout/fallback을 가진 sidecar client |
| Runtime path | retrieval-service ScoreGate mode | request filter로 `debug` 또는 `on`을 명시할 때만 실행 |
| Snapshot | runtime probes 및 collector | 이후 controlled runtime 측정용 JSON snapshot 수집 |

구현상 deterministic source/path/governance ranking score와 cross-encoder `r_i`를 분리했다. 논문에서 말하는 `r_i`는 query-candidate pair relevance score이므로, Local RAG의 source authority나 path priority 점수와 섞지 않았다.

## 평가 데이터

이 보고서에는 원문 query, source ID, document path, filename, chunk text, citation, document title을 포함하지 않는다.

평가 데이터는 다음 세 층으로 사용했다.

| 층 | 건수 | 용도 | 보고서 노출 수준 |
| --- | ---: | --- | --- |
| Offline ScoreGate fixture | 5 cases, 12 candidates | selector-level before/after 비교 | 집계 지표만 노출 |
| Runtime probe definition | 5 probes, 6 expected references | 추후 controlled runtime snapshot 수집 | 건수만 노출 |
| Sidecar direct smoke | 1 request, 2 synthetic candidates | `/rerank` scoring 동작 확인 | source-derived text 없음 |

offline fixture의 5개 유형은 다음 목적을 커버한다.

- 자연어/용어 불일치형
- 식별자 조회형
- 우선 출처 선호형
- stale/noise 억제형
- 복합 근거 유지형

위 유형명은 평가 목적만 나타내며, 실제 색인 데이터의 내용은 유추할 수 없도록 원문 식별 정보를 제외했다.

## 테스트 방법

before/after 비교는 동일한 candidate snapshot을 사용했다.

Before 조건:

- fixed top-K 방식의 후보 유지
- ScoreGate bucket/fusion 미적용

After 조건:

- normalized similarity와 reranker score를 ScoreGate selector에 입력
- B1-B4 bucket decision 적용
- disagreement zone에서 fusion score 적용
- MAX-K cap 적용

Sidecar proof 조건:

- CPU용 sidecar image build
- `scoregate` compose profile로 sidecar 기동
- `/health` 확인
- synthetic `/rerank` 요청
- warm repeat request latency 측정
- sidecar 정지

검증 명령:

```bash
mvn -q -pl services/retrieval-service -am test
./docs/bin/validate-scoregate-offline.sh
./docs/bin/validate-codex-readiness.sh
./docs/bin/validate-closeout.sh --all
docker compose --env-file .env.example config
docker compose --profile scoregate --env-file .env.example config
python3 -m py_compile docs/bin/collect-scoregate-runtime-snapshot.py services/reranker-sidecar/app/main.py
git diff --check
```

## 결과

Selector-level before/after:

| Metric | Fixed Top-K | ScoreGate | 변화 |
| --- | ---: | ---: | ---: |
| Cases | 5 | 5 | - |
| hit@1 | 20.0% | 80.0% | +60.0 pp |
| hit@5 | 60.0% | 100.0% | +40.0 pp |
| MRR | 0.307 | 0.900 | +0.593 |
| Expected candidate retained | 60.0% | 100.0% | +40.0 pp |
| Multi-evidence coverage | 80.0% | 100.0% | +20.0 pp |
| Known retained token estimate | 1,810 | 1,460 | -350 / -19.3% |
| Must-drop candidates retained | 3 | 0 | -3 |
| Cross-score rescue cases | 0 | 2 | +2 |

Sidecar proof:

| Check | Result |
| --- | --- |
| Sidecar image build | passed |
| `/health` before model load | `UP`, `ready=false` |
| `/rerank` direct smoke | passed |
| `/health` after model load | `UP`, `ready=true` |
| Warm two-candidate CPU request | 약 5.983초 |
| Warm sidecar memory | 약 1.9 GiB |
| Network receive during setup | 약 1.57 GB |

direct smoke에서는 synthetic relevant candidate에 높은 normalized score, synthetic unrelated candidate에 낮은 normalized score가 반환됐다. 이 결과로 Local RAG가 ScoreGate에 필요한 true `r_i` score source를 local sidecar를 통해 확보할 수 있음을 확인했다.

## 결과 해석

ScoreGate의 효과는 retrieval 자체보다 final context selection 품질에서 나타났다.

관측된 개선은 다음으로 요약된다.

- first-stage similarity가 낮아도 cross-encoder relevance가 높은 후보를 유지
- first-stage similarity는 높지만 cross-encoder relevance가 낮은 후보를 제거
- expected retention을 높이면서 retained token estimate 감소
- 복합 근거가 필요한 case에서 coverage 유지

CPU sidecar는 proof, debug, audit, calibration snapshot 용도로 사용할 수 있는 상태다. 다만 warm 2-candidate 기준 약 5.983초가 측정되어, 현 CPU profile에서 기본 always-on 경로로 쓰려면 candidate window, batching, timeout, serving runtime을 별도로 조정해야 한다.

## 적용 상태

| 항목 | 상태 |
| --- | --- |
| Default `rag_search` | 기존 동작 유지 |
| ScoreGate selector | 구현 및 테스트 완료 |
| Offline evaluator | 구현 및 테스트 완료 |
| Reranker sidecar | build 및 direct smoke 완료 |
| Runtime ScoreGate debug/on path | 명시 요청/config 기반으로 구현 |
| Default ScoreGate rollout | 이번 결과 기준 채택 대상 아님 |

다음 측정 단계는 retrieval-service를 reranker enabled 상태로 controlled restart 또는 deploy한 뒤 runtime snapshot collector를 실행하는 것이다. 이때도 source text와 식별 정보는 저장하지 않고 score, bucket, retained count, latency, aggregate quality metric 중심으로 기록한다.

## 데이터 보호 기준

보고서에서 제외한 정보:

- source document name
- file path
- source ID
- query string
- chunk text
- citation
- document title
- local hostname

보고서에 포함한 정보:

- case/probe/candidate 건수
- 익명화된 평가 유형
- 집계 metric
- runtime component
- resource/latency 관측값

## 결론

ScoreGate 논문 아이디어는 Local RAG에 적용 가능한 형태로 구현됐다.

이번 결과의 핵심은 다음이다.

- anonymized 5-case fixture에서 selector-level 품질과 compactness가 개선됐다.
- local cross-encoder sidecar가 normalized `r_i` score를 반환했다.
- CPU sidecar latency는 기본 runtime 승격보다 debug/opt-in 및 calibration-first 운영에 적합한 수준으로 측정됐다.

실무적 결론은 명확하다. Local RAG는 이제 기본 검색 동작을 유지하면서도 real `s_i`/`r_i` runtime evidence를 수집할 수 있는 ScoreGate proof path를 갖췄다.
