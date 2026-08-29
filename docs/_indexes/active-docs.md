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
| T0025 | ollama-qwen38-chat-cutover | active | `P0001` | `local-rag-system-development-direction`, `msa-runtime-and-storage` | 2026-08-29 |
| T0024 | local-cross-encoder-sidecar-proof | active | `P0003` | `retrieval-quality-improvement-design` | 2026-06-16 |
| T0010 | codex-rag-utilization-hardening | active | `P0001` | `local-rag-system-development-direction`, `source-registry-and-project-ssot`, `msa-runtime-and-storage` | 2026-05-29 |

## Draft Tasks

| ID | Title | Status | Parent / Reference Project | Related Design | Updated |
| --- | --- | --- | --- | --- | --- |
| T0018 | source-root-symlink-boundary-hardening | draft | `P0001` | `local-rag-system-development-direction`, `source-registry-and-project-ssot`, `msa-runtime-and-storage` | 2026-06-16 |
| T0019 | indexer-failure-job-state-persistence | draft | `P0001` | `local-rag-system-development-direction`, `msa-runtime-and-storage` | 2026-06-16 |

## Blocked Tasks

| ID | Title | Status | Parent / Reference Project | Blocked By | Updated |
| --- | --- | --- | --- | --- | --- |
| _none_ | _none_ | _none_ | _none_ | _none_ | _none_ |

## Done Tasks

