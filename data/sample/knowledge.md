# Local RAG Notes

The local RAG system indexes registered files from disk into PostgreSQL document state and Weaviate chunk search storage.

The MVP supports Markdown and plain text files from read-only local source folders.

Search uses Weaviate hybrid retrieval with BM25 and external vectors generated through local Ollama embeddings.

The Spring Boot API gateway runs on localhost and exposes index status, force scan, and citation-bearing search endpoints.

The indexer also watches filesystem changes and refreshes changed files without requiring a manual force scan.
