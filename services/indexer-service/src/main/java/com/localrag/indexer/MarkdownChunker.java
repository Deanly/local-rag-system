package com.localrag.indexer;

import com.localrag.common.embedding.EmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownChunker {
    private static final Logger LOG = LoggerFactory.getLogger(MarkdownChunker.class);
    private static final int OVERLAP_CHARS = 220;
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[[^\\]]+\\]\\(([^)]+)\\)");

    public List<ChunkCandidate> chunk(String text, int targetChars) {
        return chunkDocument(text, targetChars, DocumentDefaults.empty()).chunks();
    }

    public ChunkedDocument chunkDocument(String text, int targetChars, DocumentDefaults defaults) {
        String sourceText = text == null ? "" : text;
        ParsedDocument parsed;
        try {
            parsed = splitFrontmatter(sourceText);
        } catch (YAMLException exception) {
            // Source notes are read-only. Retain every byte of text for retrieval,
            // but do not trust authority or status from an invalid YAML header.
            LOG.warn("Indexing {} as plain text because its YAML frontmatter is invalid",
                    defaults == null ? "<unknown document>" : defaults.relativePath());
            parsed = new ParsedDocument(Map.of(), sourceText, 0);
        }
        String body = parsed.body();
        String firstH1 = firstH1(body);
        DocumentMetadata metadata = documentMetadata(parsed.frontmatter(), defaults, firstH1, body);

        List<ChunkCandidate> chunks = new ArrayList<>();
        List<String> headingStack = new ArrayList<>();
        HeadingSnapshot currentHeading = HeadingSnapshot.forDocument(metadata.title());
        List<String> currentLines = new ArrayList<>();
        int currentStart = parsed.bodyOffset();
        int position = parsed.bodyOffset();
        boolean inCodeFence = false;

        for (String line : body.split("\\R", -1)) {
            Matcher headingMatcher = HEADING_PATTERN.matcher(line);
            if (!inCodeFence && headingMatcher.matches()) {
                if (hasBodyContent(currentLines)) {
                    addChunk(chunks, metadata, currentHeading, currentLines, currentStart, position);
                    currentLines = new ArrayList<>();
                    currentStart = position;
                } else if (!hasMeaningfulContent(currentLines)) {
                    currentLines.clear();
                    currentStart = position;
                }
                int depth = headingMatcher.group(1).length();
                updateHeadingStack(headingStack, depth, headingMatcher.group(2).trim());
                currentHeading = HeadingSnapshot.fromStack(metadata.title(), headingStack, depth);
            }

            if (currentLines.isEmpty()) {
                currentStart = position;
            }
            currentLines.add(line);
            inCodeFence = updateCodeFenceState(inCodeFence, line);
            position += line.length() + 1;

            if (charLength(currentLines) >= targetChars && !inCodeFence && !isTableLine(line)) {
                addChunk(chunks, metadata, currentHeading, currentLines, currentStart, position);
                List<String> overlap = overlapLines(currentLines);
                currentLines = new ArrayList<>(overlap);
                currentStart = Math.max(parsed.bodyOffset(), position - charLength(overlap));
            }
        }

        if (hasMeaningfulContent(currentLines)) {
            addChunk(chunks, metadata, currentHeading, currentLines, currentStart, text == null ? 0 : text.length());
        }
        return new ChunkedDocument(metadata, List.copyOf(chunks));
    }

    private static void addChunk(
            List<ChunkCandidate> chunks,
            DocumentMetadata metadata,
            HeadingSnapshot heading,
            List<String> lines,
            int start,
            int end
    ) {
        String rawContent = String.join("\n", lines).trim();
        if (rawContent.isBlank()) {
            return;
        }
        String headingPath = String.join(" > ", heading.path());
        String chunkContext = chunkContext(metadata, headingPath);
        String content = chunkContext + "\n\n" + rawContent;
        chunks.add(new ChunkCandidate(
                chunks.size(),
                headingPath,
                content,
                start,
                end,
                EmbeddingClient.sha256Hex(content),
                metadata,
                heading.path(),
                heading.depth(),
                slugify(headingPath),
                chunkContext,
                estimateTokens(content)
        ));
    }

    private static ParsedDocument splitFrontmatter(String text) {
        if (!(text.startsWith("---\n") || text.startsWith("---\r\n"))) {
            return new ParsedDocument(Map.of(), text, 0);
        }
        int yamlStart = text.startsWith("---\r\n") ? 5 : 4;
        int cursor = yamlStart;
        while (cursor < text.length()) {
            int lineEnd = nextLineEnd(text, cursor);
            String line = text.substring(cursor, lineEnd).trim();
            int nextLineStart = nextLineStart(text, lineEnd);
            if ("---".equals(line) || "...".equals(line)) {
                String frontmatter = text.substring(yamlStart, cursor);
                return new ParsedDocument(parseFrontmatter(frontmatter), text.substring(nextLineStart), nextLineStart);
            }
            cursor = nextLineStart;
        }
        return new ParsedDocument(Map.of(), text, 0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseFrontmatter(String frontmatter) {
        Object raw = new Yaml().load(frontmatter);
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private static DocumentMetadata documentMetadata(
            Map<String, Object> frontmatter,
            DocumentDefaults defaults,
            String firstH1,
            String body
    ) {
        DocumentDefaults safeDefaults = defaults == null ? DocumentDefaults.empty() : defaults;
        String fallbackTitle = titleFromPath(safeDefaults.relativePath());
        String title = firstNonBlank(stringValue(frontmatter, "title"), firstH1, fallbackTitle);
        Instant fileModifiedAt = safeDefaults.fileModifiedAt() == null ? Instant.EPOCH : safeDefaults.fileModifiedAt();
        return new DocumentMetadata(
                title,
                normalizeDocType(firstNonBlank(stringValue(frontmatter, "type"), docTypeFromPath(safeDefaults.relativePath()))),
                normalizeValue(firstNonBlank(stringValue(frontmatter, "status"), "unknown"), "unknown"),
                normalizeValue(firstNonBlank(stringValue(frontmatter, "authority"), authorityFromSource(safeDefaults)), "source-default"),
                canonicalUpdated(firstNonBlank(
                        stringValue(frontmatter, "updated"),
                        stringValue(frontmatter, "updatedAt"),
                        stringValue(frontmatter, "lastUpdated")
                ), fileModifiedAt),
                stringList(frontmatter.get("supersedes")),
                stringList(firstPresent(frontmatter, "supersededBy", "superseded_by")),
                stringList(frontmatter.get("tags")),
                extractLinks(body)
        );
    }

    private static String chunkContext(DocumentMetadata metadata, String headingPath) {
        return """
                Title: %s
                Document: type=%s, status=%s, authority=%s, updated=%s
                Heading: %s
                ---
                """.formatted(
                metadata.title(),
                metadata.docType(),
                metadata.frontmatterStatus(),
                metadata.authority(),
                metadata.updated(),
                headingPath == null || headingPath.isBlank() ? metadata.title() : headingPath
        ).trim();
    }

    private static void updateHeadingStack(List<String> headingStack, int depth, String heading) {
        while (headingStack.size() >= depth) {
            headingStack.remove(headingStack.size() - 1);
        }
        while (headingStack.size() < depth - 1) {
            headingStack.add("");
        }
        headingStack.add(heading);
    }

    private static boolean hasMeaningfulContent(List<String> lines) {
        return lines.stream().anyMatch(line -> !line.isBlank());
    }

    private static boolean hasBodyContent(List<String> lines) {
        return lines.stream().anyMatch(line -> !line.isBlank() && !HEADING_PATTERN.matcher(line).matches());
    }

    private static List<String> overlapLines(List<String> lines) {
        List<String> overlap = new ArrayList<>();
        int chars = 0;
        for (int i = lines.size() - 1; i >= 0 && chars < OVERLAP_CHARS; i--) {
            String line = lines.get(i);
            overlap.add(0, line);
            chars += line.length() + 1;
        }
        if (overlap.size() == lines.size()) {
            return List.of();
        }
        return overlap;
    }

    private static int charLength(List<String> lines) {
        int length = 0;
        for (String line : lines) {
            length += line.length() + 1;
        }
        return length;
    }

    private static boolean updateCodeFenceState(boolean inCodeFence, String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            return !inCodeFence;
        }
        return inCodeFence;
    }

    private static boolean isTableLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("|") && trimmed.endsWith("|");
    }

    private static String firstH1(String body) {
        for (String line : body.split("\\R", -1)) {
            Matcher matcher = HEADING_PATTERN.matcher(line);
            if (matcher.matches() && matcher.group(1).length() == 1) {
                return matcher.group(2).trim();
            }
        }
        return "";
    }

    private static int nextLineEnd(String text, int start) {
        int lineFeed = text.indexOf('\n', start);
        if (lineFeed < 0) {
            return text.length();
        }
        return lineFeed > start && text.charAt(lineFeed - 1) == '\r' ? lineFeed - 1 : lineFeed;
    }

    private static int nextLineStart(String text, int lineEnd) {
        if (lineEnd >= text.length()) {
            return text.length();
        }
        if (text.charAt(lineEnd) == '\r' && lineEnd + 1 < text.length() && text.charAt(lineEnd + 1) == '\n') {
            return lineEnd + 2;
        }
        return lineEnd + 1;
    }

    private static String titleFromPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "document";
        }
        String normalized = relativePath.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String docTypeFromPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "document";
        }
        String normalized = relativePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        String firstSegment = normalized.contains("/") ? normalized.substring(0, normalized.indexOf('/')) : normalized;
        return switch (firstSegment) {
            case "design" -> "design";
            case "tasks" -> "task";
            case "projects" -> "project";
            case "guide", "guides" -> "guide";
            case "reports" -> "report";
            case "runbook", "runbooks" -> "runbook";
            case "meeting", "meetings", "notes", "memos" -> "memo";
            default -> "document";
        };
    }

    private static String normalizeDocType(String value) {
        String normalized = normalizeValue(value, "document");
        return switch (normalized) {
            case "tasks" -> "task";
            case "projects" -> "project";
            case "guides" -> "guide";
            case "reports" -> "report";
            case "runbooks" -> "runbook";
            case "meeting-note", "meeting-notes", "notes", "memos" -> "memo";
            default -> normalized;
        };
    }

    private static String authorityFromSource(DocumentDefaults defaults) {
        if (defaults == null) {
            return "source-default";
        }
        String sourceType = normalizeValue(defaults.sourceType(), "");
        String ssotRole = normalizeValue(defaults.ssotRole(), "");
        if ("archive".equals(sourceType) || "historical-evidence".equals(ssotRole)) {
            return "reference";
        }
        if ("planning-meta".equals(sourceType) || "raw-source".equals(ssotRole)) {
            return "raw";
        }
        return "source-default";
    }

    private static String canonicalUpdated(String rawValue, Instant defaultInstant) {
        if (rawValue != null && !rawValue.isBlank()) {
            String trimmed = rawValue.trim();
            try {
                return Instant.parse(trimmed).toString();
            } catch (DateTimeParseException ignored) {
                // Try date-only frontmatter below.
            }
            try {
                return LocalDate.parse(trimmed).atStartOfDay().toInstant(ZoneOffset.UTC).toString();
            } catch (DateTimeParseException ignored) {
                // Fall back to file mtime below.
            }
        }
        return (defaultInstant == null ? Instant.EPOCH : defaultInstant).toString();
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return "";
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().toString();
        }
        if (value instanceof LocalDate date) {
            return date.atStartOfDay().toInstant(ZoneOffset.UTC).toString();
        }
        return String.valueOf(value).trim();
    }

    private static Object firstPresent(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private static List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> values) {
            return values.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        String scalar = String.valueOf(value).trim();
        if (scalar.isBlank()) {
            return List.of();
        }
        if (scalar.contains(",")) {
            return Pattern.compile(",").splitAsStream(scalar)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        return List.of(scalar);
    }

    private static List<String> extractLinks(String body) {
        List<String> links = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(body);
        while (matcher.find()) {
            links.add(matcher.group(1).trim());
        }
        return List.copyOf(links);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalizeValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
    }

    private static String slugify(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "section" : normalized;
    }

    private static int estimateTokens(String value) {
        Matcher matcher = TOKEN_PATTERN.matcher(value == null ? "" : value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return Math.max(1, count);
    }

    private record ParsedDocument(Map<String, Object> frontmatter, String body, int bodyOffset) {
    }

    private record HeadingSnapshot(List<String> path, int depth) {
        private HeadingSnapshot {
            path = path == null || path.isEmpty() ? List.of("document") : List.copyOf(path);
        }

        static HeadingSnapshot forDocument(String title) {
            return new HeadingSnapshot(List.of(firstNonBlank(title, "document")), 0);
        }

        static HeadingSnapshot fromStack(String title, List<String> headingStack, int depth) {
            List<String> path = new ArrayList<>();
            String safeTitle = firstNonBlank(title, "document");
            path.add(safeTitle);
            for (String heading : headingStack) {
                if (heading != null && !heading.isBlank() && !heading.equals(safeTitle)) {
                    path.add(heading);
                }
            }
            return new HeadingSnapshot(path, depth);
        }
    }

    public record DocumentDefaults(
            String relativePath,
            Instant fileModifiedAt,
            String sourceType,
            String ssotRole
    ) {
        public static DocumentDefaults empty() {
            return new DocumentDefaults("", Instant.EPOCH, "", "");
        }
    }

    public record DocumentMetadata(
            String title,
            String docType,
            String frontmatterStatus,
            String authority,
            String updated,
            List<String> supersedes,
            List<String> supersededBy,
            List<String> tags,
            List<String> links
    ) {
        public DocumentMetadata {
            title = firstNonBlank(title, "document");
            docType = normalizeDocType(docType);
            frontmatterStatus = normalizeValue(frontmatterStatus, "unknown");
            authority = normalizeValue(authority, "source-default");
            updated = firstNonBlank(updated, Instant.EPOCH.toString());
            supersedes = supersedes == null ? List.of() : List.copyOf(supersedes);
            supersededBy = supersededBy == null ? List.of() : List.copyOf(supersededBy);
            tags = tags == null ? List.of() : List.copyOf(tags);
            links = links == null ? List.of() : List.copyOf(links);
        }
    }

    public record ChunkedDocument(DocumentMetadata metadata, List<ChunkCandidate> chunks) {
        public ChunkedDocument {
            chunks = chunks == null ? List.of() : List.copyOf(chunks);
        }
    }

    public record ChunkCandidate(
            int index,
            String headingPath,
            String content,
            int startChar,
            int endChar,
            String contentHash,
            DocumentMetadata metadata,
            List<String> headingPathSegments,
            int headingDepth,
            String headingSlug,
            String chunkContext,
            int tokenEstimate
    ) {
        public ChunkCandidate {
            headingPathSegments = headingPathSegments == null ? List.of() : List.copyOf(headingPathSegments);
        }
    }
}
