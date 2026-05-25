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
  const reciprocal = rows.reduce((sum, row) => sum + (row.rank > 0 ? 1 / row.rank : 0), 0);
  const latency = rows.reduce((sum, row) => sum + row.latencyMs, 0);
  return {
    count,
    hitAt1: count ? hit1 / count : 0,
    hitAt5: count ? hit5 / count : 0,
    mrr: count ? reciprocal / count : 0,
    sourceAccuracyAt1: count ? source1 / count : 0,
    averageLatencyMs: count ? latency / count : 0,
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

async function main() {
  try {
    await requestJson('/api/health');
  } catch (error) {
    console.error(`[error] Local RAG runtime is not reachable at ${baseUrl}/api/health`);
    console.error(`[error] ${error.message}`);
    process.exit(1);
  }

  const groups = parseCasesYaml(fs.readFileSync(casesFile, 'utf8'));
  const rows = [];

  for (const group of groups) {
    for (const testCase of group.cases) {
      for (const mode of testCase.modes) {
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
          console.error(`[error] Search request failed for ${testCase.id}/${mode}: ${error.message}`);
          process.exit(1);
        }
        const latencyMs = Date.now() - started;
        const results = Array.isArray(response.results) ? response.results : [];
        const top = results[0] || {};
        const topScore = top.score || {};
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
          latencyMs
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
  console.log('');
  console.log('group/mode,count,hit@1,hit@5,mrr,source_accuracy@1,avg_latency_ms');
  for (const key of Object.keys(byGroupMode).sort()) {
    const value = metrics(byGroupMode[key]);
    console.log([
      key,
      value.count,
      formatPct(value.hitAt1),
      formatPct(value.hitAt5),
      value.mrr.toFixed(3),
      formatPct(value.sourceAccuracyAt1),
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
    overall.averageLatencyMs.toFixed(1)
  ].join(','));

  if (overall.misses.length) {
    console.log('');
    console.log('misses:');
    for (const miss of overall.misses) {
      console.log(`- ${miss.id}/${miss.mode} expected=${miss.expectedSourceId}:${miss.expectedRelativePath} top=${miss.topResultSourceId}:${miss.topResultRelativePath}`);
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
    rows
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
