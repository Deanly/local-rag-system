# source-registry-service

Machine-local source registry service.

Responsibilities:

- Load `source-registry.yaml` from `LOCAL_RAG_SOURCE_REGISTRY`.
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
