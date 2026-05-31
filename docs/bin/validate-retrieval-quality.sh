#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_URL="${LOCAL_RAG_BASE_URL:-http://127.0.0.1:42120}"
SEARCH_PATH="${LOCAL_RAG_SEARCH_PATH:-/api/mcp/rag_search}"
CASES_FILE="${LOCAL_RAG_EVAL_CASES:-${ROOT_DIR}/docs/evaluation/retrieval-quality-cases.yaml}"
OUTPUT_FILE="${LOCAL_RAG_EVAL_OUTPUT:-}"

if ! command -v node >/dev/null 2>&1; then
  echo "[error] node is required for retrieval quality evaluation" >&2
  exit 1
fi

if [[ ! -f "${CASES_FILE}" ]]; then
  echo "[error] retrieval evaluation fixture not found: ${CASES_FILE}" >&2
  exit 1
fi

LOCAL_RAG_BASE_URL="${BASE_URL}" \
LOCAL_RAG_SEARCH_PATH="${SEARCH_PATH}" \
LOCAL_RAG_EVAL_CASES="${CASES_FILE}" \
LOCAL_RAG_EVAL_OUTPUT="${OUTPUT_FILE}" \
node <<'NODE'
const fs = require('fs');

const baseUrl = process.env.LOCAL_RAG_BASE_URL.replace(/\/+$/, '');
const searchPath = process.env.LOCAL_RAG_SEARCH_PATH.startsWith('/')
  ? process.env.LOCAL_RAG_SEARCH_PATH
  : `/${process.env.LOCAL_RAG_SEARCH_PATH}`;
const casesFile = process.env.LOCAL_RAG_EVAL_CASES;
const outputFile = process.env.LOCAL_RAG_EVAL_OUTPUT || '';

function parseScalar(value) {
  const trimmed = value.trim();
  if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
    return trimmed.slice(1, -1);
  }
  if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
    return trimmed.slice(1, -1);
  }
  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    const inner = trimmed.slice(1, -1).trim();
    if (!inner) return [];
    return inner.split(',').map((part) => parseScalar(part.trim()));
  }
  if (/^\d+$/.test(trimmed)) {
    return Number(trimmed);
  }
  if (trimmed === 'true') {
    return true;
  }
  if (trimmed === 'false') {
    return false;
  }
  return trimmed;
}

function parseCasesYaml(text) {
  const groups = [];
  let group = null;
  let current = null;
  let inExpected = false;

  for (const rawLine of text.split(/\r?\n/)) {
    if (!rawLine.trim() || rawLine.trim().startsWith('#')) continue;
    const line = rawLine.replace(/\s+$/, '');
    const indent = line.search(/\S/);
    const trimmed = line.trim();

    if (indent === 2 && trimmed.startsWith('- id: ')) {
      group = { id: parseScalar(trimmed.slice(6)), description: '', cases: [] };
      groups.push(group);
      current = null;
      inExpected = false;
      continue;
    }
    if (group && indent === 4 && trimmed.startsWith('description: ')) {
      group.description = parseScalar(trimmed.slice('description: '.length));
      continue;
    }
    if (group && indent === 6 && trimmed.startsWith('- id: ')) {
      current = { id: parseScalar(trimmed.slice(6)), expected: {} };
      group.cases.push(current);
      inExpected = false;
      continue;
    }
    if (!current) continue;
    if (indent === 8 && trimmed === 'expected:') {
      inExpected = true;
      continue;
    }
    if (indent === 8 && trimmed.includes(': ')) {
      inExpected = false;
      const [key, ...rest] = trimmed.split(': ');
      current[key] = parseScalar(rest.join(': '));
      continue;
    }
    if (indent === 10 && inExpected && trimmed.includes(': ')) {
      const [key, ...rest] = trimmed.split(': ');
      current.expected[key] = parseScalar(rest.join(': '));
    }
  }

  return groups;
}

function rankOf(results, expected) {
  const expectedPath = expected.relativePath;
  const expectedSource = expected.sourceId;
  const index = results.findIndex((item) => (
    item.sourceId === expectedSource && item.relativePath === expectedPath
  ));
  return index < 0 ? 0 : index + 1;
}

