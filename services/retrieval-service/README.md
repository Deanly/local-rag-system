# retrieval-service

Hybrid retrieval and optional local answer generation.

Responsibilities:

- Normalize user queries.
- Resolve search scope through source registry metadata.
- Execute Weaviate hybrid BM25/vector search.
- Apply metadata filters.
- Deduplicate by document and chunk hash.
- Return score breakdown, source metadata, snippets, and citations.
- Optionally call local Ollama chat model using retrieved context.
- Persist `search_audit` records.

Initial endpoints:

```http
POST /api/search
GET  /api/documents/{documentId}
POST /api/answer
```

Out of scope:

- File watching.
- Document chunking.
- Registry mutation.

Retrieval invariant:

Search results must include enough metadata to explain where the answer came from and whether it is current project truth, compiled knowledge, raw source, or historical evidence.
