# Local RAG reranker sidecar

This optional sidecar provides a local cross-encoder score source for ScoreGate
proof runs.

It is intentionally not part of the default Local RAG startup path. Start it with
the `scoregate` compose profile when you need real `r_i` scores for debug,
snapshot, or calibration work.

## API

- `GET /health`
- `POST /rerank`

Example request:

```json
{
  "query": "배포 릴리즈 기록 어디 남기지?",
  "candidates": [
    {
      "id": "chunk-1",
      "text": "Release notes are recorded in the deployment registry."
    }
  ],
  "normalize": true,
  "batchSize": 8
}
```

Example response:

```json
{
  "model": "BAAI/bge-reranker-v2-m3",
  "normalized": true,
  "scores": [
    {
      "id": "chunk-1",
      "score": 0.76
    }
  ]
}
```

## Environment

- `LOCAL_RAG_RERANKER_MODEL`: defaults to `BAAI/bge-reranker-v2-m3`
- `LOCAL_RAG_RERANKER_MAX_LENGTH`: defaults to `1024`
- `LOCAL_RAG_RERANKER_USE_FP16`: defaults to `false`

The current proof candidate is `BAAI/bge-reranker-v2-m3` because its model card
describes it as multilingual and supports normalized 0-1 scoring through
`FlagReranker.compute_score(..., normalize=True)`.
