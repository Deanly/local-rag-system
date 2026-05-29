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

- official baseline version `1.0.0`
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
- `docs/guide/installing-on-a-new-machine.md`
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
- Provide hybrid keyword/vector search with citations.
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

Prerequisites:

- Docker with Docker Compose
- Ollama running on the host, a reachable LAN host, or both
- `qwen3-embedding:4b` installed in Ollama, or another embedding model configured with `LOCAL_RAG_EMBEDDING_MODEL`

For a host-local Ollama on Docker Desktop or a recent Linux Docker engine, containers should use `http://host.docker.internal:11434`, not `http://localhost:11434`. Inside a container, `localhost` means the container itself.

Prepare Ollama:

```bash
ollama pull qwen3-embedding:4b
curl -fsS http://127.0.0.1:11434/api/tags
```

Create a local `.env` and point source host variables at the folders that should be mounted read-only into the containers. The sample registry uses paths under `/source` and registers:

- `personal-notes`
- `project-alpha.docs`
- `project-beta.docs`
- `local-rag-system.docs`

```bash
cp .env.example .env
# edit LOCAL_RAG_SOURCE_ROOT, LOCAL_RAG_DATA_DIR, and LOCAL_RAG_HOST_SOURCE_* when needed
# keep real host paths in .env or a local Compose override
# optionally copy config/source-registry.local.example.yaml to
# config/source-registry.local.yaml and set LOCAL_RAG_SOURCE_REGISTRY
docker compose --env-file .env config
docker compose --env-file .env up -d --build
```

The default gateway URL is `http://127.0.0.1:42120`.

Smoke commands with the local registry:

```bash
curl -fsS http://127.0.0.1:42120/api/health
curl -fsS -X POST http://127.0.0.1:42120/api/index/scan
curl -fsS -X POST http://127.0.0.1:42120/api/search \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"project-alpha","query":"source registry","limit":3,"mode":"hybrid"}'
curl -fsS -X POST http://127.0.0.1:42120/api/answer \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"project-alpha","query":"registered source 운영 기준을 요약해줘","limit":3,"mode":"hybrid"}'
node integrations/codex/smoke-local-rag.mjs --adapter-only
```

The tracked Compose file provides generic read-only source slots for machine-local registry paths:

- `/sources/source-01`: `LOCAL_RAG_HOST_SOURCE_01`
- `/sources/source-02`: `LOCAL_RAG_HOST_SOURCE_02`
- `/sources/source-03`: `LOCAL_RAG_HOST_SOURCE_03`
- `/sources/source-04`: `LOCAL_RAG_HOST_SOURCE_04`
- `/sources/source-05`: `LOCAL_RAG_HOST_SOURCE_05`

For other real documents, copy `config/source-registry.local.example.yaml` to `config/source-registry.local.yaml`, set `LOCAL_RAG_SOURCE_REGISTRY=/config/source-registry.local.yaml`, and edit source `path` values to container paths under `/source` or `/sources`. Keep host paths in `.env` or a local Compose override, not in committed registry examples.

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

File watcher events are debounced before indexing because indexing calls the configured embedding model. The default is 10 seconds after the last filesystem event:

```env
LOCAL_RAG_WATCH_DEBOUNCE_SECONDS=10
```

Periodic scan remains the final freshness authority and defaults to 300 seconds.

## Ollama Prerequisite

The Docker Compose default expects host Ollama to be reachable from containers on `http://host.docker.internal:11434` with `LOCAL_RAG_EMBEDDING_FALLBACK_ENABLED=false`.

Use `LOCAL_RAG_OLLAMA_BASE_URLS` when this notebook should use more than one local/LAN-local Ollama endpoint. Values are comma-separated and tried in order for both embeddings and chat. Keep real hostnames and IPs in the local `.env` file, not in committed files.

Portable notebook first, Mac mini second:

```env
LOCAL_RAG_OLLAMA_BASE_URL=http://host.docker.internal:11434
LOCAL_RAG_OLLAMA_BASE_URLS=http://host.docker.internal:11434,http://mac-mini-host.local:11434
```

Mac mini preferred when it is reachable, notebook fallback when away from the desk:

```env
LOCAL_RAG_OLLAMA_BASE_URL=http://host.docker.internal:11434
LOCAL_RAG_OLLAMA_BASE_URLS=http://mac-mini-host.local:11434,http://host.docker.internal:11434
```

`LOCAL_RAG_OLLAMA_CONNECT_TIMEOUT_MILLIS` controls how quickly the client moves past an unreachable endpoint. The default is `1500`, which keeps remote-first profiles usable when the notebook leaves the local network. `LOCAL_RAG_OLLAMA_READ_TIMEOUT_MILLIS` defaults to `120000` so local answer generation has enough time to complete.

`qwen3-embedding:4b` is the current embedding baseline because it is materially faster and lighter than `qwen3-embedding:8b` for large indexing runs while remaining multilingual and compatible with the current Ollama `/api/embeddings` endpoint. Device-specific endpoints, such as a directly connected LAN Ollama host, belong in the local env file and should not be committed.

Install the same embedding model on every endpoint in `LOCAL_RAG_OLLAMA_BASE_URLS`. If you change `LOCAL_RAG_EMBEDDING_MODEL`, force a reindex so Weaviate does not mix vectors from different model dimensions or distributions.

Set `LOCAL_RAG_CHAT_MODEL` in the local env file to enable `/api/answer`. The answer path retrieves local chunks first, then sends only those retrieved snippets to the configured local Ollama chat model.

Fallback embeddings are for development smoke only. Do not use fallback for production indexing because fallback vectors are deterministic placeholders, not semantic embeddings.

## Operations

Development happens from a normal workspace checkout. For a single-user host deployment, the provided operation command runs under `~/Services/local-rag-system` and reads host-specific configuration from `~/Services/local-rag-system/config/local.env`.

Install the service command:

```bash
ops/service/local-rag install-command
~/Services/bin/local-rag sync-local "$PWD"
~/Services/bin/local-rag init-config
~/Services/bin/local-rag doctor
```

Common operations:

```bash
~/Services/bin/local-rag deploy
~/Services/bin/local-rag start
~/Services/bin/local-rag status
~/Services/bin/local-rag update
~/Services/bin/local-rag codex-smoke --allow-empty-search
~/Services/bin/local-rag force-scan
~/Services/bin/local-rag logs
~/Services/bin/local-rag stop
```

## Codex Integration

Codex needs Node.js for the MCP stdio adapter and a running Local RAG gateway. Install the global Codex integration from this repo:

```bash
./integrations/codex/install-codex-local-rag.sh
```

The installer registers a `local_rag` MCP stdio server in `~/.codex/config.toml` and installs the `local-rag` skill under `~/.codex/skills/local-rag`. Restart Codex after installation.

If the local registry has a preferred default project, set `LOCAL_RAG_DEFAULT_PROJECT_ID` during install. Otherwise the gateway's configured default project is used.

After installation, run `node integrations/codex/smoke-local-rag.mjs --adapter-only` to verify MCP framing and advertised tools. Once the stack is up and indexed, run `local-rag codex-smoke` or `node integrations/codex/smoke-local-rag.mjs` to verify gateway health, registry listing, index status, `rag_search`, source-safe `rag_get_document`, and unknown-project 400 behavior.
