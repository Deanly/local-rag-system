# source-registry-service

Machine-local source registry service.

Responsibilities:

- Load the registry selected by the Spring property `local-rag.registry.path`
  (`LOCAL_RAG_REGISTRY_PATH` in the canonical environment contract).
- Validate project ids, source ids, absolute paths, include/exclude rules, active flags, read/write policies, and SSOT roles.
- Normalize registry entries into PostgreSQL tables.
- Resolve search scopes from `projectId`, `includeSourceIds`, `excludeSourceIds`, and filters.
- Prevent unregistered folders from entering indexing/search.

Primary storage:

- `project_registration`
- `source_root`
- `device_profile`

Initial endpoints:

```http
GET  /api/registry/projects
GET  /api/registry/sources
POST /api/registry/validate
POST /api/registry/reload
```

Out of scope:

- File scanning.
- Search execution.
- Source file writes.
