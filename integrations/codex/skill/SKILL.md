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
- questions about any project or note source currently returned by `rag_list_projects` and `rag_list_sources`

Do not use Local RAG for general internet facts, current news, package docs, prices, schedules, or other external facts. Use web or official docs for those.

## Preferred Tool Path

If MCP tools are available, use them:

- `mcp__local_rag__rag_search`
- `mcp__local_rag__rag_answer`
- `mcp__local_rag__rag_get_document`
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
2. Public search modes are `hybrid`, `vector`, and `keyword`; treat old `bm25` wording as `keyword`.
3. Prefer concise queries that include project/task/design terms.
4. If results are noisy, narrow with `includeSourceIds` using ids returned by `rag_list_sources`.
5. Cite the returned `citation` fields in the answer when using retrieved context.
6. Use `rag_get_document` only for a returned `sourceId` and `relativePath` when a full registered source document is needed.
7. Use `rag_answer` when the user wants a synthesized answer and the installed profile has an explicit answer model. If it returns `ANSWER_GENERATION_DISABLED`, use `rag_search` and synthesize from its citations in the current authorized reasoning boundary; do not retry through a Voice or Trade binding.
8. If freshness matters, check `rag_index_status`; use `rag_force_scan` only when the user asks for immediate sync or the answer depends on just-changed files.

## Fallback Practice

If MCP tools are not exposed in the current Codex session, use the REST examples above instead of answering from memory. If the gateway is unavailable, say that Local RAG could not be reached and continue only with explicitly available repository files or user-provided context.

## Response Rule

When Local RAG influenced the answer, mention the retrieved source citations briefly. Do not present retrieved content as if it came from model memory.
