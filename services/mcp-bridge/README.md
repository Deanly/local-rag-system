# mcp-bridge

Codex-facing MCP bridge.

Responsibilities:

- Expose MCP tools over stdio or HTTP, depending on Codex runtime support.
- Forward tool calls to `retrieval-service` and `indexer-service`.
- Keep MCP protocol concerns separate from retrieval/indexing logic.

Initial tools:

```text
rag_list_projects
rag_list_sources
rag_search
rag_answer
rag_get_document
rag_index_status
rag_force_scan
```

Out of scope:

- Direct file access.
- Embedding generation.
- Search ranking.
- Durable state.
