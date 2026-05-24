# AGENTS.md

## Repository Map

- `README.md` is the project entrypoint for the target Java/Spring Boot MSA runtime.
- `docs/README.md` is the document-harness schema and operating guide.
- `docs/design/control-plane.md` is the whole-system control surface for Local RAG.
- `docs/design/local-rag-system-development-direction.md` is the current architecture and development-direction truth.
- `docs/design/source-registry-and-project-ssot.md` is the current source registry, project id, and SSOT registration truth.
- `docs/design/msa-runtime-and-storage.md` is the current MSA, Docker Compose, PostgreSQL, and Weaviate storage truth.
- `docs/design/ubiquitous-language.md` holds canonical Local RAG terms.
- `docs/guide/sdlc-automation.md` defines the SDLC automation goal, critical path, gates, and verification ladder.
- `docs/guide/installing-on-a-new-machine.md` is the repo-only bootstrap path for a new machine and Codex integration.
- `docs/projects/P0001-local-rag-system.md` is the completed umbrella project for the functional baseline.
- `docs/tasks/README.md` is the active task roster. If no task is active, issue a new `T####` task under `P0001` before broad implementation work unless the user explicitly asks for a quick fix.
- `docs/tasks/T0002-msa-runtime-baseline.md` is done and records the runtime/compose/storage contract baseline.
- `docs/tasks/T0003-spring-boot-msa-skeleton.md` is done and records the Spring Boot service skeleton baseline.
- `docs/tasks/T0004-local-rag-functional-baseline.md` is done and records the first end-to-end RAG functional baseline.
- `services/` contains target Spring Boot MSA subprojects.
- `database/` contains PostgreSQL DDL and Weaviate schema contracts.
- `config/` contains machine-local config examples.
- `docs/bin/` contains document validators and document creation helpers.

## Codex Workflow

- Start broad work by reading this file, `docs/design/control-plane.md`, and the directly relevant project/design document.
- Treat source refs in the docs as mandatory inputs when a task depends on original project notes or external evidence.
- Keep changes scoped to the requested surface. Avoid unrelated refactors or style churn.
- Prefer `rg` for navigation and the scripts in `docs/bin/` for document verification.
- For implementation work, follow the target Spring Boot MSA direction. The earlier Python BM25 scaffold was removed from the active runtime surface.
- For source indexing work, read `docs/design/source-registry-and-project-ssot.md` first. Do not index unregistered local folders.
- For runtime, compose, service boundary, or storage work, read `docs/design/msa-runtime-and-storage.md` first.
- For SDLC automation or implementation sessions, read `docs/guide/sdlc-automation.md` and work the single active critical-path task first.
- Do not issue a new `project` document unless the user explicitly asks for it or approves it. Prefer a new task under `P0001`.

## Documentation Rules

- Preserve YAML frontmatter on generated markdown templates.
- Keep frontmatter properties and first-screen bullet metadata in sync.
- Keep `source_refs` populated when a claim depends on raw source material, external docs, transcripts, datasets, or official references.
- `design` documents hold current truth; `task` and `project` status sections hold append-only execution history.
- Update `docs/design/README.md`, `docs/_indexes/design-map.md`, and active folder README files when active design/project/task surfaces change.
- If a reusable answer emerges from a report or conversation, promote it into `guide`, `design`, `project`, or `task` as appropriate.

## Verification Commands

Run these after document-harness or design surface changes:

```bash
./docs/bin/validate-codex-readiness.sh
./docs/bin/validate-harness-foundation.sh
./docs/bin/validate-doc-retrieval.sh
./docs/bin/validate-closeout.sh --all
docker compose --env-file .env.example config
```

For focused code-only changes, run the relevant Spring Boot test command after the build is introduced. For compose/storage changes, run `docker compose --env-file .env.example config`.

## Done Criteria

- The relevant design/project/task documents agree with each other.
- Codex-facing instructions remain concise enough to load automatically.
- Validators pass, or any skipped validator is explicitly explained.
- Code changes include focused tests or a clear reason tests could not be run.
- Final responses name the changed surfaces and verification result.
