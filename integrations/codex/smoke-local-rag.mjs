#!/usr/bin/env node

import { spawn } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const args = new Set(process.argv.slice(2));
const baseUrl = (process.env.LOCAL_RAG_BASE_URL || "http://127.0.0.1:42120").replace(/\/$/, "");
const query = process.env.LOCAL_RAG_SMOKE_QUERY || "source registry";
const requestedProjectId = process.env.LOCAL_RAG_SMOKE_PROJECT_ID || "";
const smokeDocumentSourceId = process.env.LOCAL_RAG_SMOKE_DOC_SOURCE_ID || "";
const smokeDocumentRelativePath = process.env.LOCAL_RAG_SMOKE_DOC_RELATIVE_PATH || "";
const allowEmptySearch = args.has("--allow-empty-search") || process.env.LOCAL_RAG_SMOKE_ALLOW_EMPTY_SEARCH === "true";
const forceScan = args.has("--force-scan") || process.env.LOCAL_RAG_SMOKE_FORCE_SCAN === "true";
const answerSmoke = args.has("--answer") || process.env.LOCAL_RAG_SMOKE_ANSWER === "true";
const allowAnswerUnavailable = args.has("--allow-answer-unavailable")
  || process.env.LOCAL_RAG_SMOKE_ALLOW_ANSWER_UNAVAILABLE === "true";

const adapterPath = join(dirname(fileURLToPath(import.meta.url)), "local-rag-mcp-server.mjs");
const requiredTools = [
  "rag_search",
  "rag_answer",
  "rag_get_document",
  "rag_list_projects",
  "rag_list_sources",
  "rag_index_status",
  "rag_force_scan"
];

async function main() {
  if (!args.has("--rest-only")) {
    await adapterFramingSmoke(!args.has("--adapter-only") && !args.has("--skip-adapter-call"));
  }
  if (!args.has("--adapter-only")) {
    await restSmoke();
  }
}

async function adapterFramingSmoke(includeToolCall) {
  const messages = [
    {
      jsonrpc: "2.0",
      id: 1,
      method: "initialize",
      params: {
        protocolVersion: "2024-11-05",
        capabilities: {},
        clientInfo: { name: "local-rag-smoke", version: "0.1.0" }
      }
    },
    { jsonrpc: "2.0", id: 2, method: "tools/list", params: {} }
  ];
  if (includeToolCall) {
    messages.push({
      jsonrpc: "2.0",
      id: 3,
      method: "tools/call",
      params: { name: "rag_index_status", arguments: {} }
    });
  }

  const responses = await exchangeMcpMessages(messages);

  assert(responses.some((message) => message.id === 1 && message.result?.serverInfo?.name === "local-rag"), "initialize response missing");
  const tools = responses.find((message) => message.id === 2)?.result?.tools || [];
  const toolNames = new Set(tools.map((tool) => tool.name));
  for (const toolName of requiredTools) {
    assert(toolNames.has(toolName), `tools/list is missing ${toolName}`);
  }
  if (includeToolCall) {
    const call = responses.find((message) => message.id === 3);
    assert(call?.result && call.result.isError === false, "tools/call rag_index_status failed");
  }
  console.log(`[ok] MCP adapter framing smoke passed (${tools.length} tools)`);
}

