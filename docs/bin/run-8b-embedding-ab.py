#!/usr/bin/env python3
"""
T0025 / P0004 — Isolated 4b-vs-8b embedding retrieval A/B gate.

Goal: decide whether qwen3-embedding:8b (4096-dim) gives a real retrieval-quality
gain over the operational qwen3-embedding:4b (2560-dim), WITHOUT touching the
operational Weaviate class `LocalRagChunk` or the operational index/model.

Method (isolates the embedding-model effect):
  1. Read a sample of source files (default: personal-core docs/design/*.md).
  2. Chunk them once with a simple, conservative chunker (char cap well under the
     ~2k-token input size that crashes the 8b Ollama runner with a BPT trap).
  3. Embed the SAME chunks with BOTH 4b and 8b (batch=1, sequential, split/truncate
     on failure) and write them to two NEW classes: LocalRagChunk4bRef (2560) and
     LocalRagChunk8bExp (4096).
  4. For a fixed query set, embed each query with 4b -> nearVector search the 4bRef
     class, and with 8b -> nearVector search the 8bExp class. Same chunks, same
     queries -> only the model differs.
  5. Emit a side-by-side top-k comparison report.

Safety:
  - Never creates/deletes/writes the operational class `LocalRagChunk`.
  - Run only when the Mac mini Ollama is otherwise idle (no Codex / other models).
  - stdlib only (urllib); no third-party deps.

Usage:
  python3 docs/bin/run-8b-embedding-ab.py --reset --index --query \
      --report docs/reports/2026-06-17-8b-embedding-ab-gate.md
  python3 docs/bin/run-8b-embedding-ab.py --query   # query only (classes already built)
"""

import argparse
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

# --- Config (override via CLI) ---------------------------------------------
OLLAMA_URL = "http://10.10.10.2:11434"
WEAVIATE_URL = "http://127.0.0.1:42131"
MODEL_4B = "qwen3-embedding:4b"
MODEL_8B = "qwen3-embedding:8b"
CLASS_4B = "LocalRagChunk4bRef"
CLASS_8B = "LocalRagChunk8bExp"
OPERATIONAL_CLASS = "LocalRagChunk"  # MUST NOT be touched
SAMPLE_DIR = "~/Workspace/personal-sandbox/personal-core/docs/design"
CHAR_CAP = 1500          # conservative: well under the ~2k-token BPT-trap threshold
EMBED_RETRIES = 2        # on failure, halve the text and retry (no same-input retry)
TOP_K = 5

# Representative query set (KO/EN, lexical-mismatch, identifier, multi-evidence).
QUERIES = [
    "control plane 도메인 경계와 책임",
    "session workroom provisioning interfaces",
    "운영자 제어 권한과 승인 정책",
    "telegram lobby workroom publisher",
    "domain boundaries bounded context",
    "구조화된 진입과 lane orchestration",
    "stale 문서 demotion 정책",
    "project workspace 위임 흐름",
]


def _post(url, payload, timeout=120):
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def _delete(url, timeout=30):
    req = urllib.request.Request(url, method="DELETE")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status
    except urllib.error.HTTPError as e:
        return e.code


