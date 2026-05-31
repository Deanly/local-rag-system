---
type: report
title: local-rag-functional-smoke
status: done
owner:
created: 2026-05-24
updated: 2026-05-24
related_project: docs/projects/P0001-local-rag-system.md
related_task:
  - docs/tasks/T0001-source-registry-project-ssot-registration.md
  - docs/tasks/T0003-spring-boot-msa-skeleton.md
  - docs/tasks/T0004-local-rag-functional-baseline.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - docker-compose.yml
  - config/source-registry.example.yaml
  - data/sample/knowledge.md
quality_axes:
  - EVIDENCE
  - WHOLE
  - SECURITY
tags:
  - docs/report
  - local-rag-system
  - smoke
---

# 2026-05-24 local-rag-functional-smoke

## Summary

`local-rag-system` now has a runnable Spring Boot MSA baseline under Docker Compose.

Verified runtime path:

- PostgreSQL and Weaviate start through Compose.
- Five Spring Boot services start: `api-gateway`, `source-registry-service`, `indexer-service`, `retrieval-service`, `mcp-bridge`.
- Gateway health aggregates downstream health at `http://127.0.0.1:42120/api/health`.
- Source registry YAML validates with one project and one source.
- Force scan indexes the sample Markdown file.
- File watch detects sample file modification without force scan.
- File watch detects temporary sample file creation and deletion.
- Search returns citation-bearing hybrid results.
- MCP REST bridge path returns search results through the gateway.

## Commands

```bash
docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -q test

LOCAL_RAG_SOURCE_DIR="$PWD/data/sample" \
LOCAL_RAG_DATA_DIR="$PWD/.runtime/smoke" \
LOCAL_RAG_EMBEDDING_FALLBACK_ENABLED=true \
LOCAL_RAG_SCAN_INTERVAL_MILLIS=300000 \
docker compose --env-file .env.example up -d --build

curl -fsS http://127.0.0.1:42120/api/health | jq .
curl -fsS -X POST http://127.0.0.1:42120/api/registry/validate | jq .
curl -fsS -X POST 'http://127.0.0.1:42120/api/index/force?projectId=personal-notes' | jq .
curl -fsS http://127.0.0.1:42120/api/index/status | jq .
curl -fsS -X POST http://127.0.0.1:42120/api/search \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"personal-notes","query":"Weaviate hybrid retrieval local Ollama embeddings","limit":5,"mode":"hybrid"}' | jq .
curl -fsS -X POST http://127.0.0.1:42120/api/mcp/rag_search \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"personal-notes","query":"source registry and hybrid retrieval","limit":3,"mode":"hybrid"}' | jq .
```

## Evidence

- Full Maven test suite passed.
- Compose build succeeded and all runtime containers reached `Up`.
- Gateway health returned `UP` for all application services.
- Registry validation returned `valid: true`, `projects: 1`, `sources: 1`.
- Force scan returned `documentsDetected: 1`, `documentsIndexed: 1`, `chunksIndexed: 1`.
- Index status returned `documentsByStatus.indexed: 1` and `chunks: 1`.
- Hybrid search returned `knowledge.md#Local RAG Notes` with snippet and score.
- MCP bridge search through gateway returned the same citation-bearing result shape.
- Modification watch was verified by adding text to `data/sample/knowledge.md` and observing search snippet refresh without force scan.
- Creation watch was verified by adding `data/sample/auto-detection-smoke.md` and observing a searchable citation.
- Deletion watch was verified by deleting `data/sample/auto-detection-smoke.md`; status moved one document to `removed`, chunk count returned to `1`, and the deleted file stopped appearing in results.

## Runtime Note

The smoke run used `LOCAL_RAG_EMBEDDING_FALLBACK_ENABLED=true` so the system remains testable before the external local Ollama embedding model is installed.

Observed Ollama status:

- The operator-local Ollama endpoint was reachable.
- The alternate operator-local endpoint was not reachable from that machine.
- `bge-m3` was not installed on the operator-local endpoint; `/api/embeddings` returned `model "bge-m3" not found`.

This was the first functional smoke state. The later deployment baseline was updated to `qwen3-embedding:4b` on the operator-local Ollama endpoint with fallback disabled after the embedding model became available. The endpoint names from that run are intentionally omitted because they were local-only.
