package com.localrag.indexer;

import com.localrag.common.registry.SourceRoot;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.LinkedHashSet;
import java.util.List;

class SourcePathFilter {
    private static final String DIRECTORY_SENTINEL = "__local_rag_directory__";

    private final Path root;
    private final List<PathMatcher> includeMatchers;
    private final List<PathMatcher> excludeMatchers;

    SourcePathFilter(SourceRoot source) {
        this.root = source.path();
        this.includeMatchers = compile(source.include());
        this.excludeMatchers = compile(source.exclude());
    }

    boolean shouldVisitDirectory(Path directory) {
        if (root.equals(directory)) {
            return true;
        }
        String relative = relative(directory);
        return !matches(excludeMatchers, relative)
                && !matches(excludeMatchers, relative + "/" + DIRECTORY_SENTINEL);
    }

    boolean shouldIndexFile(Path file) {
        String relative = relative(file);
        if (matches(excludeMatchers, relative)) {
            return false;
        }
        return includeMatchers.isEmpty() || matches(includeMatchers, relative);
    }

    private String relative(Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static List<PathMatcher> compile(List<String> patterns) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String normalized = pattern.replace('\\', '/');
            expanded.add(normalized);
            if (normalized.startsWith("**/")) {
                expanded.add(normalized.substring(3));
            }
        }
        return expanded.stream()
                .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
                .toList();
    }

    private static boolean matches(List<PathMatcher> matchers, String relativePath) {
        Path path = Path.of(relativePath);
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path)) {
                return true;
            }
        }
        return false;
    }
}
