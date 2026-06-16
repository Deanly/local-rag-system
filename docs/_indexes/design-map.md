# Design Map

This is a compact design selection map derived from `docs/design/README.md`.

| Design Doc | Retrieval Class | Domain | Size Tier | Default Load | Section Load |
| --- | --- | --- | --- | --- | --- |
| `docs/design/control-plane.md` | `core-start` | whole-system control | small | true | false |
| `docs/design/ubiquitous-language.md` | `term-excerpt` | all domains | small | false | true |
| `docs/design/local-rag-system-development-direction.md` | `domain-current` | local-rag-system | medium | false | false |
| `docs/design/source-registry-and-project-ssot.md` | `domain-current` | source-registry | medium | false | false |
| `docs/design/msa-runtime-and-storage.md` | `domain-current` | runtime/storage | medium | false | false |
| `docs/design/retrieval-quality-improvement-design.md` | `domain-current` | retrieval-quality | medium | false | false |

## Change Log

- 2026-05-24: Local RAG domain design added to compact design map.
- 2026-05-24: Source registry and project SSOT design added to compact design map.
- 2026-05-24: MSA runtime and storage design added to compact design map.
- 2026-05-25: MSA runtime source mount contract updated to generic source slots.
- 2026-05-25: Retrieval quality improvement design added to compact design map.
- 2026-05-29: Runtime and Local RAG design docs updated for ordered local/LAN Ollama endpoint failover.
- 2026-05-29: Retrieval quality improvement design now feeds P0002/T0013 retrieval governance hardening.
- 2026-05-30: Retrieval quality and runtime/storage docs updated for T0013 metadata-aware chunking and document authority indexing.
- 2026-05-30: Retrieval quality design updated for T0014/T0015 metadata filters, answer context governance, and staleness/citation evaluation checks.
- 2026-05-30: Retrieval quality design updated for T0016 search audit observability expansion.
- 2026-05-31: Retrieval quality design updated for T0017 local reranker deployment decision and P0002 closeout.
- 2026-06-16: Retrieval quality design updated for P0003 ScoreGate adaptive context selection, T0021 offline selector substrate, T0022 offline snapshot evaluator, and T0023 runtime no-ship decision.
