#!/usr/bin/env python3
"""Collect Local RAG ScoreGate runtime snapshots from /api/search."""

from __future__ import annotations

import argparse
import datetime as dt
import json
import pathlib
import time
import urllib.error
import urllib.request


ROOT_DIR = pathlib.Path(__file__).resolve().parents[2]
DEFAULT_PROBES = ROOT_DIR / "docs" / "evaluation" / "scoregate-runtime-probes.json"
DEFAULT_OUTPUT_DIR = ROOT_DIR / "docs" / "reports"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--probes", type=pathlib.Path, default=DEFAULT_PROBES)
    parser.add_argument("--url", default="http://127.0.0.1:42120/api/search")
    parser.add_argument("--scoregate-mode", choices=["debug", "on"], default=None)
    parser.add_argument("--timeout", type=float, default=30.0)
    parser.add_argument("--output", type=pathlib.Path, default=None)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    probes_payload = json.loads(args.probes.read_text(encoding="utf-8"))
    scoregate_mode = args.scoregate_mode or probes_payload.get("defaultScoreGateMode", "debug")
    collected_at = dt.datetime.now(dt.timezone.utc).isoformat()

    rows = []
    for probe in probes_payload.get("probes", []):
        request_payload = with_scoregate_filter(probe.get("request", {}), scoregate_mode)
        if args.dry_run:
            rows.append({
                "id": probe.get("id"),
                "intent": probe.get("intent"),
                "request": request_payload,
                "dryRun": True,
            })
            continue
        rows.append(call_probe(args.url, probe, request_payload, args.timeout))

    output_payload = {
        "version": 1,
        "collectedAt": collected_at,
        "searchUrl": args.url,
        "scoreGateMode": scoregate_mode,
        "probeFile": str(args.probes),
        "results": rows,
    }
    output_path = args.output or default_output_path(scoregate_mode)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(output_payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(output_path)
    return 0


def with_scoregate_filter(request_payload: dict, scoregate_mode: str) -> dict:
    payload = json.loads(json.dumps(request_payload))
    filters = payload.setdefault("filters", {})
    filters["scoreGate"] = [scoregate_mode]
    return payload


def call_probe(url: str, probe: dict, request_payload: dict, timeout: float) -> dict:
    body = json.dumps(request_payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response_payload = json.loads(response.read().decode("utf-8"))
            latency_ms = int((time.perf_counter() - started) * 1000)
            return {
                "id": probe.get("id"),
                "intent": probe.get("intent"),
                "expectedRelativePaths": probe.get("expectedRelativePaths", []),
                "status": "ok",
                "httpStatus": response.status,
                "latencyMs": latency_ms,
                "request": request_payload,
                "summary": summarize(response_payload),
                "response": response_payload,
            }
    except urllib.error.HTTPError as exc:
        return error_row(probe, request_payload, int((time.perf_counter() - started) * 1000), exc.code, exc.read().decode("utf-8"))
    except Exception as exc:
        return error_row(probe, request_payload, int((time.perf_counter() - started) * 1000), None, str(exc))


def summarize(response_payload: dict) -> dict:
    results = response_payload.get("results", [])
    top = results[0] if results else {}
    buckets = {}
    retained = 0
    fallback_reasons = {}
    for result in results:
        score = result.get("score", {})
        bucket = score.get("scoreGateBucket")
        if bucket:
            buckets[bucket] = buckets.get(bucket, 0) + 1
        if score.get("scoreGateRetained") is True:
            retained += 1
        fallback = score.get("scoreGateFallbackReason")
        if fallback:
            fallback_reasons[fallback] = fallback_reasons.get(fallback, 0) + 1
    return {
        "resultCount": len(results),
        "topRelativePath": top.get("relativePath"),
        "topChunkId": top.get("chunkId"),
        "scoreGateBuckets": buckets,
        "scoreGateRetainedCount": retained,
        "scoreGateFallbackReasons": fallback_reasons,
    }


def error_row(probe: dict, request_payload: dict, latency_ms: int, http_status: int | None, error: str) -> dict:
    return {
        "id": probe.get("id"),
        "intent": probe.get("intent"),
        "expectedRelativePaths": probe.get("expectedRelativePaths", []),
        "status": "error",
        "httpStatus": http_status,
        "latencyMs": latency_ms,
        "request": request_payload,
        "error": error,
    }


def default_output_path(scoregate_mode: str) -> pathlib.Path:
    stamp = dt.datetime.now().strftime("%Y-%m-%d-%H%M%S")
    return DEFAULT_OUTPUT_DIR / f"{stamp}-scoregate-runtime-snapshot-{scoregate_mode}.json"


if __name__ == "__main__":
    raise SystemExit(main())