function metrics(rows) {
  const count = rows.length;
  const hit1 = rows.filter((row) => row.rank > 0 && row.rank <= 1).length;
  const hit5 = rows.filter((row) => row.rank > 0 && row.rank <= 5).length;
  const source1 = rows.filter((row) => row.topResultSourceId === row.expectedSourceId).length;
  const mustUse = rows.filter((row) => row.mustUsePassed).length;
  const mustNotUse = rows.filter((row) => row.mustNotUsePassed).length;
  const citationUseful = rows.filter((row) => row.citationUsefulnessPassed).length;
  const stalenessErrors = rows.filter((row) => row.stalenessError).length;
  const reciprocal = rows.reduce((sum, row) => sum + (row.rank > 0 ? 1 / row.rank : 0), 0);
  const latency = rows.reduce((sum, row) => sum + row.latencyMs, 0);
  return {
    count,
    hitAt1: count ? hit1 / count : 0,
    hitAt5: count ? hit5 / count : 0,
    mrr: count ? reciprocal / count : 0,
    sourceAccuracyAt1: count ? source1 / count : 0,
    mustUsePassRate: count ? mustUse / count : 0,
    mustNotUsePassRate: count ? mustNotUse / count : 0,
    citationUsefulness: count ? citationUseful / count : 0,
    stalenessErrors,
    averageLatencyMs: count ? latency / count : 0,
    top1Misses: rows.filter((row) => row.rank !== 1).map((row) => ({
      id: row.id,
      groupId: row.groupId,
      mode: row.mode,
      projectId: row.projectId,
      query: row.query,
      expectedSourceId: row.expectedSourceId,
      expectedRelativePath: row.expectedRelativePath,
      rank: row.rank,
      topResultSourceId: row.topResultSourceId,
      topResultRelativePath: row.topResultRelativePath
    })),
    misses: rows.filter((row) => row.rank === 0).map((row) => ({
      id: row.id,
      groupId: row.groupId,
      mode: row.mode,
      projectId: row.projectId,
      query: row.query,
      expectedSourceId: row.expectedSourceId,
      expectedRelativePath: row.expectedRelativePath,
      topResultSourceId: row.topResultSourceId,
      topResultRelativePath: row.topResultRelativePath
    }))
  };
}

function arrayValue(value) {
  if (value == null) return [];
  if (Array.isArray(value)) return value.map(String);
  return [String(value)];
}

function statusOf(result) {
  return String(result?.metadata?.frontmatterStatus || '').toLowerCase();
}

function citationUseful(result) {
  const citation = String(result?.citation || '');
  const relativePath = String(result?.relativePath || '');
  return Boolean(citation && relativePath && citation.includes(relativePath));
}

function qualityChecks(testCase, results) {
  const resultPaths = results.map((item) => item.relativePath).filter(Boolean);
  const resultStatuses = results.map(statusOf).filter(Boolean);
  const mustUseRelativePaths = arrayValue(testCase.mustUseRelativePaths);
  const mustNotUseRelativePaths = arrayValue(testCase.mustNotUseRelativePaths);
  const mustNotUseStatuses = arrayValue(testCase.mustNotUseStatuses).map((value) => value.toLowerCase());
  const requireCitation = testCase.requireCitation === true || testCase.requireCitation === 'true';

  const mustUsePassed = mustUseRelativePaths.every((path) => resultPaths.includes(path));
  const mustNotUsePathsPassed = mustNotUseRelativePaths.every((path) => !resultPaths.includes(path));
  const mustNotUseStatusesPassed = mustNotUseStatuses.every((status) => !resultStatuses.includes(status));
  const citationUsefulnessPassed = !requireCitation || results.every(citationUseful);
  return {
    mustUsePassed,
    mustNotUsePassed: mustNotUsePathsPassed && mustNotUseStatusesPassed,
    citationUsefulnessPassed,
    stalenessError: !mustNotUsePathsPassed || !mustNotUseStatusesPassed,
    mustUseRelativePaths,
    mustNotUseRelativePaths,
    mustNotUseStatuses
  };
}

function formatPct(value) {
  return `${(value * 100).toFixed(1)}%`;
}

