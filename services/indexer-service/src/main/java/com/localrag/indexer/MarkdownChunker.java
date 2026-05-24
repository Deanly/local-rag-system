package com.localrag.indexer;

import com.localrag.common.embedding.EmbeddingClient;

import java.util.ArrayList;
import java.util.List;

public class MarkdownChunker {
    public List<ChunkCandidate> chunk(String text, int targetChars) {
        List<ChunkCandidate> chunks = new ArrayList<>();
        String heading = "";
        StringBuilder current = new StringBuilder();
        int start = 0;
        int position = 0;
        for (String line : text.split("\\R", -1)) {
            if (line.startsWith("#")) {
                String trimmed = line.replaceFirst("^#+\\s*", "").trim();
                if (!trimmed.isBlank()) {
                    heading = trimmed;
                }
            }
            if (current.length() == 0) {
                start = position;
            }
            current.append(line).append('\n');
            if (current.length() >= targetChars) {
                addChunk(chunks, heading, current.toString(), start, position + line.length());
                current.setLength(0);
            }
            position += line.length() + 1;
        }
        if (!current.isEmpty()) {
            addChunk(chunks, heading, current.toString(), start, text.length());
        }
        return chunks;
    }

    private void addChunk(List<ChunkCandidate> chunks, String heading, String content, int start, int end) {
        String normalized = content.trim();
        if (!normalized.isBlank()) {
            chunks.add(new ChunkCandidate(chunks.size(), heading, normalized, start, end, EmbeddingClient.sha256Hex(normalized)));
        }
    }

    public record ChunkCandidate(
            int index,
            String headingPath,
            String content,
            int startChar,
            int endChar,
            String contentHash
    ) {
    }
}
