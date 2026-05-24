# Security Policy

Local RAG System is designed to index local folders. Do not commit real source registries, local environment files, indexed data, or generated runtime state.

## Public Repository Rules

- Keep `.env`, `.env.*`, and `config/*.local.yaml` untracked.
- Use `config/source-registry.local.example.yaml` for portable examples only.
- Do not commit absolute user paths, private network endpoints, real project names, indexed document content, Weaviate data, PostgreSQL data, logs, or model cache files.
- Put host-specific Ollama, oMLX, source-folder, and operation-zone settings in an untracked env file.

## Reporting

If you find a secret, private document excerpt, or host-specific identifier in tracked files, remove it from the working tree and rotate any exposed credential before publishing a new release.
