# indexer-service

Source folder freshness and indexing pipeline.

Responsibilities:

- Watch registered local source folders using best-effort file events.
- Periodically scan source folders as the freshness authority.
- Compare path, size, mtime, and content hash.
- Track `document_state`, `index_job`, `chunk_state`, and `failure_record`.
- Parse Markdown first, then later support additional file types.
- Create heading-aware chunks.
- Generate embeddings through the configured local Ollama endpoint.
- Upsert/delete chunks in Weaviate.

Initial endpoints:

```http
GET  /api/index/status
POST /api/index/scan
POST /api/index/force
POST /api/documents/get
```

`POST /api/documents/get` is a source-safe read endpoint for Codex and local tools. It accepts `sourceId` and `relativePath`, then rejects absolute paths, path traversal, unknown source ids, and files excluded by registry filters.

Out of scope:

- User-facing search ranking.
- MCP protocol.
- Source file writes.

Freshness invariant:

Watcher events improve latency, but periodic scan is the authority. A missed event must be repaired by the next scan.
