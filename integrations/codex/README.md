# Codex Integration

This directory contains the installable Codex integration for `local-rag-system`.

It installs two global surfaces on this machine:

- `local_rag` MCP stdio server in `~/.codex/config.toml`
- `local-rag` Codex skill in `~/.codex/skills/local-rag/SKILL.md`

The MCP server is a thin Node.js stdio adapter. It does not index or search by itself; it forwards tool calls to the running local gateway at `http://127.0.0.1:42120`.

## Install

```bash
./integrations/codex/install-codex-local-rag.sh
```

Then restart Codex so the global MCP server and skill are loaded.

Optional environment:

```bash
LOCAL_RAG_BASE_URL=http://127.0.0.1:42120 \
LOCAL_RAG_DEFAULT_PROJECT_ID=project-alpha \
./integrations/codex/install-codex-local-rag.sh
```

## Uninstall

```bash
./integrations/codex/install-codex-local-rag.sh --uninstall
```

## Installed MCP Tools

- `rag_search`
- `rag_list_projects`
- `rag_list_sources`
- `rag_index_status`
- `rag_force_scan`

`rag_get_document` is intentionally not exposed yet because the current REST bridge does not implement a source-safe document fetch endpoint.

## Manual Smoke

```bash
curl -fsS http://127.0.0.1:42120/api/index/status
curl -fsS -X POST http://127.0.0.1:42120/api/mcp/rag_search \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"project-alpha","query":"source registry hybrid retrieval","limit":3,"mode":"hybrid"}'
```
