# Storage Contracts

This directory holds runtime storage contracts for the local RAG system.

- `postgres/ddl/001_core_schema.sql`: source registry, document state, chunk state, index jobs, failures, and retrieval-debuggable search audit.
- `weaviate/local-rag-chunk.schema.json`: Weaviate collection contract for hybrid BM25/vector retrieval and document authority metadata.

PostgreSQL is the control/state store. Weaviate is the derived retrieval index. Both are rebuildable from registered source folders, but PostgreSQL state is retained to track freshness, metadata migration version, failures, and operations.
