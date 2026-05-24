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
GET  /api/index/failures
POST /api/search
GET  /api/documents/{documentId}
```

Out of scope:

- Document parsing.
- Embedding generation.
- Direct Weaviate access.
- Durable source registry storage.
