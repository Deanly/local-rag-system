package com.localrag.indexer;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownChunkerTests {
    @Test
    void preservesFrontmatterAndFullHeadingPathOnChunks() {
        MarkdownChunker chunker = new MarkdownChunker();

        var document = chunker.chunkDocument("""
                ---
                title: "Sample ExecutionSkillHub ADR"
                type: adr
                status: accepted
                authority: canonical
                updated: 2026-05-29
                supersedes:
                  - 2026-04-10-toolhub-draft.md
                supersededBy: []
                tags: [mcp, skill]
                ---
                # Sample ExecutionSkillHub ADR

                ## Decision

                Execution skills and instruction skills stay separate.
                """, 1000, new MarkdownChunker.DocumentDefaults(
                "10-adr/2026-05-29-execution-skillhub.md",
                Instant.parse("2026-05-30T00:00:00Z"),
                "project-docs",
                "project-current-truth"
        ));

        assertThat(document.metadata().title()).isEqualTo("Sample ExecutionSkillHub ADR");
        assertThat(document.metadata().docType()).isEqualTo("adr");
        assertThat(document.metadata().frontmatterStatus()).isEqualTo("accepted");
        assertThat(document.metadata().authority()).isEqualTo("canonical");
        assertThat(document.metadata().updated()).isEqualTo("2026-05-29T00:00:00Z");
        assertThat(document.metadata().supersedes()).containsExactly("2026-04-10-toolhub-draft.md");
        assertThat(document.metadata().tags()).containsExactly("mcp", "skill");

        assertThat(document.chunks()).hasSize(1);
        MarkdownChunker.ChunkCandidate chunk = document.chunks().get(0);
        assertThat(chunk.headingPath()).isEqualTo("Sample ExecutionSkillHub ADR > Decision");
        assertThat(chunk.headingPathSegments()).containsExactly("Sample ExecutionSkillHub ADR", "Decision");
        assertThat(chunk.headingDepth()).isEqualTo(2);
        assertThat(chunk.headingSlug()).isEqualTo("sample-executionskillhub-adr-decision");
        assertThat(chunk.chunkContext()).contains("status=accepted", "authority=canonical");
        assertThat(chunk.content()).contains("Title: Sample ExecutionSkillHub ADR", "Execution skills");
        assertThat(chunk.tokenEstimate()).isPositive();
    }

    @Test
    void usesConservativeDefaultsWhenFrontmatterIsMissing() {
        MarkdownChunker chunker = new MarkdownChunker();

        var document = chunker.chunkDocument("""
                # Local RAG Runtime

                Body text for local rag.
                """, 1000, new MarkdownChunker.DocumentDefaults(
                "design/local-rag-runtime.md",
                Instant.parse("2026-05-30T01:02:03Z"),
                "project-docs",
                "project-current-truth"
        ));

        assertThat(document.metadata().title()).isEqualTo("Local RAG Runtime");
        assertThat(document.metadata().docType()).isEqualTo("design");
        assertThat(document.metadata().frontmatterStatus()).isEqualTo("unknown");
        assertThat(document.metadata().authority()).isEqualTo("source-default");
        assertThat(document.metadata().updated()).isEqualTo("2026-05-30T01:02:03Z");
        assertThat(document.metadata().supersedes()).isEmpty();
        assertThat(document.chunks().get(0).content()).contains("status=unknown", "Body text");
    }

    @Test
    void preservesDeprecatedAndSupersededMetadataForLaterGovernance() {
        MarkdownChunker chunker = new MarkdownChunker();

        var document = chunker.chunkDocument("""
                ---
                title: "Old AI Guide"
                type: guide
                status: deprecated
                authority: reference
                supersededBy:
                  - 2026-ai-guide.md
                ---
                # Old AI Guide

                Historical policy.
                """, 1000, new MarkdownChunker.DocumentDefaults(
                "guide/old-ai-guide.md",
                Instant.parse("2026-05-30T00:00:00Z"),
                "archive",
                "historical-evidence"
        ));

        assertThat(document.metadata().frontmatterStatus()).isEqualTo("deprecated");
        assertThat(document.metadata().authority()).isEqualTo("reference");
        assertThat(document.metadata().supersededBy()).containsExactly("2026-ai-guide.md");
        assertThat(document.chunks().get(0).content()).contains("status=deprecated");
    }

    @Test
    void doesNotSplitInsideFencedCodeBlockWhenTargetIsSmall() {
        MarkdownChunker chunker = new MarkdownChunker();

        var document = chunker.chunkDocument("""
                # Troubleshooting

                ```text
                line one has enough content to exceed the tiny chunk target
                line two should remain in the same fenced block
                ```

                After code.
                """, 60, new MarkdownChunker.DocumentDefaults(
                "runbook/troubleshooting.md",
                Instant.parse("2026-05-30T00:00:00Z"),
                "project-docs",
                "project-current-truth"
        ));

        assertThat(document.chunks()).hasSize(2);
        assertThat(document.chunks().get(0).content()).contains("```text", "line two", "```");
        assertThat(document.chunks().get(1).content()).containsAnyOf("After code.", "line two should remain");
    }

    @Test
    void keepsSmallOverlapForLongSections() {
        MarkdownChunker chunker = new MarkdownChunker();

        var document = chunker.chunkDocument("""
                # Long Design

                Alpha section has repeated retrieval governance context for a chunk.
                Beta section has repeated retrieval governance context for a chunk.
                Gamma section has repeated retrieval governance context for a chunk.
                Delta section has repeated retrieval governance context for a chunk.
                """, 120, new MarkdownChunker.DocumentDefaults(
                "design/long-design.md",
                Instant.parse("2026-05-30T00:00:00Z"),
                "project-docs",
                "project-current-truth"
        ));

        assertThat(document.chunks().size()).isGreaterThan(1);
        List<String> chunkTexts = document.chunks().stream().map(MarkdownChunker.ChunkCandidate::content).toList();
        assertThat(chunkTexts.get(1)).contains("retrieval governance context");
    }
}