async function restSmoke() {
  await fetchJson("GET", "/api/health");
  console.log("[ok] gateway health responded");

  const projects = await fetchJson("GET", "/api/mcp/rag_list_projects");
  const sources = await fetchJson("GET", "/api/mcp/rag_list_sources");
  assert(Array.isArray(projects), "rag_list_projects did not return an array");
  assert(Array.isArray(sources), "rag_list_sources did not return an array");
  console.log(`[ok] registry listed ${projects.length} projects and ${sources.length} sources`);

  const projectId = chooseProjectId(projects);
  if (forceScan) {
    const suffix = projectId ? `?projectId=${encodeURIComponent(projectId)}` : "";
    await fetchJson("POST", `/api/mcp/rag_force_scan${suffix}`);
    console.log(`[ok] force scan requested${projectId ? ` for ${projectId}` : ""}`);
  }

  await fetchJson("GET", "/api/mcp/rag_index_status");
  console.log("[ok] index status responded");

  const search = await fetchJson("POST", "/api/mcp/rag_search", {
    projectId: projectId || undefined,
    query,
    limit: 3,
    mode: "hybrid"
  });
  assert(Array.isArray(search.results), "rag_search did not return results array");
  if (!allowEmptySearch) {
    assert(search.results.length > 0, "rag_search returned no results");
  }
  console.log(`[ok] rag_search returned ${search.results.length} results`);

  if (answerSmoke) {
    try {
      const answer = await fetchJson("POST", "/api/mcp/rag_answer", {
        projectId: projectId || undefined,
        query,
        limit: 3,
        mode: "hybrid"
      });
      assert(typeof answer.answer === "string" && answer.answer.length > 0, "rag_answer returned empty answer");
      console.log(`[ok] rag_answer returned answer from ${answer.citations?.length || 0} citations`);
    } catch (error) {
      if (!allowAnswerUnavailable || !String(error.message).includes("503")) {
        throw error;
      }
      console.log("[skip] rag_answer synthesis is unavailable in this local profile");
    }
  }

  const documentRequest = documentRequestFromSearch(search);
  if (documentRequest) {
    const document = await fetchJson("POST", "/api/mcp/rag_get_document", {
      ...documentRequest,
      maxBytes: 2000
    });
    assert(typeof document.content === "string" && document.content.length > 0, "rag_get_document returned empty content");
    assert(document.sourceId === documentRequest.sourceId, "rag_get_document sourceId mismatch");
    console.log(`[ok] rag_get_document read ${document.bytesRead} bytes from ${document.citation}`);
  } else {
    console.log("[skip] rag_get_document smoke requires search results or LOCAL_RAG_SMOKE_DOC_* env vars");
  }

  await expectHttpStatus("POST", "/api/mcp/rag_search", 400, {
    projectId: "__local_rag_unknown_project__",
    query: "smoke",
    limit: 1,
    mode: "keyword"
  });
  console.log("[ok] unknown project search failed with 400");
}

function chooseProjectId(projects) {
  if (requestedProjectId) {
    return requestedProjectId;
  }
  return projects.find((project) => project.projectId === "local-rag-system")?.projectId
    || projects.find((project) => project.active)?.projectId
    || projects[0]?.projectId
    || "";
}

function documentRequestFromSearch(search) {
  if (smokeDocumentSourceId && smokeDocumentRelativePath) {
    return {
      sourceId: smokeDocumentSourceId,
      relativePath: smokeDocumentRelativePath
    };
  }
  const firstResult = search.results?.find((result) => result.sourceId && result.relativePath);
  if (!firstResult) {
    return null;
  }
  return {
    sourceId: firstResult.sourceId,
    relativePath: firstResult.relativePath
  };
}

async function fetchJson(method, path, body) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: body === undefined ? undefined : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`${method} ${path} returned ${response.status}: ${text}`);
  }
  return text ? JSON.parse(text) : null;
}

async function expectHttpStatus(method, path, status, body) {
  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: body === undefined ? undefined : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await response.text();
  if (response.status !== status) {
    throw new Error(`${method} ${path} expected ${status} but returned ${response.status}: ${text}`);
  }
}

function exchangeMcpMessages(messages) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [adapterPath], {
      env: process.env,
      stdio: ["pipe", "pipe", "pipe"]
    });
    let stdout = Buffer.alloc(0);
    let stderr = "";

    child.stdout.on("data", (chunk) => {
      stdout = Buffer.concat([stdout, chunk]);
    });
    child.stderr.on("data", (chunk) => {
      stderr += chunk.toString("utf8");
    });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code !== 0) {
        reject(new Error(`adapter exited with ${code}: ${stderr}`));
        return;
      }
      try {
        resolve(parseMcpFrames(stdout));
      } catch (error) {
        reject(error);
      }
    });

    for (const message of messages) {
      child.stdin.write(encodeMcpFrame(message));
    }
    child.stdin.end();
  });
}

function encodeMcpFrame(message) {
  const json = JSON.stringify(message);
  return `Content-Length: ${Buffer.byteLength(json, "utf8")}\r\n\r\n${json}`;
}

function parseMcpFrames(buffer) {
  const messages = [];
  let remaining = buffer;
  while (remaining.length > 0) {
    const headerEnd = remaining.indexOf("\r\n\r\n");
    assert(headerEnd >= 0, "MCP response is missing header delimiter");
    const header = remaining.subarray(0, headerEnd).toString("utf8");
    const match = header.match(/content-length:\s*(\d+)/i);
    assert(match, `MCP response is missing Content-Length: ${header}`);
    const length = Number.parseInt(match[1], 10);
    const bodyStart = headerEnd + 4;
    const bodyEnd = bodyStart + length;
    assert(remaining.length >= bodyEnd, "MCP response body is truncated");
    messages.push(JSON.parse(remaining.subarray(bodyStart, bodyEnd).toString("utf8")));
    remaining = remaining.subarray(bodyEnd);
  }
  return messages;
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

main().catch((error) => {
  console.error(`[fail] ${error.message}`);
  process.exit(1);
});
