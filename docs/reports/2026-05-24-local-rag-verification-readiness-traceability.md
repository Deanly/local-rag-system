---
type: report
title: local-rag-verification-readiness-traceability
status: draft
owner:
created: 2026-05-24
updated: 2026-05-24
current_focus: "Requirement traceability for basic verification run; no new QA executed"
report_type: verification-readiness
related_project: docs/projects/P0001-local-rag-system.md
related_task:
  - docs/tasks/T0004-local-rag-functional-baseline.md
related_design:
  - docs/design/local-rag-system-development-direction.md
  - docs/design/msa-runtime-and-storage.md
  - docs/design/source-registry-and-project-ssot.md
source_refs:
  - docker-compose.yml
  - config/source-registry.example.yaml
  - services/api-gateway/src/main/java/com/localrag/gateway/GatewayController.java
  - docs/reports/2026-05-24-local-rag-functional-smoke.md
quality_axes:
  - WHOLE
  - CONTRACT
  - EVIDENCE
  - SECURITY
tags:
  - docs/report
  - local-rag-system
  - verification-readiness
---

# local-rag-verification-readiness-traceability

- Type: report
- Status: draft
- Created: 2026-05-24
- Updated: 2026-05-24
- Current Focus: Requirement traceability for basic verification run; no new QA executed
- Report Type: verification-readiness
- Related Project: `docs/projects/P0001-local-rag-system.md`
- Related Task: `docs/tasks/T0004-local-rag-functional-baseline.md`

## Summary

This report prepares the next verification pass for the current `local-rag-system` functional baseline. It does not contain fresh QA evidence: no validators, Maven tests, Compose config checks, Docker runtime, curl smoke, indexing, search, or mutation checks were executed while writing this report.

The selected verification scope is basic audit, report only, with runtime smoke included in the next verification step. The matrix below connects the requirements baseline, acceptance criteria, and QA commands so that pass/fail evidence and defects can be recorded consistently.

## Inputs

- `docs/projects/P0001-local-rag-system.md`
- `docs/tasks/T0004-local-rag-functional-baseline.md`
- Provided requirements baseline and acceptance criteria
- Provided QA plan
- Structural read-only inspection of repository file paths and selected source/config files

## Current QA Evidence Status

- Fresh QA evidence for this delegated turn: none
- Reason: the requested work explicitly excluded new verification execution
- Next step: execute the QA plan and attach command outputs, API responses, and defect records

## QA Plan To Execute Next

### Document Checks

```bash
./docs/bin/validate-codex-readiness.sh
./docs/bin/validate-harness-foundation.sh
./docs/bin/validate-doc-retrieval.sh
./docs/bin/validate-closeout.sh --all
```

### Build And Compose Checks

```bash
docker compose --env-file .env.example config
docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -q test
```

### Runtime Smoke Checks

```bash
curl -fsS http://127.0.0.1:42120/api/health | jq .
curl -fsS -X POST http://127.0.0.1:42120/api/registry/validate | jq .
curl -fsS -X POST 'http://127.0.0.1:42120/api/index/force?projectId=personal-notes' | jq .
curl -fsS http://127.0.0.1:42120/api/index/status | jq .
curl -fsS -X POST http://127.0.0.1:42120/api/search \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"personal-notes","query":"Weaviate hybrid retrieval local Ollama embeddings","limit":5,"mode":"hybrid"}' | jq .
curl -fsS -X POST http://127.0.0.1:42120/api/mcp/rag_search \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"personal-notes","query":"source registry and hybrid retrieval","limit":3,"mode":"hybrid"}' | jq .
```

## Requirement Traceability Matrix

