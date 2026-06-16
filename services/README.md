# Services

The target runtime is an internal MSA-style local system managed by Docker Compose.

All application services are Java/Spring Boot Maven subprojects. The earlier Python BM25 scaffold is not part of the active runtime surface.

## Service Map

| Service | Responsibility | Owns State | Talks To |
| --- | --- | --- | --- |
| `api-gateway` | Public local REST surface for CLI/UI/Codex adapters | No | registry, indexer, retrieval, mcp bridge |
| `source-registry-service` | Load and validate machine-local source registry | PostgreSQL registry tables | PostgreSQL |
| `indexer-service` | Watch/scan source folders, chunk documents, call embeddings, upsert/delete index | PostgreSQL state/job tables, Weaviate chunks | PostgreSQL, Weaviate, Ollama |
| `retrieval-service` | Hybrid retrieval, filters, citations, optional answer generation | search audit | Weaviate, PostgreSQL, Ollama |
| `mcp-bridge` | Codex-facing MCP-style REST bridge | No durable state | registry, retrieval, indexer |

## Build Boundary

Each service directory is a Spring Boot Maven subproject. Dockerfiles use a multi-stage Maven build and run the resulting service jar.

Expected artifact convention:

```text
services/<service-name>/target/<service-name>-<project-version>.jar
```

The implemented baseline includes real Spring Boot source, focused tests, Docker build output, and smoke-tested registry/index/search endpoints.
