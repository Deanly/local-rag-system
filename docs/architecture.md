# Architecture

Local RAG System is split into target MSA services plus supporting storage.

The source registry service decides which local folders are eligible knowledge sources on this device. It maps stable `project_id` values to registered source roots.

The indexer service watches and scans registered folders, reads changed files, chunks supported formats, calls local embeddings, and upserts/deletes retrieval chunks.

The retrieval service runs hybrid BM25/vector search, applies filters, deduplicates results, and returns citations.

The API gateway exposes the local REST API for CLI/UI consumers.

The MCP bridge exposes Codex-facing tools without mixing MCP protocol concerns into retrieval or indexing.

PostgreSQL stores source registry, document state, index jobs, failures, and search audit. Weaviate stores the derived hybrid retrieval index. Ollama remains the local model endpoint.

The index is derived state, not the source of truth. Registered source folders remain the source of truth.