| ID | Requirement / Acceptance Coverage | Verification Command | Expected Evidence | Defect Record On Failure |
| --- | --- | --- | --- | --- |
| RTM-01 | RB1: current workspace `local-rag-system` functional baseline. AC1: docs current truth and code structure do not contradict each other. | `rg --files`; `sed -n '1,260p' docs/projects/P0001-local-rag-system.md`; `sed -n '1,320p' docs/tasks/T0004-local-rag-functional-baseline.md` | Repository contains the Spring Boot MSA modules, `docker-compose.yml`, registry config, DB DDL, sample source, and docs referenced by P0001/T0004. No required file path in the docs is missing. | Severity: High if a claimed required module/config is absent; Evidence file: missing path plus P0001/T0004 section; Repro command: exact `rg --files` or `test -e` command; Recommended action: update implementation or correct stale document truth. |
| RTM-02 | RB2: use P0001 and T0004 completion criteria as verification baseline. AC1 and AC8. | `sed -n '1,260p' docs/projects/P0001-local-rag-system.md`; `sed -n '1,320p' docs/tasks/T0004-local-rag-functional-baseline.md` | P0001 exit criteria and T0004 completion criteria are explicitly mapped to the verification report; unverified criteria remain open rather than silently closed. | Severity: Medium if criteria are unmapped; Evidence file: this traceability report and source doc section; Repro command: source doc read command; Recommended action: add/update RTM entry before verification. |
| RTM-03 | RB3: Spring Boot MSA, Docker Compose, PostgreSQL, Weaviate, local Ollama endpoint, read-only source root. AC2 and AC7. | `docker compose --env-file .env.example config`; review generated service definitions for `postgres`, `weaviate`, `source-registry-service`, `indexer-service`, `retrieval-service`, `mcp-bridge`, `api-gateway` | Compose config renders successfully; public host ports are localhost-bound; source and config mounts are `:ro`; Ollama URL is local/network-local; Weaviate vectorizer is `none`. | Severity: Critical for non-local public bind or writable source mount; Evidence file: `docker-compose.yml` and rendered config excerpt; Repro command: compose config command; Recommended action: restore localhost bind, read-only mounts, and local embedding endpoint. |
| RTM-04 | RB3 runtime implementation and AC3 Maven test criterion. | `docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -q test` | Maven test exits 0; failing test output is absent; if Docker Maven cannot run, failure reason is captured with environment details. | Severity: High for test failures; Evidence file: Maven output; Repro command: exact Docker Maven command; Recommended action: fix failing module/test or document environmental blocker. |
| RTM-05 | RB4: only registered source roots are indexing/search targets. AC4 registry validation. | `curl -fsS -X POST http://127.0.0.1:42120/api/registry/validate | jq .`; `curl -fsS http://127.0.0.1:42120/api/registry/sources | jq .` | Registry response is valid and lists only the configured project/source; returned source metadata matches `config/source-registry.example.yaml` including private sensitivity and read-only policy. | Severity: Critical if unregistered paths are accepted; Evidence file: registry response and YAML; Repro command: registry curl command; Recommended action: enforce registry scope resolution and reject unknown source roots. |
| RTM-06 | RB4 and AC5: created/modified/deleted Markdown is reflected in index status and search results. | Create temp Markdown under the registered sample source, force scan or wait for watcher, search for unique marker, modify token, delete file, then run `/api/index/status` and `/api/search` after each step. | Created document appears with citation; modified snippet/search marker refreshes; deleted document moves to removed/stale state and no longer appears in search results. | Severity: High if create/modify/delete state is stale; Evidence file: temp file path, status responses, search responses; Repro command: exact shell and curl sequence; Recommended action: fix watcher/scanner diff, delete propagation, or Weaviate cleanup. |
| RTM-07 | RB5 and AC6: `/api/search` returns citation-bearing hybrid results with snippet, score, source metadata. | `curl -fsS -X POST http://127.0.0.1:42120/api/search -H 'Content-Type: application/json' -d '{"projectId":"personal-notes","query":"Weaviate hybrid retrieval local Ollama embeddings","limit":5,"mode":"hybrid"}'` | Response contains non-empty results with citation, snippet, score, source id/path/project metadata, and mode-compatible hybrid search behavior. | Severity: High if successful response lacks citation/snippet/score/source metadata; Evidence file: API JSON response; Repro command: exact curl; Recommended action: repair retrieval DTO mapping or Weaviate result enrichment. |
| RTM-08 | RB5 and AC6: `/api/mcp/rag_search` bridge returns same result contract. | `curl -fsS -X POST http://127.0.0.1:42120/api/mcp/rag_search -H 'Content-Type: application/json' -d '{"projectId":"personal-notes","query":"source registry and hybrid retrieval","limit":3,"mode":"hybrid"}'` | MCP REST bridge response includes citation, snippet, score, and source metadata through the gateway path. | Severity: High if bridge drops fields or bypasses project scope; Evidence file: bridge response and gateway route; Repro command: exact curl; Recommended action: align bridge DTO/forwarding with retrieval service contract. |
| RTM-09 | RB5 and AC4: `/api/index/status` and force scan API are available. | `curl -fsS -X POST 'http://127.0.0.1:42120/api/index/force?projectId=personal-notes' | jq .`; `curl -fsS http://127.0.0.1:42120/api/index/status | jq .` | Force scan returns detected/indexed/chunk counts; index status reports document and chunk totals/status buckets consistent with sample source state. | Severity: High if scan fails or status contradicts search; Evidence file: scan/status JSON; Repro command: exact curl commands; Recommended action: fix gateway routing, indexer scan path, or database state accounting. |
| RTM-10 | RB6 and AC7: private source content must not be sent to hosted external APIs. | `docker compose --env-file .env.example config`; inspect environment for embedding/chat endpoints; review runtime logs during smoke for outbound hosted API references if logs are available. | Embedding/chat base URL points only to a local or LAN Ollama endpoint; no hosted external API key or URL is required for indexing/search; source mounts are read-only. | Severity: Critical if private content is sent to hosted APIs; Evidence file: rendered compose, env vars, logs; Repro command: compose config plus log command; Recommended action: remove hosted provider path, add explicit local-only guard, and fail closed when endpoint is not local. |
| RTM-11 | AC2: document validators and Compose config pass or fail with clear reasons. | Run document checks and `docker compose --env-file .env.example config`. | Each command has pass/fail status, captured stdout/stderr, timestamp, and environment note. Failures are recorded as defects, not omitted. | Severity: Medium for validator failure, High for compose config failure; Evidence file: command transcript; Repro command: exact validator/compose command; Recommended action: fix doc metadata/index/config or document reproducible blocker. |
| RTM-12 | AC8: final verification report includes severity, evidence file, reproduction command, and recommended action. | Review final audit report before closeout. | Every defect row includes id, severity, affected requirement, evidence file, reproduction command, expected, actual, recommended action, and status. | Severity: Medium if report is incomplete; Evidence file: final verification report; Repro command: report review checklist; Recommended action: complete defect metadata before marking verification complete. |

## Defect Record Format

```yaml
id: DEF-YYYYMMDD-NNN
severity: Critical|High|Medium|Low
status: open|resolved|accepted|blocked
affectedRequirement: RBn|ACn|RTM-nn
title: short failure summary
evidenceFile: path/to/file-or-command-transcript
reproductionCommand: exact command or curl sequence
expected: expected behavior or response fields
actual: observed failure output or response fields
recommendedAction: concrete fix or operator prerequisite
notes: optional environment notes
```

## Readiness Conclusion

The verification design is ready to execute without additional user decisions. The project must remain active until the QA plan is run and fresh evidence confirms or defects record each acceptance criterion.

## Status

- 2026-05-24: report created to connect P0001, T0004, requirements baseline, acceptance criteria, and QA plan. No new QA commands were run.
