#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FIXTURE="${LOCAL_RAG_SCOREGATE_FIXTURE:-${ROOT_DIR}/docs/evaluation/scoregate-offline-cases.json}"
OUTPUT_FILE="${LOCAL_RAG_SCOREGATE_OUTPUT:-}"

if [[ ! -f "${FIXTURE}" ]]; then
  echo "[error] ScoreGate offline fixture not found: ${FIXTURE}" >&2
  exit 1
fi

LOCAL_RAG_SCOREGATE_FIXTURE="${FIXTURE}" \
LOCAL_RAG_SCOREGATE_OUTPUT="${OUTPUT_FILE}" \
mvn -q -pl services/retrieval-service -am \
  -Dtest=ScoreGateOfflineEvaluationTests \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
