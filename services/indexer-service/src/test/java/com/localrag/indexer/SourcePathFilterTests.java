package com.localrag.indexer;

import com.localrag.common.registry.SourceRoot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourcePathFilterTests {
    @Test
    void matchesRootFilesForDoubleStarIncludes() {
        Path root = Path.of("/source/docs");
        SourcePathFilter filter = new SourcePathFilter(source(root,
                List.of("**/*.md", "**/*.txt"),
                List.of("**/.git/**")));

        assertThat(filter.shouldIndexFile(root.resolve("README.md"))).isTrue();
        assertThat(filter.shouldIndexFile(root.resolve("guide/setup.txt"))).isTrue();
        assertThat(filter.shouldIndexFile(root.resolve("guide/setup.yaml"))).isFalse();
    }

    @Test
    void excludesRootAndNestedDirectories() {
        Path root = Path.of("/source/docs");
        SourcePathFilter filter = new SourcePathFilter(source(root,
                List.of("**/*.md"),
                List.of("**/.git/**", "**/legacy/**")));

        assertThat(filter.shouldVisitDirectory(root.resolve(".git"))).isFalse();
        assertThat(filter.shouldVisitDirectory(root.resolve("legacy"))).isFalse();
        assertThat(filter.shouldVisitDirectory(root.resolve("docs/legacy"))).isFalse();
        assertThat(filter.shouldIndexFile(root.resolve(".git/notes.md"))).isFalse();
        assertThat(filter.shouldIndexFile(root.resolve("docs/legacy/old.md"))).isFalse();
        assertThat(filter.shouldIndexFile(root.resolve("docs/design/current.md"))).isTrue();
    }

    private static SourceRoot source(Path root, List<String> include, List<String> exclude) {
        return new SourceRoot(
                "test.docs",
                "test",
                "project-docs",
                "project-current-truth",
                root,
                100,
                true,
                "private",
                include,
                exclude,
                "registered-default",
                "repo-docs"
        );
    }
}