async function requestJson(path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, options);
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}: ${text.slice(0, 300)}`);
  }
  return text ? JSON.parse(text) : {};
}

async function registeredProjectIds() {
  try {
    const projects = await requestJson('/api/mcp/rag_list_projects');
    if (Array.isArray(projects)) {
      return new Set(projects
        .filter((project) => project.active !== false)
        .map((project) => project.projectId)
        .filter(Boolean));
    }
  } catch (error) {
    console.error(`[warn] Could not list registered projects before evaluation: ${error.message}`);
  }
  return null;
}

async function main() {
  try {
    await requestJson('/api/health');
  } catch (error) {
    console.error(`[error] Local RAG runtime is not reachable at ${baseUrl}/api/health`);
    console.error(`[error] ${error.message}`);
    process.exit(1);
  }

  const groups = parseCasesYaml(fs.readFileSync(casesFile, 'utf8'));
  const activeProjectIds = await registeredProjectIds();
  const rows = [];
  const skipped = [];

  for (const group of groups) {
    for (const testCase of group.cases) {
      for (const mode of testCase.modes) {
        if (activeProjectIds && testCase.projectId && !activeProjectIds.has(testCase.projectId)) {
          skipped.push({
            id: testCase.id,
            groupId: group.id,
            projectId: testCase.projectId,
            mode,
            reason: 'unknown-project'
          });
          continue;
        }
        const started = Date.now();
        let response;
        try {
          response = await requestJson(searchPath, {
            method: 'POST',
            headers: { 'content-type': 'application/json' },
            body: JSON.stringify({
              projectId: testCase.projectId,
              query: testCase.query,
              mode,
              limit: 5
            })
          });
        } catch (error) {
          if (error.message.includes('Unknown projectId')) {
            skipped.push({
              id: testCase.id,
              groupId: group.id,
              projectId: testCase.projectId,
              mode,
              reason: 'unknown-project'
            });
            continue;
          }
          console.error(`[error] Search request failed for ${testCase.id}/${mode}: ${error.message}`);
          process.exit(1);
        }
        const latencyMs = Date.now() - started;
        const results = Array.isArray(response.results) ? response.results : [];
        const top = results[0] || {};
        const topScore = top.score || {};
        const checks = qualityChecks(testCase, results);
        rows.push({
          id: testCase.id,
          groupId: group.id,
          projectId: testCase.projectId,
          language: testCase.language,
          intent: testCase.intent,
          query: testCase.query,
          mode,
          expectedSourceId: testCase.expected.sourceId,
          expectedRelativePath: testCase.expected.relativePath,
          rank: rankOf(results, testCase.expected),
          resultCount: results.length,
          rawCandidateCount: topScore.rawCandidateCount ?? null,
          finalResultCount: topScore.finalResultCount ?? results.length,
          topResultSourceId: top.sourceId || null,
          topResultRelativePath: top.relativePath || null,
          sourcesSearched: response.sourcesSearched || [],
          latencyMs,
          ...checks
        });
      }
    }
  }

  const overall = metrics(rows);
  const byGroupMode = {};
  for (const row of rows) {
    const key = `${row.groupId}/${row.mode}`;
    byGroupMode[key] ??= [];
    byGroupMode[key].push(row);
  }

  console.log(`[ok] retrieval quality evaluation completed against ${baseUrl}${searchPath}`);
  console.log(`cases=${rows.length} fixture=${casesFile}`);
  if (skipped.length) {
    console.log(`skipped=${skipped.length} reason=unknown-project`);
  }
  console.log('');
  console.log('group/mode,count,hit@1,hit@5,mrr,source_accuracy@1,must_use,must_not_use,citation_usefulness,staleness_errors,avg_latency_ms');
  for (const key of Object.keys(byGroupMode).sort()) {
    const value = metrics(byGroupMode[key]);
    console.log([
      key,
      value.count,
      formatPct(value.hitAt1),
      formatPct(value.hitAt5),
      value.mrr.toFixed(3),
      formatPct(value.sourceAccuracyAt1),
      formatPct(value.mustUsePassRate),
      formatPct(value.mustNotUsePassRate),
      formatPct(value.citationUsefulness),
      value.stalenessErrors,
      value.averageLatencyMs.toFixed(1)
    ].join(','));
  }
  console.log([
    'overall',
    overall.count,
    formatPct(overall.hitAt1),
    formatPct(overall.hitAt5),
    overall.mrr.toFixed(3),
    formatPct(overall.sourceAccuracyAt1),
    formatPct(overall.mustUsePassRate),
    formatPct(overall.mustNotUsePassRate),
    formatPct(overall.citationUsefulness),
    overall.stalenessErrors,
    overall.averageLatencyMs.toFixed(1)
  ].join(','));

  if (overall.misses.length) {
    console.log('');
    console.log('misses:');
    for (const miss of overall.misses) {
      console.log(`- ${miss.id}/${miss.mode} expected=${miss.expectedSourceId}:${miss.expectedRelativePath} top=${miss.topResultSourceId}:${miss.topResultRelativePath}`);
    }
  }

  if (overall.top1Misses.length) {
    console.log('');
    console.log('top1_misses:');
    for (const miss of overall.top1Misses) {
      console.log(`- ${miss.id}/${miss.mode} rank=${miss.rank} expected=${miss.expectedSourceId}:${miss.expectedRelativePath} top=${miss.topResultSourceId}:${miss.topResultRelativePath}`);
    }
  }

  const qualityFailures = rows.filter((row) => (
    !row.mustUsePassed || !row.mustNotUsePassed || !row.citationUsefulnessPassed
  ));
  if (qualityFailures.length) {
    console.log('');
    console.log('quality_failures:');
    for (const failure of qualityFailures) {
      console.log(`- ${failure.id}/${failure.mode} mustUse=${failure.mustUsePassed} mustNotUse=${failure.mustNotUsePassed} citation=${failure.citationUsefulnessPassed}`);
    }
  }

  if (skipped.length) {
    console.log('');
    console.log('skipped:');
    for (const item of skipped) {
      console.log(`- ${item.id}/${item.mode} projectId=${item.projectId} reason=${item.reason}`);
    }
  }

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    searchPath,
    fixture: casesFile,
    summary: {
      overall,
      byGroupMode: Object.fromEntries(Object.entries(byGroupMode).map(([key, value]) => [key, metrics(value)]))
    },
    rows,
    skipped
  };

  if (outputFile) {
    fs.writeFileSync(outputFile, `${JSON.stringify(report, null, 2)}\n`);
    console.log('');
    console.log(`[ok] wrote evaluation report: ${outputFile}`);
  }
}

main().catch((error) => {
  console.error(`[error] ${error.stack || error.message}`);
  process.exit(1);
});
NODE
