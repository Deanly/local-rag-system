#!/usr/bin/env node

const baseUrl = (process.env.LOCAL_RAG_BASE_URL || "http://127.0.0.1:42120").replace(/\/$/, "");
const defaultProjectId = process.env.LOCAL_RAG_DEFAULT_PROJECT_ID || "";

const ragQueryInputSchema = {
  type: "object",
  properties: {
    projectId: {
      type: "string",
      description: "Optional project id from the local source registry. If omitted, the configured default project id is used."
    },
    query: {
      type: "string",
      description: "Natural-language or keyword query to search in the local RAG index."
    },
    limit: {
      type: "integer",
      minimum: 1,
      maximum: 50,
      default: 5
    },
    mode: {
      type: "string",
      enum: ["hybrid", "vector", "keyword"],
      default: "hybrid"
    },
    includeSourceIds: {
      type: "array",
      items: { type: "string" },
      description: "Optional source ids from the local source registry to include."
    },
    excludeSourceIds: {
      type: "array",
      items: { type: "string" },
      description: "Optional source ids to exclude."
    },
    filters: {
      type: "object",
      additionalProperties: {
        type: "array",
        items: { type: "string" }
      }
    }
  },
  required: ["query"],
  additionalProperties: false
};

const ragDocumentInputSchema = {
  type: "object",
  properties: {
    sourceId: {
      type: "string",
      description: "Registered source id that owns the document."
    },
    relativePath: {
      type: "string",
      description: "Path relative to the registered source root. Absolute paths and path traversal are rejected."
    },
    maxBytes: {
      type: "integer",
      minimum: 1000,
      maximum: 200000,
      default: 20000,
      description: "Maximum UTF-8 bytes to return from the document."
    }
  },
  required: ["sourceId", "relativePath"],
  additionalProperties: false
};

const tools = [
  {
    name: "rag_search",
    description: "Search the local RAG index with project-aware hybrid retrieval and citation-bearing results.",
    inputSchema: ragQueryInputSchema
  },
  {
    name: "rag_answer",
    description: "Generate a local-only answer from retrieved snippets when an answer model is explicitly configured. Embedding-only profiles return ANSWER_GENERATION_DISABLED; use rag_search and synthesize in the authorized caller.",
    inputSchema: ragQueryInputSchema
  },
  {
    name: "rag_get_document",
    description: "Fetch a registered source document by source id and relative path. Reads are constrained to registered source roots.",
    inputSchema: ragDocumentInputSchema
  },
  {
    name: "rag_list_projects",
    description: "List projects registered in the local RAG source registry.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false }
  },
  {
    name: "rag_list_sources",
    description: "List source roots registered in the local RAG source registry.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false }
  },
  {
    name: "rag_index_status",
    description: "Show local RAG indexing status and document/chunk counts.",
    inputSchema: { type: "object", properties: {}, additionalProperties: false }
  },
  {
    name: "rag_force_scan",
    description: "Force a local RAG scan for all sources or one project. Use only when explicitly asked or when freshness is required.",
    inputSchema: {
      type: "object",
      properties: {
        projectId: {
          type: "string",
          description: "Optional project id from the local source registry to scan."
        }
      },
      additionalProperties: false
    }
  }
];

let inputBuffer = Buffer.alloc(0);
let transportMode = "framed";
let pendingMessages = 0;
let stdinEnded = false;

process.stdin.on("data", (chunk) => {
  inputBuffer = Buffer.concat([inputBuffer, chunk]);
  processMessages();
});

process.stdin.on("end", () => {
  stdinEnded = true;
  maybeExitWhenIdle();
});

function processMessages() {
  while (true) {
    const headerEnd = inputBuffer.indexOf("\r\n\r\n");
    if (headerEnd < 0) {
      const newlineEnd = inputBuffer.indexOf("\n");
      if (newlineEnd < 0) {
        return;
      }
      const line = inputBuffer.subarray(0, newlineEnd).toString("utf8").trim();
      inputBuffer = inputBuffer.subarray(newlineEnd + 1);
      if (line.length === 0) {
        continue;
      }
      transportMode = "line";
      dispatchMessage(line);
      continue;
    }

    const header = inputBuffer.subarray(0, headerEnd).toString("utf8");
    const match = header.match(/content-length:\s*(\d+)/i);
    if (!match) {
      throw new Error(`Missing Content-Length header: ${header}`);
    }

    const contentLength = Number.parseInt(match[1], 10);
    const bodyStart = headerEnd + 4;
    const messageEnd = bodyStart + contentLength;
    if (inputBuffer.length < messageEnd) {
      return;
    }

    const body = inputBuffer.subarray(bodyStart, messageEnd).toString("utf8");
    inputBuffer = inputBuffer.subarray(messageEnd);
    transportMode = "framed";
    dispatchMessage(body);
  }
}

