# api-gateway

Public local API entrypoint bound to `127.0.0.1:42120`.

Responsibilities:

- Expose stable local REST endpoints for CLI, future UI, and automation.
- Delegate registry reads to `source-registry-service`.
- Delegate indexing operations to `indexer-service`.
- Delegate search and answer generation to `retrieval-service`.
- Keep local-only network binding as the default.

Initial endpoints:

```http
GET  /api/health
GET  /api/registry/projects
GET  /api/registry/sources
GET  /api/index/status
POST /api/index/scan
POST /api/index/force
POST /api/search
POST /api/answer
POST /api/documents/get
GET  /api/documents/{documentId}
GET  /api/mcp/rag_list_projects
GET  /api/mcp/rag_list_sources
POST /api/mcp/rag_search
POST /api/mcp/rag_answer
POST /api/mcp/rag_get_document
GET  /api/mcp/rag_index_status
POST /api/mcp/rag_force_scan
```

`GET /api/documents/{documentId}` remains a compatibility placeholder. Source-safe document reads use `POST /api/documents/get` or the MCP bridge equivalent.

Out of scope:

- Document parsing.
- Embedding generation.
- Direct Weaviate access.
- Durable source registry storage.
