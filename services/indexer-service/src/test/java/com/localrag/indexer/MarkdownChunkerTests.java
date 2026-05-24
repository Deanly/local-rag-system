package com.localrag.indexer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownChunkerTests {
    @Test
    void preservesHeadingPathOnChunks() {
        MarkdownChunker chunker = new MarkdownChunker();

        var chunks = chunker.chunk("# Title\n\nBody text for local rag.", 1000);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).headingPath()).isEqualTo("Title");
        assertThat(chunks.get(0).content()).contains("Body text");
    }
}
