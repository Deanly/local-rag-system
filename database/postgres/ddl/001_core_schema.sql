CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS device_profile (
    device_id TEXT PRIMARY KEY,
    display_name TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS project_registration (
    project_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    repo_path TEXT,
    primary_source_id TEXT NOT NULL,
    default_context_source_ids TEXT[] NOT NULL DEFAULT '{}',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS source_root (
    source_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL REFERENCES project_registration(project_id),
    source_type TEXT NOT NULL,
    ssot_role TEXT NOT NULL,
    root_path TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 50,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sensitivity_default TEXT NOT NULL DEFAULT 'private',
    read_policy TEXT NOT NULL,
    write_policy TEXT NOT NULL,
    include_globs TEXT[] NOT NULL DEFAULT '{}',
    exclude_globs TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT source_root_source_type_check CHECK (
        source_type IN ('source-folder', 'project-docs', 'compiled-wiki', 'planning-meta', 'code-source', 'archive')
    ),
    CONSTRAINT source_root_ssot_role_check CHECK (
        ssot_role IN ('project-current-truth', 'compiled-wiki', 'planning-meta', 'historical-evidence', 'code-source', 'raw-source')
    )
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'project_registration_primary_source_fk'
    ) THEN
        ALTER TABLE project_registration
            ADD CONSTRAINT project_registration_primary_source_fk
            FOREIGN KEY (primary_source_id) REFERENCES source_root(source_id)
            DEFERRABLE INITIALLY DEFERRED;
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS document_state (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id TEXT NOT NULL REFERENCES source_root(source_id),
    relative_path TEXT NOT NULL,
    absolute_path_hash TEXT NOT NULL,
    file_name TEXT NOT NULL,
    extension TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    mtime_ns BIGINT NOT NULL,
    sha256 TEXT,
    status TEXT NOT NULL,
    title TEXT NOT NULL DEFAULT '',
    doc_type TEXT NOT NULL DEFAULT 'document',
    frontmatter_status TEXT NOT NULL DEFAULT 'unknown',
    authority TEXT NOT NULL DEFAULT 'source-default',
    document_updated TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z',
    supersedes TEXT[] NOT NULL DEFAULT '{}',
    superseded_by TEXT[] NOT NULL DEFAULT '{}',
    metadata_version INTEGER NOT NULL DEFAULT 0,
    last_detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_indexed_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (source_id, relative_path),
    CONSTRAINT document_state_status_check CHECK (
        status IN ('detected', 'queued', 'indexing', 'indexed', 'changed', 'deleted', 'removing', 'removed', 'failed', 'skipped')
    )
);

CREATE TABLE IF NOT EXISTS chunk_state (
    chunk_id TEXT PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES document_state(document_id) ON DELETE CASCADE,
    source_id TEXT NOT NULL REFERENCES source_root(source_id),
    chunk_index INTEGER NOT NULL,
    heading_path TEXT,
    heading_depth INTEGER NOT NULL DEFAULT 0,
    heading_slug TEXT,
    chunk_context TEXT,
    content_hash TEXT NOT NULL,
    token_estimate INTEGER,
    weaviate_uuid UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, chunk_index)
);

CREATE TABLE IF NOT EXISTS index_job (
    job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id TEXT REFERENCES source_root(source_id),
    document_id UUID REFERENCES document_state(document_id) ON DELETE SET NULL,
    relative_path TEXT,
    job_type TEXT NOT NULL,
    status TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    priority INTEGER NOT NULL DEFAULT 50,
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    error_code TEXT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT index_job_type_check CHECK (job_type IN ('scan-source', 'index-file', 'delete-file', 'force-reindex')),
    CONSTRAINT index_job_status_check CHECK (status IN ('pending', 'running', 'succeeded', 'failed', 'cancelled'))
);

CREATE TABLE IF NOT EXISTS failure_record (
    failure_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES index_job(job_id) ON DELETE SET NULL,
    source_id TEXT REFERENCES source_root(source_id),
    document_id UUID REFERENCES document_state(document_id) ON DELETE SET NULL,
    relative_path TEXT,
    phase TEXT NOT NULL,
    error_code TEXT NOT NULL,
    error_message TEXT NOT NULL,
    retryable BOOLEAN NOT NULL DEFAULT TRUE,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS search_audit (
    search_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id TEXT REFERENCES project_registration(project_id),
    query TEXT NOT NULL,
    mode TEXT NOT NULL,
    limit_requested INTEGER NOT NULL,
    rerank_requested BOOLEAN NOT NULL DEFAULT FALSE,
    sources_searched TEXT[] NOT NULL DEFAULT '{}',
    result_count INTEGER NOT NULL DEFAULT 0,
    latency_ms INTEGER,
    candidate_limit INTEGER NOT NULL DEFAULT 0,
    raw_candidate_count INTEGER NOT NULL DEFAULT 0,
    final_result_count INTEGER NOT NULL DEFAULT 0,
    embedding_latency_ms INTEGER,
    weaviate_latency_ms INTEGER,
    weighting_latency_ms INTEGER,
    rerank_latency_ms INTEGER,
    total_latency_ms INTEGER,
    source_distribution JSONB NOT NULL DEFAULT '{}'::jsonb,
    top_result_source_id TEXT,
    top_result_relative_path TEXT,
    top_result_score JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT search_audit_mode_check CHECK (mode IN ('keyword', 'vector', 'hybrid'))
);

CREATE INDEX IF NOT EXISTS idx_source_root_project ON source_root(project_id);
CREATE INDEX IF NOT EXISTS idx_document_state_source_status ON document_state(source_id, status);
CREATE INDEX IF NOT EXISTS idx_document_state_sha256 ON document_state(sha256);
CREATE INDEX IF NOT EXISTS idx_chunk_state_document ON chunk_state(document_id);
CREATE INDEX IF NOT EXISTS idx_index_job_status_priority ON index_job(status, priority DESC, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_failure_record_retryable ON failure_record(retryable, resolved_at);
CREATE INDEX IF NOT EXISTS idx_search_audit_project_created ON search_audit(project_id, created_at DESC);

ALTER TABLE document_state
    ADD COLUMN IF NOT EXISTS title TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS doc_type TEXT NOT NULL DEFAULT 'document',
    ADD COLUMN IF NOT EXISTS frontmatter_status TEXT NOT NULL DEFAULT 'unknown',
    ADD COLUMN IF NOT EXISTS authority TEXT NOT NULL DEFAULT 'source-default',
    ADD COLUMN IF NOT EXISTS document_updated TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z',
    ADD COLUMN IF NOT EXISTS supersedes TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS superseded_by TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS metadata_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE chunk_state
    ADD COLUMN IF NOT EXISTS heading_depth INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS heading_slug TEXT,
    ADD COLUMN IF NOT EXISTS chunk_context TEXT;

CREATE INDEX IF NOT EXISTS idx_document_state_retrieval_metadata ON document_state(source_id, doc_type, frontmatter_status, authority);

ALTER TABLE search_audit
    ADD COLUMN IF NOT EXISTS candidate_limit INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS raw_candidate_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS final_result_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS embedding_latency_ms INTEGER,
    ADD COLUMN IF NOT EXISTS weaviate_latency_ms INTEGER,
    ADD COLUMN IF NOT EXISTS weighting_latency_ms INTEGER,
    ADD COLUMN IF NOT EXISTS rerank_latency_ms INTEGER,
    ADD COLUMN IF NOT EXISTS total_latency_ms INTEGER,
    ADD COLUMN IF NOT EXISTS source_distribution JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS top_result_source_id TEXT,
    ADD COLUMN IF NOT EXISTS top_result_relative_path TEXT,
    ADD COLUMN IF NOT EXISTS top_result_score JSONB NOT NULL DEFAULT '{}'::jsonb;
