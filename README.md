# Local RAG System

A local-only retrieval augmented generation system for indexing registered macOS source folders, detecting file changes, and providing hybrid retrieval through local services.

The runtime is an internal MSA-style Docker Compose system:

- Java/Spring Boot application services under `services/`
- PostgreSQL control/state store under `database/postgres/ddl/`
- Weaviate hybrid retrieval index under `database/weaviate/`
- Ollama as the local embedding/chat endpoint
- source folders mounted read-only

The earlier Python BM25 scaffold has been removed from the active runtime surface. This repository now treats Java/Spring Boot MSA services as the only implementation target.

## Current Status

The functional baseline is implemented and smoke-tested:

- source registry validation and project-scoped source resolution
- file watcher plus periodic scan fallback
- Markdown/plain text chunk indexing
- PostgreSQL document/chunk/audit state
- Weaviate keyword/vector/hybrid retrieval
- gateway API on `127.0.0.1:42120`
- MCP REST bridge endpoints through the gateway

Smoke evidence is recorded in `docs/reports/2026-05-24-local-rag-functional-smoke.md`.

## Project Goal

Build a one-person local-only RAG system that watches registered macOS local source folders read-only, incrementally indexes changed Markdown files through local Ollama embeddings, stores chunk search data in Weaviate, stores operational state in PostgreSQL, and exposes citation-bearing hybrid search and index status through Spring Boot MSA services managed by Docker Compose.

SDLC automation should start from `docs/guide/sdlc-automation.md` and the single active critical-path task in `docs/tasks/README.md`.

Authoritative design:

- `docs/design/control-plane.md`
- `docs/guide/sdlc-automation.md`
- `docs/design/local-rag-system-development-direction.md`
- `docs/design/source-registry-and-project-ssot.md`
- `docs/design/msa-runtime-and-storage.md`

## Target Features

- Register local source folders by stable `project_id` and `source_id`.
- Watch and periodically scan registered folders.
- Re-index changed files only.
- Delete stale chunks when source files are removed.
- Store registry, document state, jobs, failures, and audit records in PostgreSQL.
- Store searchable chunks in Weaviate with external vectors.
- Use local Ollama embedding/chat models only.
- Provide hybrid BM25/vector search with citations.
- Expose one local API surface for CLI, UI, and Codex/MCP adapters.

## Target Runtime Layout

```text
services/
  api-gateway/
  source-registry-service/
  indexer-service/
  retrieval-service/
  mcp-bridge/
database/
  postgres/ddl/
  weaviate/
config/
  source-registry.example.yaml
  source-registry.local.example.yaml
docker-compose.yml
.env.example
```

## Docker Compose

Create a local `.env` and point each source variable at the folders to index. The local example registers:

- `personal-notes`
- `project-alpha.docs`
- `project-beta.docs`

```bash
cp .env.example .env
# edit LOCAL_RAG_PERSONAL_NOTES_DIR, LOCAL_RAG_PROJECT_ALPHA_DOCS_DIR,
# LOCAL_RAG_PROJECT_BETA_DOCS_DIR, and LOCAL_RAG_DATA_DIR when needed
# optionally copy config/source-registry.local.example.yaml to
# config/source-registry.local.yaml and set LOCAL_RAG_SOURCE_REGISTRY
docker compose --env-file .env config
docker compose --env-file .env up -d --build
```

The default gateway URL is `http://127.0.0.1:42120`.

## Development

Current validation:

```bash
docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -q test
docker compose --env-file .env.example config
./docs/bin/validate-codex-readiness.sh
./docs/bin/validate-closeout.sh --all
```

The current build is a Maven multi-module build. If Maven is not installed locally, use Docker Maven or the service Dockerfiles during Compose build.

## Source Scope

The registry is the only indexing boundary. Source folders are mounted read-only into the indexer and registry services, and `include`/`exclude` globs are enforced by the scanner before a supported `.md`, `.markdown`, or `.txt` file can be indexed.

When a search request includes `projectId`, retrieval uses the project's active `primary_source_id`, ordered `default_context`, and other active project sources. This allows project docs to remain the primary truth while `personal-notes` can act as a lower-priority cross-project support source.

## Ollama Prerequisite

The portable default expects Ollama on `http://localhost:11434` with `LOCAL_RAG_EMBEDDING_FALLBACK_ENABLED=false`.

`qwen3-embedding:4b` is the current embedding baseline because it is materially faster and lighter than `qwen3-embedding:8b` for large indexing runs while remaining multilingual and compatible with the current Ollama `/api/embeddings` endpoint. Device-specific endpoints, such as a directly connected LAN Ollama host, belong in the local env file and should not be committed.

Fallback embeddings are for development smoke only. Do not use fallback for production indexing because fallback vectors are deterministic placeholders, not semantic embeddings.

## Operations

Development happens from a normal workspace checkout. For a single-user host deployment, the provided operation command can run from `~/Service/code/local-rag-system` and read host-specific configuration from `~/Service/config/local-rag-system/local.env`.

Install the service command:

```bash
install -m 0755 ops/service/local-rag ~/Service/bin/local-rag
```

Common operations:

```bash
local-rag deploy
local-rag status
local-rag force-scan
local-rag logs
local-rag down
```

## Codex Integration

Install the global Codex integration from this repo:

```bash
./integrations/codex/install-codex-local-rag.sh
```

The installer registers a `local_rag` MCP stdio server in `~/.codex/config.toml` and installs the `local-rag` skill under `~/.codex/skills/local-rag`. Restart Codex after installation.
