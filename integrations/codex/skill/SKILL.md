---
name: "local-rag"
description: "Use when the user asks about local project knowledge, personal-notes compiled wiki content, project-alpha docs, project-beta docs, or asks to search indexed local documents before answering or editing."
---

# Local RAG

Use this skill to retrieve local, source-backed context from the `local-rag-system` running on this machine.

## When To Use

Use Local RAG before answering or editing when the request depends on indexed local knowledge from:

- `personal-notes`
- `project-alpha`
- `project-beta`
- cross-project planning, task, design, and report documents that may be in the registered local source folders

Use it especially for:

- "what did we decide", "current status", "related docs", "find the task/design/report"
- project-specific questions where local docs are more authoritative than model memory
- code changes that should respect project design/task documents
- checking stale assumptions before summarizing a project

Do not use Local RAG for general internet facts, current news, package docs, prices, schedules, or other external facts. Use web or official docs for those.

## Preferred Tool Path

If MCP tools are available, use them:

- `mcp__local_rag__rag_search`
- `mcp__local_rag__rag_list_projects`
- `mcp__local_rag__rag_list_sources`
- `mcp__local_rag__rag_index_status`
- `mcp__local_rag__rag_force_scan`

If MCP tools are not available in the current session, use the REST bridge with shell:

```bash
curl -fsS -X POST http://127.0.0.1:42120/api/mcp/rag_search \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"project-alpha","query":"source registry hybrid retrieval","limit":5,"mode":"hybrid"}'
```

## Project Ids

- `personal-notes`: compiled wiki support layer
- `project-alpha`: Project Alpha docs, with `personal-notes` as support context
- `project-beta`: Project Beta docs, with `personal-notes` as support context

When the current workspace path is under:

- `/path/to/workspace/project-alpha`, default to `projectId: "project-alpha"`.
- `/path/to/workspace/project-beta`, default to `projectId: "project-beta"`.
- `/path/to/personal-notes`, default to `projectId: "personal-notes"`.

If the user asks for cross-project context, either omit `projectId` or run multiple focused searches.

## Search Practice

1. Start with `mode: "hybrid"` and `limit: 5`.
2. Prefer concise queries that include project/task/design terms.
3. If results are noisy, narrow with `includeSourceIds`:
   - `personal-notes`
   - `project-alpha.docs`
   - `project-beta.docs`
4. Cite the returned `citation` fields in the answer when using retrieved context.
5. If freshness matters, check `rag_index_status`; use `rag_force_scan` only when the user asks for immediate sync or the answer depends on just-changed files.

## Response Rule

When Local RAG influenced the answer, mention the retrieved source citations briefly. Do not present retrieved content as if it came from model memory.
