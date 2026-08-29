---
type: guide
title: installing-on-a-new-machine
status: active
owner:
created: 2026-05-24
updated: 2026-08-29
related_project: docs/projects/P0001-local-rag-system.md
related_task:
related_design:
  - docs/design/msa-runtime-and-storage.md
source_refs: []
tags:
  - docs/guide
  - install
  - codex
---

# Installing On A New Machine

This guide is the repo-only bootstrap path for a new single-user machine.

## Prerequisites

- Git access to the private repository.
- Docker with Docker Compose.
- Ollama reachable from Docker containers. This can be notebook-local, LAN-local, or both.
- `qwen3-embedding:4b` installed in Ollama, or another embedding model configured in the local env file.
- `qwen3.8:latest` installed for local answer generation, or another chat model explicitly configured in the local env file.
- Node.js if Codex MCP integration will be installed.

For a host-local Ollama, Docker services should use `http://host.docker.internal:11434`. Do not use `http://localhost:11434` inside Compose unless Ollama is running in the same container.

## Developer Checkout

```bash
git clone https://github.com/Deanly/local-rag-system.git
cd local-rag-system
cp .env.example .env
docker compose --env-file .env config
docker compose --env-file .env up -d --build
```

Smoke:

```bash
curl -fsS http://127.0.0.1:42120/api/health
curl -fsS -X POST http://127.0.0.1:42120/api/index/scan
curl -fsS http://127.0.0.1:42120/api/index/status
```

## Operation-Zone Install

From any checkout:

```bash
ollama pull qwen3-embedding:4b
ollama pull qwen3.8:latest
ops/service/local-rag install-command
~/Services/bin/local-rag sync-local "$PWD"
~/Services/bin/local-rag init-config
~/Services/bin/local-rag doctor
```

Edit:

- `~/Services/local-rag-system/config/local.env`
- `~/Services/local-rag-system/config/source-registry.local.yaml`

The generated env starts with notebook-local Ollama via `http://host.docker.internal:11434`. For a desk setup that prefers a Mac mini but still works when the notebook leaves the LAN, put the Mac mini URL first in `LOCAL_RAG_OLLAMA_BASE_URLS` and keep `http://host.docker.internal:11434` second.

Then run:

```bash
~/Services/bin/local-rag deploy
~/Services/bin/local-rag status
```

`local-rag deploy` syncs the configured local checkout or pulls the configured repo, starts Docker Compose with the local env file, and installs the Codex MCP/skill integration.

## Operation-Zone Update

When a development-zone commit is ready but the running service should not be restarted yet, review the operation-zone impact with:

```bash
~/Services/bin/local-rag config
~/Services/bin/local-rag pull
~/Services/bin/local-rag doctor
```

For this task class, deployment requires a stack rebuild/recreate because Spring Boot services and the Codex adapter changed:

```bash
~/Services/bin/local-rag update
~/Services/bin/local-rag force-scan local-rag-system
~/Services/bin/local-rag codex-smoke
```

Restart Codex after `install-codex` so the stdio MCP server and skill are reloaded. Use `local-rag codex-smoke --allow-empty-search` only before the first successful indexing run.

## Source Folder Mapping

Compose mounts one common host folder as `/source` and also provides generic read-only slots under `/sources/source-01` to `/sources/source-08`:

```env
LOCAL_RAG_SOURCE_ROOT=/path/to/source-root
LOCAL_RAG_HOST_SOURCE_04=/path/to/local-rag-system/docs
LOCAL_RAG_HOST_SOURCE_06=/path/to/extra-project-06/docs
LOCAL_RAG_HOST_SOURCE_07=/path/to/extra-project-07/docs
LOCAL_RAG_HOST_SOURCE_08=/path/to/extra-support-wiki
```

Registry source paths must be container paths under `/source` or `/sources`, for example:

```yaml
sources:
  - source_id: project-alpha.docs
    project_id: project-alpha
    path: /source/project-alpha/docs
  - source_id: local-rag-system.docs
    project_id: local-rag-system
    path: /sources/source-04
  - source_id: extra-support-wiki
    project_id: extra-support-wiki
    path: /sources/source-08
```

If real source folders are not under a common parent, create a local compose override with additional read-only mounts and point registry paths at those container mount paths.

The operation command automatically reads this untracked override when it exists:

```text
~/Services/local-rag-system/config/docker-compose.override.yaml
```

## Codex Integration

Install manually:

```bash
./integrations/codex/install-codex-local-rag.sh
```

Or through the operation script:

```bash
~/Services/bin/local-rag install-codex
```

Restart Codex after installation. The installer writes:

- `mcp_servers.local_rag` in `~/.codex/config.toml`
- `~/.codex/skills/local-rag/SKILL.md`

For machine-specific project ids or source names, keep a local skill file outside the repository:

```text
~/Services/local-rag-system/config/codex-skill.local.md
```

`local-rag deploy` and `local-rag install-codex` use that file when it exists. For manual installation, pass `LOCAL_RAG_SKILL_FILE=/path/to/SKILL.md`.

Codex readiness is not proven by config installation alone. Run the project-owned smoke command to verify gateway health, index status, MCP adapter framing, representative `rag_search`, source-safe document fetch, and invalid-project behavior:

```bash
~/Services/bin/local-rag codex-smoke --allow-empty-search
```

Use `--allow-empty-search` before the first successful indexing run. Remove it after sources are indexed so the smoke fails when retrieval is not returning evidence.

## Expected Model Settings

Default:

```env
LOCAL_RAG_OLLAMA_BASE_URL=http://host.docker.internal:11434
LOCAL_RAG_OLLAMA_BASE_URLS=
LOCAL_RAG_OLLAMA_CONNECT_TIMEOUT_MILLIS=1500
LOCAL_RAG_OLLAMA_READ_TIMEOUT_MILLIS=120000
LOCAL_RAG_EMBEDDING_MODEL=qwen3-embedding:4b
LOCAL_RAG_EMBEDDING_FALLBACK_ENABLED=false
LOCAL_RAG_WATCH_DEBOUNCE_SECONDS=10
```

For a notebook that sometimes leaves the local network, keep the host-local endpoint available as a fallback. To prefer the Mac mini when reachable, set the ordered list in the untracked env file:

```env
LOCAL_RAG_OLLAMA_BASE_URLS=http://mac-mini-host.local:11434,http://host.docker.internal:11434
```

To keep notebook-local Ollama primary and only use the Mac mini as a secondary endpoint:

```env
LOCAL_RAG_OLLAMA_BASE_URLS=http://host.docker.internal:11434,http://mac-mini-host.local:11434
```

Install the same embedding model on every endpoint in the ordered list. If `LOCAL_RAG_EMBEDDING_MODEL` changes later, force a reindex before trusting vector results.

Fallback embeddings are deterministic placeholders for development smoke only. Do not use them for real indexing.

Watcher debounce waits for the source folders to be quiet before scanning. A 10 second default avoids repeated embedding calls during editor autosave bursts while still keeping interactive edits reasonably fresh. Periodic scan remains the fallback freshness authority.