def embed(model, text):
    """Embed one text. On HTTP error, halve and retry (avoids same-input retry on the
    8b runner crash). Returns vector or None if it never succeeds."""
    t = text
    for attempt in range(EMBED_RETRIES + 1):
        try:
            r = _post(f"{OLLAMA_URL}/api/embeddings", {"model": model, "prompt": t}, timeout=120)
            v = r.get("embedding")
            if v:
                return v
            raise ValueError(f"no embedding field: {list(r.keys())}")
        except (urllib.error.HTTPError, urllib.error.URLError, ValueError, TimeoutError) as e:
            t = t[: max(200, len(t) // 2)]  # shrink, then retry
            sys.stderr.write(f"  [embed retry {attempt+1}] {model} err={e}; shrink->{len(t)} chars\n")
            time.sleep(1.0)
    return None


# --- Simple conservative chunker -------------------------------------------
def chunk_markdown(text, char_cap=CHAR_CAP):
    """Split on blank lines, accumulate paragraphs up to char_cap. Hard-split any
    single paragraph longer than char_cap. Returns list of chunk strings."""
    paras = [p.strip() for p in text.replace("\r\n", "\n").split("\n\n") if p.strip()]
    chunks, buf = [], ""
    for p in paras:
        while len(p) > char_cap:
            if buf:
                chunks.append(buf); buf = ""
            chunks.append(p[:char_cap]); p = p[char_cap:]
        if len(buf) + len(p) + 2 > char_cap:
            if buf:
                chunks.append(buf)
            buf = p
        else:
            buf = (buf + "\n\n" + p) if buf else p
    if buf:
        chunks.append(buf)
    return chunks


def collect_sample(max_files=0):
    base = Path(SAMPLE_DIR)
    items = []  # (relative_path, chunk_index, text)
    files = sorted(base.rglob("*.md"))
    if max_files:
        files = files[:max_files]
    for f in files:
        try:
            raw = f.read_text(encoding="utf-8")
        except Exception as e:
            sys.stderr.write(f"  skip {f}: {e}\n")
            continue
        rel = str(f.relative_to(base.parent))  # e.g. design/foo.md
        for i, ch in enumerate(chunk_markdown(raw)):
            items.append((rel, i, ch))
    return items


# --- Weaviate helpers ------------------------------------------------------
def ensure_class(name, drop=False):
    if drop:
        _delete(f"{WEAVIATE_URL}/v1/schema/{name}")
    schema = {
        "class": name,
        "vectorizer": "none",
        "properties": [
            {"name": "text", "dataType": ["text"]},
            {"name": "relativePath", "dataType": ["text"]},
            {"name": "chunkIndex", "dataType": ["int"]},
        ],
    }
    try:
        _post(f"{WEAVIATE_URL}/v1/schema", schema, timeout=30)
    except urllib.error.HTTPError as e:
        if e.code != 422:  # 422 = already exists
            raise


def insert(name, props, vector):
    _post(f"{WEAVIATE_URL}/v1/objects", {"class": name, "properties": props, "vector": vector}, timeout=60)


def near_vector_search(name, vector, k=TOP_K):
    vec = "[" + ",".join(repr(float(x)) for x in vector) + "]"
    q = (
        "{ Get { %s(nearVector:{vector:%s}, limit:%d){ relativePath chunkIndex "
        "_additional{distance} } } }" % (name, vec, k)
    )
    r = _post(f"{WEAVIATE_URL}/v1/graphql", {"query": q}, timeout=60)
    rows = (r.get("data", {}).get("Get", {}) or {}).get(name) or []
    return [(x["relativePath"], x["chunkIndex"], round(x["_additional"]["distance"], 4)) for x in rows]


# --- Phases ----------------------------------------------------------------
def do_index(reset, max_files=0):
    assert CLASS_4B != OPERATIONAL_CLASS and CLASS_8B != OPERATIONAL_CLASS
    items = collect_sample(max_files)
    print(f"sample: {len(items)} chunks from {SAMPLE_DIR}")
    ensure_class(CLASS_4B, drop=reset)
    ensure_class(CLASS_8B, drop=reset)
    stats = {MODEL_4B: {"ok": 0, "fail": 0}, MODEL_8B: {"ok": 0, "fail": 0}}
    t0 = time.time()
    for n, (rel, idx, text) in enumerate(items, 1):
        props = {"text": text, "relativePath": rel, "chunkIndex": idx}
        for model, cls in ((MODEL_4B, CLASS_4B), (MODEL_8B, CLASS_8B)):
            v = embed(model, text)
            if v is None:
                stats[model]["fail"] += 1
                continue
            insert(cls, props, v)
            stats[model]["ok"] += 1
        if n % 25 == 0:
            print(f"  {n}/{len(items)}  elapsed={int(time.time()-t0)}s  {stats}")
    print(f"index done in {int(time.time()-t0)}s: {stats}")
    return stats


def do_query():
    lines = []
    for qtext in QUERIES:
        v4 = embed(MODEL_4B, qtext)
        v8 = embed(MODEL_8B, qtext)
        r4 = near_vector_search(CLASS_4B, v4) if v4 else []
        r8 = near_vector_search(CLASS_8B, v8) if v8 else []
        set4 = {(p, i) for p, i, _ in r4}
        set8 = {(p, i) for p, i, _ in r8}
        overlap = len(set4 & set8)
        lines.append((qtext, r4, r8, overlap))
    return lines


def render_report(stats, qlines):
    out = []
    out.append("# 8b-embedding-ab-gate (T0025 / P0004)\n")
    out.append("- Generated by `docs/bin/run-8b-embedding-ab.py`")
    out.append(f"- Sample dir: `{SAMPLE_DIR}`")
    out.append(f"- Classes: `{CLASS_4B}` (4b/2560) vs `{CLASS_8B}` (8b/4096); operational `{OPERATIONAL_CLASS}` untouched")
    out.append(f"- Char cap per chunk: {CHAR_CAP}; batch=1; embed retries(shrink)={EMBED_RETRIES}\n")
    if stats:
        out.append("## Index stats\n")
        out.append("| model | indexed | failed |")
        out.append("| --- | ---: | ---: |")
        for m, s in stats.items():
            out.append(f"| {m} | {s['ok']} | {s['fail']} |")
        out.append("")
    out.append("## Per-query top-k (4b vs 8b)\n")
    for qtext, r4, r8, overlap in qlines:
        out.append(f"### `{qtext}`  — top-{TOP_K} overlap: {overlap}/{TOP_K}\n")
        out.append("| rank | 4b (path#chunk, dist) | 8b (path#chunk, dist) |")
        out.append("| ---: | --- | --- |")
        for i in range(TOP_K):
            a = f"{r4[i][0]}#{r4[i][1]} ({r4[i][2]})" if i < len(r4) else ""
            b = f"{r8[i][0]}#{r8[i][1]} ({r8[i][2]})" if i < len(r8) else ""
            out.append(f"| {i+1} | {a} | {b} |")
        out.append("")
    out.append("## Verdict\n")
    out.append("- TODO(human): 8b가 4b 대비 의미 있는 품질 이득이 있는가? yes / no / unclear")
    out.append("- 판정 근거(질의 유형별 관찰)와 P0004 다음 단계(T0026 발급 또는 4b 유지)를 적는다.\n")
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--reset", action="store_true", help="drop+recreate the experiment classes")
    ap.add_argument("--index", action="store_true", help="embed sample into both classes")
    ap.add_argument("--query", action="store_true", help="run the query A/B")
    ap.add_argument("--max-files", type=int, default=0, help="limit sample to first N files (0=all) for a cheap first pass")
    ap.add_argument("--report", default="", help="write markdown report to this path")
    args = ap.parse_args()
    if not (args.index or args.query):
        ap.error("nothing to do: pass --index and/or --query")

    # Hard guard: never operate on the operational class.
    assert OPERATIONAL_CLASS not in (CLASS_4B, CLASS_8B)

    stats = do_index(args.reset, args.max_files) if args.index else None
    qlines = do_query() if args.query else []
    if args.report and qlines:
        Path(args.report).write_text(render_report(stats, qlines), encoding="utf-8")
        print(f"report written: {args.report}")
    elif qlines:
        print(render_report(stats, qlines))


if __name__ == "__main__":
    main()
