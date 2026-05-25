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

const tools = [
  {
    name: "rag_search",
    description: "Search the local RAG index with project-aware hybrid retrieval and citation-bearing results.",
    inputSchema: ragQueryInputSchema
  },
  {
    name: "rag_answer",
    description: "Generate a local-only answer from retrieved local RAG snippets with citations.",
    inputSchema: ragQueryInputSchema
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

process.stdin.on("data", (chunk) => {
  inputBuffer = Buffer.concat([inputBuffer, chunk]);
  processMessages();
});

process.stdin.on("end", () => process.exit(0));

function processMessages() {
  while (true) {
    const headerEnd = inputBuffer.indexOf("\r\n\r\n");
    if (headerEnd < 0) {
      return;
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
    handleMessage(JSON.parse(body)).catch((error) => {
      if (body.includes("\"id\"")) {
        const parsed = JSON.parse(body);
        sendError(parsed.id, -32603, error.message);
      } else {
        console.error(error);
      }
    });
  }
}

async function handleMessage(message) {
  if (message.id === undefined || message.id === null) {
    return;
  }

  switch (message.method) {
    case "initialize":
      sendResult(message.id, {
        protocolVersion: "2024-11-05",
        capabilities: { tools: {} },
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
      const body = {
        ...args,
        projectId: args.projectId || defaultProjectId || undefined,
        limit: args.limit || 5,
        mode: args.mode || "hybrid"
      };
      result = await requestJson("POST", "/api/mcp/rag_search", body);
    } else if (name === "rag_answer") {
      const body = {
        ...args,
        projectId: args.projectId || defaultProjectId || undefined,
        limit: args.limit || 5,
        mode: args.mode || "hybrid"
      };
      result = await requestJson("POST", "/api/mcp/rag_answer", body);
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
  process.stdout.write(`Content-Length: ${Buffer.byteLength(json, "utf8")}\r\n\r\n${json}`);
}