| ID | Title | Status | Parent / Reference Project | Evidence | Updated |
| --- | --- | --- | --- | --- | --- |
| P0001 | local-rag-system | done | self | Functional baseline plus portable host-local/LAN Ollama endpoint failover | 2026-05-29 |
| P0002 | retrieval-governance-hardening | done | `P0001` | Metadata-aware retrieval governance, source-priority answer context, staleness/citation evaluation, search audit observability, and local reranker deployment decision | 2026-05-31 |
| P0003 | scoregate-adaptive-context-selection | done | `P0001` | ScoreGate selector and offline evaluator completed; runtime rollout closed as no-ship because no true local cross-encoder `r_i` source exists in the current profile | 2026-06-16 |
| T0001 | source-registry-project-ssot-registration | done | `P0001` | Registry validation and scope resolution smoke | 2026-05-24 |
| T0002 | msa-runtime-baseline | done | `P0001` | Compose config, DDL smoke, service/storage contracts | 2026-05-24 |
| T0003 | spring-boot-msa-skeleton | done | `P0001` | Maven tests, Compose build/up, gateway health | 2026-05-24 |
| T0004 | local-rag-functional-baseline | done | `P0001` | Watch/index/search/MCP REST bridge smoke | 2026-05-24 |
| T0005 | multi-source-application-hardening | done | `P0001` | Multi-source mount and scanner glob enforcement | 2026-05-24 |
| T0006 | local-device-application-baseline | done | `P0001` | Local embedding baseline and full indexing/search smoke | 2026-05-24 |
| T0007 | codex-global-rag-integration | done | `P0001` | Global MCP adapter, skill installer, and `rag_answer` refresh | 2026-05-25 |
| T0008 | portable-ops-zone-deployment | done | `P0001` | Portable defaults and operation-zone deployment | 2026-05-24 |
| T0009 | host-local-ollama-rag-configuration | done | `P0001` | Portable source-slot runtime indexing/search/answer smoke | 2026-05-25 |
| T0011 | retrieval-quality-hardening | done | `P0001` | Evaluation harness, deterministic rerank, primary-source weighting, and diversity control | 2026-05-25 |
| T0012 | portable-ollama-endpoint-failover | done | `P0001` | Ordered local/LAN Ollama endpoint failover for notebook-local and Mac mini profiles | 2026-05-29 |
| T0013 | retrieval-chunking-and-document-authority-hardening | done | `P0002` | Metadata-aware chunking, document authority indexing, additive schema migration, force-scan reindex, and search result metadata exposure | 2026-05-30 |
| T0014 | search-filter-and-answer-context-governance | done | `P0002` | Metadata filter enforcement, stale-source demotion, historical opt-in, and answer source priority cues | 2026-05-30 |
| T0015 | answer-quality-and-staleness-evaluation | done | `P0002` | Evaluation runner source-use, citation usefulness, staleness error metrics, unknown-project skip, and Korean task-id regression coverage | 2026-05-30 |
| T0016 | retrieval-audit-observability-expansion | done | `P0002` | Search audit candidate counts, phase latency, top result, source distribution, and score JSON for retrieval debugging | 2026-05-30 |
| T0017 | local-reranker-evaluation | done | `P0002` | Local reranker deployment decision; separate model reranker deferred from P0002 release path | 2026-05-31 |
| T0020 | service-artifact-version-doc-drift | done | `P0001` | Service README artifact convention aligned with `1.2.0` Maven/Docker artifact naming | 2026-06-16 |
| T0021 | scoregate-offline-selector-experiment | done | `P0003` | Pure ScoreGate selector, B1-B4/fusion/MAX-K decision tests, and no default runtime behavior change | 2026-06-16 |
| T0022 | scoregate-offline-evaluation-fixture | done | `P0003` | Offline ScoreGate snapshot fixture, selector-backed validator, and snapshot evaluation report | 2026-06-16 |
| T0023 | local-cross-encoder-score-source | done | `P0003` | Local cross-encoder score-source decision; runtime ScoreGate no-ship for current profile | 2026-06-16 |

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
- 2026-05-25: Retrieval quality baseline, improvement design, and `T0011` active task were added for evaluation-backed ranking hardening.
- 2026-05-25: `T0011` completed in the development zone with versioned evaluation cases, runner, deterministic source/path rerank, document diversity control, and before/after metrics.
- 2026-05-29: `T0012` completed ordered local/LAN Ollama endpoint failover so a notebook-local endpoint and Mac mini endpoint can be configured without committing private hostnames.
- 2026-05-29: `T0010` operation deployment resumed by user request, with `~/Services/local-rag-system` as the operation-zone root.
- 2026-05-29: `T0010` operation-zone deployment completed under `~/Services/local-rag-system`; Docker stack, host Ollama, `local-rag-system` force scan, search, answer, and `local-rag codex-smoke` passed.
- 2026-05-29: `T0010` operation registry initial source slots expanded using generic `/sources/source-06..08`; live source searches and Codex smoke passed with machine-local identities kept out of committed docs.
- 2026-05-29: `P0002` issued as the retrieval governance hardening exception branch after P0001 functional baseline; `T0013` is the first active P0002 task.
- 2026-05-30: P0002/T0013 planning reviewed and supplemented with priority gates, metadata contract draft, and migration plan.
- 2026-05-30: `T0013` completed P0002 P0 metadata-aware chunking and document authority indexing. P1 search filter, answer context, and staleness evaluation work continued under P0002.
- 2026-05-30: `T0014` issued as P0002 P1 implementation slice for metadata filter enforcement, stale-source demotion, historical opt-in, and answer source priority cues.
- 2026-05-30: `T0014` completed. Search filters now use T0013 metadata, stale/low-authority evidence is demoted by default, historical opt-in is supported, and answer context carries source priority cues.
- 2026-05-30: `T0015` completed. Retrieval evaluation now reports must-use, must-not-use, citation usefulness, and staleness error checks, skips unregistered fixture projects, and covers Korean task-id suffix retrieval.
- 2026-05-30: `T0016` completed. Search audit now stores candidate limits, raw/final counts, phase latency, top result source/path, source distribution, and score JSON.
- 2026-05-31: `T0017` and `P0002` completed. Retrieval governance hardening is deployable with deterministic governance ranking and no separate local model reranker in the release path.
- 2026-06-16: Project review findings were issued as draft remediation tasks: `T0018` source-root symlink boundary hardening, `T0019` indexer failure/job persistence, and `T0020` service artifact version documentation drift.
- 2026-06-16: `P0003` issued for ScoreGate adaptive context selection; `T0021` completed the pure offline selector substrate without changing runtime defaults.
- 2026-06-16: `T0022` completed ScoreGate offline snapshot fixture and validator; P0003 remains active pending local cross-encoder score source and rollout decision.
- 2026-06-16: `T0023` closed P0003 as runtime no-ship for the current profile because no true local cross-encoder `r_i` score source exists.
- 2026-06-16: `T0020` closed during P0003 release prep by aligning service artifact documentation with `1.2.0` Maven/Docker artifact naming.
- 2026-06-16: `T0024` issued as the P0003 follow-up proof task for a local-only cross-encoder score source and ScoreGate opt-in path.
- 2026-06-16: `T0024` implementation substrate added optional reranker sidecar, retrieval-service ScoreGate debug/opt-in path, runtime probes, and snapshot collector while keeping default search behavior unchanged.
- 2026-06-16: `T0024` sidecar proof smoke built the CPU reranker image and confirmed direct normalized `r_i` scoring; live `rag_search` runtime snapshots remain pending controlled restart/deploy.
- 2026-08-29: `T0025` issued to align tracked/generated/current Service answer generation with native Ollama `qwen3.8:latest` and close it through a tagged deployment.
