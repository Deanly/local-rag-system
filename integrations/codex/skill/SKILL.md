---
name: "local-rag"
description: "Use when the user asks about indexed local knowledge from registered local sources, or asks to search local documents before answering or editing."
---

# Local RAG

Use this skill to retrieve local, source-backed context from the `local-rag-system` running on this machine.

## When To Use

Use Local RAG before answering or editing when the request depends on indexed local knowledge from the machine's registered source registry, such as project docs, local notes, exported wiki content, or other private filesystem sources.

Use it especially for:

- "what did we decide", "current status", "related docs", "find the task/design/report"
- project-specific questions where local docs are more authoritative than model memory
- code changes that should respect project design/task documents
- checking stale assumptions before summarizing a project

Do not use Local RAG for general internet facts, current news, package docs, prices, schedules, or other external facts. Use web or official docs for those.

## Preferred Tool Path

If MCP tools are available, use them:

- `mcp__local_rag__rag_search`
- `mcp__local_rag__rag_answer`
- `mcp__local_rag__rag_list_projects`
- `mcp__local_rag__rag_list_sources`
- `mcp__local_rag__rag_index_status`
- `mcp__local_rag__rag_force_scan`

If MCP tools are not available in the current session, use the REST bridge with shell:

```bash
curl -fsS -X POST http://127.0.0.1:42120/api/mcp/rag_search \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"project-alpha","query":"source registry","limit":5,"mode":"hybrid"}'
```

## Project Ids

Project ids and source ids are registry-defined and differ by machine. Use `rag_list_projects` and `rag_list_sources` when the right id is not obvious.

If `LOCAL_RAG_DEFAULT_PROJECT_ID` was configured during install, `projectId` can be omitted for default searches. If the user asks for cross-project context, either omit `projectId` or run multiple focused searches using ids returned by the registry.

## Search Practice

1. Start with `mode: "hybrid"` and `limit: 5`.
2. Prefer concise queries that include project/task/design terms.
3. If results are noisy, narrow with `includeSourceIds` using ids returned by `rag_list_sources`.
4. Cite the returned `citation` fields in the answer when using retrieved context.
5. Use `rag_answer` when the user wants a synthesized answer from indexed local evidence.
6. If freshness matters, check `rag_index_status`; use `rag_force_scan` only when the user asks for immediate sync or the answer depends on just-changed files.

## Response Rule

When Local RAG influenced the answer, mention the retrieved source citations briefly. Do not present retrieved content as if it came from model memory.
