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
