---
type: guide
title: installing-on-a-new-machine
status: active
owner:
created: 2026-05-24
updated: 2026-05-24
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
- Ollama reachable from Docker containers.
- `qwen3-embedding:4b` installed in Ollama, or another embedding model configured in the local env file.
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
install -m 0755 ops/service/local-rag ~/Service/bin/local-rag
local-rag init-config
local-rag doctor
```

Edit:

- `~/Service/config/local-rag-system/local.env`
- `~/Service/config/local-rag-system/source-registry.local.yaml`

Then run:

```bash
local-rag deploy
local-rag status
```

`local-rag deploy` clones or pulls `~/Service/code/local-rag-system`, starts Docker Compose with the local env file, and installs the Codex MCP/skill integration.

## Source Folder Mapping

Compose mounts one host folder as `/source`:

```env
LOCAL_RAG_SOURCE_ROOT=/path/to/source-root
```

Registry source paths must be container paths under `/source`, for example:

```yaml
sources:
  - source_id: project-alpha.docs
    project_id: project-alpha
    path: /source/project-alpha/docs
```

If real source folders are not under a common parent, create a local compose override with additional read-only mounts and point registry paths at those container mount paths.

The operation command automatically reads this untracked override when it exists:

```text
~/Service/config/local-rag-system/docker-compose.override.yaml
```

## Codex Integration

Install manually:

```bash
./integrations/codex/install-codex-local-rag.sh
```

Or through the operation script:

```bash
local-rag install-codex
```

Restart Codex after installation. The installer writes:

- `mcp_servers.local_rag` in `~/.codex/config.toml`
- `~/.codex/skills/local-rag/SKILL.md`

## Expected Model Settings

Default:

```env
LOCAL_RAG_OLLAMA_BASE_URL=http://host.docker.internal:11434
LOCAL_RAG_EMBEDDING_MODEL=qwen3-embedding:4b
LOCAL_RAG_EMBEDDING_FALLBACK_ENABLED=false
```

Fallback embeddings are deterministic placeholders for development smoke only. Do not use them for real indexing.
