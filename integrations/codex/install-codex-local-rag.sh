#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CODEX_HOME="${CODEX_HOME:-${HOME}/.codex}"
CONFIG_FILE="${CODEX_CONFIG_FILE:-${CODEX_HOME}/config.toml}"
SKILL_DIR="${CODEX_HOME}/skills/local-rag"
SERVER_FILE="${REPO_ROOT}/integrations/codex/local-rag-mcp-server.mjs"
SKILL_FILE="${REPO_ROOT}/integrations/codex/skill/SKILL.md"
BASE_URL="${LOCAL_RAG_BASE_URL:-http://127.0.0.1:42120}"
DEFAULT_PROJECT_ID="${LOCAL_RAG_DEFAULT_PROJECT_ID:-}"

usage() {
  cat <<'USAGE'
Usage:
  integrations/codex/install-codex-local-rag.sh [--uninstall]

Environment:
  CODEX_HOME                 Default: ~/.codex
  CODEX_CONFIG_FILE          Default: $CODEX_HOME/config.toml
  LOCAL_RAG_BASE_URL         Default: http://127.0.0.1:42120
  LOCAL_RAG_DEFAULT_PROJECT_ID optional default project id
  LOCAL_RAG_NODE_BIN         optional absolute node path
USAGE
}

toml_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

remove_config_block() {
  local source_file="$1"
  local target_file="$2"
  if [[ -f "${source_file}" ]]; then
    awk '
      /^\[mcp_servers\.local_rag(\.env)?\]/ { skip = 1; next }
      /^\[/ { skip = 0 }
      skip != 1 { print }
    ' "${source_file}" > "${target_file}"
  else
    : > "${target_file}"
  fi
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi

mkdir -p "${CODEX_HOME}"

TMP_CONFIG="$(mktemp)"
trap 'rm -f "${TMP_CONFIG}"' EXIT

if [[ "${1:-}" == "--uninstall" ]]; then
  remove_config_block "${CONFIG_FILE}" "${TMP_CONFIG}"
  cp "${TMP_CONFIG}" "${CONFIG_FILE}"
  rm -rf "${SKILL_DIR}"
  echo "Removed local_rag MCP server and local-rag skill from ${CODEX_HOME}"
  exit 0
fi

if [[ ! -f "${SERVER_FILE}" ]]; then
  echo "Missing MCP server file: ${SERVER_FILE}" >&2
  exit 1
fi

if [[ ! -f "${SKILL_FILE}" ]]; then
  echo "Missing skill file: ${SKILL_FILE}" >&2
  exit 1
fi

NODE_BIN="${LOCAL_RAG_NODE_BIN:-$(command -v node || true)}"
if [[ -z "${NODE_BIN}" ]]; then
  echo "node is required. Set LOCAL_RAG_NODE_BIN to an absolute node path." >&2
  exit 1
fi

chmod +x "${SERVER_FILE}"
mkdir -p "${SKILL_DIR}"
cp "${SKILL_FILE}" "${SKILL_DIR}/SKILL.md"

if [[ -f "${CONFIG_FILE}" ]]; then
  cp "${CONFIG_FILE}" "${CONFIG_FILE}.bak.$(date +%Y%m%d%H%M%S)"
fi

remove_config_block "${CONFIG_FILE}" "${TMP_CONFIG}"

{
  printf '\n[mcp_servers.local_rag]\n'
  printf 'command = "%s"\n' "$(toml_escape "${NODE_BIN}")"
  printf 'args = ["%s"]\n' "$(toml_escape "${SERVER_FILE}")"
  printf 'startup_timeout_sec = 30\n'
  printf '\n[mcp_servers.local_rag.env]\n'
  printf 'LOCAL_RAG_BASE_URL = "%s"\n' "$(toml_escape "${BASE_URL}")"
  printf 'LOCAL_RAG_DEFAULT_PROJECT_ID = "%s"\n' "$(toml_escape "${DEFAULT_PROJECT_ID}")"
} >> "${TMP_CONFIG}"

cp "${TMP_CONFIG}" "${CONFIG_FILE}"

echo "Installed local_rag MCP server in ${CONFIG_FILE}"
echo "Installed local-rag skill in ${SKILL_DIR}"
echo "Base URL: ${BASE_URL}"
echo "Restart Codex to load the new global MCP server and skill."
