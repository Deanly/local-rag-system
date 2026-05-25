# Active Docs Index

This is a semi-generated index maintained with the active README files and validated by `docs/bin/validate-doc-retrieval.sh`.

It is a context-selection surface for LLM/Codex runs, not a replacement for source documents.

## Active Projects

| ID | Title | Status | Source | Related Tasks | Updated |
| --- | --- | --- | --- | --- | --- |
| _none_ | _none_ | _none_ | _none_ | _none_ | _none_ |

## Active Tasks

| ID | Title | Status | Parent / Reference Project | Related Design | Updated |
| --- | --- | --- | --- | --- | --- |
| T0010 | codex-rag-utilization-hardening | active | `P0001` | `local-rag-system-development-direction`, `source-registry-and-project-ssot`, `msa-runtime-and-storage` | 2026-05-25 |

## Blocked Tasks

| ID | Title | Status | Parent / Reference Project | Blocked By | Updated |
| --- | --- | --- | --- | --- | --- |
| _none_ | _none_ | _none_ | _none_ | _none_ | _none_ |

## Done Tasks

| ID | Title | Status | Parent / Reference Project | Evidence | Updated |
| --- | --- | --- | --- | --- | --- |
| P0001 | local-rag-system | done | self | Functional baseline plus portable host-local Ollama RAG smoke | 2026-05-25 |
| T0001 | source-registry-project-ssot-registration | done | `P0001` | Registry validation and scope resolution smoke | 2026-05-24 |
| T0002 | msa-runtime-baseline | done | `P0001` | Compose config, DDL smoke, service/storage contracts | 2026-05-24 |
| T0003 | spring-boot-msa-skeleton | done | `P0001` | Maven tests, Compose build/up, gateway health | 2026-05-24 |
| T0004 | local-rag-functional-baseline | done | `P0001` | Watch/index/search/MCP REST bridge smoke | 2026-05-24 |
| T0005 | multi-source-application-hardening | done | `P0001` | Multi-source mount and scanner glob enforcement | 2026-05-24 |
| T0006 | local-device-application-baseline | done | `P0001` | Local embedding baseline and full indexing/search smoke | 2026-05-24 |
| T0007 | codex-global-rag-integration | done | `P0001` | Global MCP adapter, skill installer, and `rag_answer` refresh | 2026-05-25 |
| T0008 | portable-ops-zone-deployment | done | `P0001` | Portable defaults and operation-zone deployment | 2026-05-24 |
| T0009 | host-local-ollama-rag-configuration | done | `P0001` | Portable source-slot runtime indexing/search/answer smoke | 2026-05-25 |

## Active Reports

| Title | Status | Report Type | Source | Source Refs | Updated |
| --- | --- | --- | --- | --- | --- |
| local-rag-functional-smoke | done | smoke | `docs/reports/2026-05-24-local-rag-functional-smoke.md` | Compose, registry, sample data | 2026-05-24 |

## Change Log

- 2026-05-24: `P0001-local-rag-system.md` added as active umbrella project.
- 2026-05-24: `T0001-source-registry-project-ssot-registration.md` issued for source registry implementation.
- 2026-05-24: `T0002-msa-runtime-baseline.md` issued for runtime/compose/storage contract.
- 2026-05-24: SDLC automation alignment set `T0003` active, `T0001` blocked, and `T0002` done.
- 2026-05-24: Functional baseline completed directly after SDLC core automation failure. `P0001`, `T0001`, `T0003`, and `T0004` marked done with smoke evidence.
- 2026-05-25: `T0009` issued and blocked after host-local Ollama config and answer endpoint implementation; runtime smoke waits on Docker image pull.
- 2026-05-25: `T0009` registry target updated through machine-local config while committed Compose stayed on generic source slots.
- 2026-05-25: `T0007` global Codex integration refreshed for registry-driven defaults and `rag_answer`.
- 2026-05-25: `T0009` blocker resolved; missing images pulled, Compose stack started, current registered sources indexed, and search/answer smoke passed.
- 2026-05-25: `T0009` answer generation stabilized by disabling Ollama thinking output in the bounded chat request.
- 2026-05-25: Device-specific source names, host paths, and preferred default project selection were kept in ignored local config or the installed global skill, not in committed repo defaults.
- 2026-05-25: `T0010` issued for Codex RAG utilization hardening after active-session MCP tool visibility and API/tool contract gaps were identified.
- 2026-05-25: `T0010` implementation reached development-zone hardening completion: `rag_get_document`, mode normalization, unknown-project 400 handling, `local-rag-system.docs` examples, Codex skill guidance, and smoke tooling are in place; operation-zone deploy/restart remains pending.