function dispatchMessage(body) {
  const parsed = JSON.parse(body);
  pendingMessages += 1;
  handleMessage(parsed)
    .catch((error) => {
      if (parsed.id !== undefined && parsed.id !== null) {
        sendError(parsed.id, -32603, error.message);
        return;
      }
      console.error(error);
    })
    .finally(() => {
      pendingMessages -= 1;
      maybeExitWhenIdle();
    });
}

async function handleMessage(message) {
  if (message.id === undefined || message.id === null) {
    return;
  }

  switch (message.method) {
    case "initialize":
      sendResult(message.id, {
        protocolVersion: message.params?.protocolVersion || "2024-11-05",
        capabilities: { tools: { listChanged: false } },
        serverInfo: { name: "local-rag", version: "0.1.0" }
      });
      return;
    case "ping":
      sendResult(message.id, {});
      return;
    case "tools/list":
      sendResult(message.id, { tools });
      return;
    case "tools/call":
      await callTool(message.id, message.params || {});
      return;
    default:
      sendError(message.id, -32601, `Unsupported method: ${message.method}`);
  }
}

async function callTool(id, params) {
  const name = params.name;
  const args = params.arguments || {};

  try {
    let result;
    if (name === "rag_search") {
      const body = normalizeSearchArgs(args);
      result = await requestJson("POST", "/api/mcp/rag_search", body);
    } else if (name === "rag_answer") {
      const body = normalizeSearchArgs(args);
      result = await requestJson("POST", "/api/mcp/rag_answer", body);
    } else if (name === "rag_get_document") {
      const body = {
        sourceId: args.sourceId,
        relativePath: args.relativePath,
        maxBytes: args.maxBytes || undefined
      };
      result = await requestJson("POST", "/api/mcp/rag_get_document", body);
    } else if (name === "rag_list_projects") {
      result = await requestJson("GET", "/api/mcp/rag_list_projects");
    } else if (name === "rag_list_sources") {
      result = await requestJson("GET", "/api/mcp/rag_list_sources");
    } else if (name === "rag_index_status") {
      result = await requestJson("GET", "/api/mcp/rag_index_status");
    } else if (name === "rag_force_scan") {
      const projectId = args.projectId ? `?projectId=${encodeURIComponent(args.projectId)}` : "";
      result = await requestJson("POST", `/api/mcp/rag_force_scan${projectId}`);
    } else {
      sendError(id, -32602, `Unknown tool: ${name}`);
      return;
    }

    sendResult(id, {
      content: [
        {
          type: "text",
          text: JSON.stringify(result, null, 2)
        }
      ],
      isError: false
    });
  } catch (error) {
    sendResult(id, {
      content: [
        {
          type: "text",
          text: `local-rag tool failed: ${error.message}`
        }
      ],
      isError: true
    });
  }
}

function normalizeSearchArgs(args) {
  return {
    ...args,
    projectId: args.projectId || defaultProjectId || undefined,
    limit: args.limit || 5,
    mode: normalizeMode(args.mode || "hybrid")
  };
}

function normalizeMode(mode) {
  const normalized = String(mode || "hybrid").trim().toLowerCase();
  if (normalized === "bm25") {
    return "keyword";
  }
  if (["hybrid", "vector", "keyword"].includes(normalized)) {
    return normalized;
  }
  throw new Error(`Unsupported rag_search mode: ${mode}`);
}

async function requestJson(method, path, body) {
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

function sendResult(id, result) {
  send({ jsonrpc: "2.0", id, result });
}

function sendError(id, code, message) {
  send({ jsonrpc: "2.0", id, error: { code, message } });
}

function send(message) {
  const json = JSON.stringify(message);
  if (transportMode === "line") {
    process.stdout.write(`${json}\n`);
    return;
  }
  process.stdout.write(`Content-Length: ${Buffer.byteLength(json, "utf8")}\r\n\r\n${json}`);
}

function maybeExitWhenIdle() {
  if (stdinEnded && pendingMessages === 0 && inputBuffer.length === 0) {
    setImmediate(() => process.exit(0));
  }
}
