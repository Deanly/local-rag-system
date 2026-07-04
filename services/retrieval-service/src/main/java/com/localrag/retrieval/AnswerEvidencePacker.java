package com.localrag.retrieval;

import com.localrag.common.dto.SearchResultItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReContext-inspired grounded evidence selection for the /api/answer prompt.
 *
 * The packer never touches search ranking or the retrieved context block; it only decides which
 * citation-bearing snippets are replayed close to the question and in what order. Selection is
 * fully deterministic (lexical query-term overlap, stable retrieval-rank tie-break) so prompt
 * behavior can be benchmarked without an LLM in the loop.
 */
final class AnswerEvidencePacker {

    static final int DEFAULT_REPLAY_CHAR_BUDGET = 1600;
    static final int DEFAULT_MAX_REPLAY_EVIDENCE = 6;

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final int MIN_TOKEN_LENGTH = 2;
    private static final Set<String> STOPWORDS = Set.of(
            "the", "an", "and", "or", "of", "to", "in", "on", "for", "is", "are", "was", "were",
            "be", "been", "does", "do", "did", "how", "what", "which", "when", "where", "who",
            "why", "it", "its", "as", "at", "by", "with", "that", "this", "these", "those"
    );

    private AnswerEvidencePacker() {
    }

    record Config(int replayCharBudget, int maxReplayEvidence) {
        static Config defaults() {
            return new Config(DEFAULT_REPLAY_CHAR_BUDGET, DEFAULT_MAX_REPLAY_EVIDENCE);
        }
    }

    record Evidence(SearchResultItem item, double groundingScore, int retrievalRank) {
    }

    record Packed(
            List<Evidence> selected,
            int candidateCount,
            int replaySnippetChars,
            boolean lexicallyGrounded
    ) {
        Packed {
            selected = selected == null ? List.of() : List.copyOf(selected);
        }
    }

    /**
     * Query-aware, budget-bounded selection. Falls back to deduplicated retrieval order when no
     * candidate has lexical overlap with the query, so the replay block is never worse than the
     * previous replay-in-retrieval-order behavior for the leading evidence.
     */
    static Packed pack(String query, List<SearchResultItem> results, Config config) {
        List<Evidence> candidates = dedupedCandidates(results);
        Set<String> queryTokens = tokens(query);
        boolean grounded = false;
        List<Evidence> scored = new ArrayList<>(candidates.size());
        for (Evidence candidate : candidates) {
            double score = queryOverlapScore(queryTokens, candidate.item().snippet());
            if (score > 0.0) {
                grounded = true;
            }
            scored.add(new Evidence(candidate.item(), score, candidate.retrievalRank()));
        }
        if (grounded) {
            // The full retrieved context block still carries every result, so the replay block can
            // safely drop candidates with no lexical grounding instead of re-emphasizing them.
            scored.removeIf(candidate -> candidate.groundingScore() <= 0.0);
            scored.sort((left, right) -> {
                int byScore = Double.compare(right.groundingScore(), left.groundingScore());
                if (byScore != 0) {
                    return byScore;
                }
                return Integer.compare(left.retrievalRank(), right.retrievalRank());
            });
        }
        List<Evidence> selected = new ArrayList<>();
        int usedChars = 0;
        for (Evidence candidate : scored) {
            if (selected.size() >= config.maxReplayEvidence()) {
                break;
            }
            int snippetChars = candidate.item().snippet().trim().length();
            if (!selected.isEmpty() && usedChars + snippetChars > config.replayCharBudget()) {
                continue;
            }
            selected.add(candidate);
            usedChars += snippetChars;
        }
        return new Packed(selected, candidates.size(), usedChars, grounded);
    }

    /**
     * The pre-T0026 replay behavior: deduplicated snippets in retrieval order without query
     * awareness or budget. Kept as the reproducible baseline arm for the grounding benchmark.
     */
    static Packed baselinePack(List<SearchResultItem> results) {
        List<Evidence> candidates = dedupedCandidates(results);
        int usedChars = candidates.stream()
                .mapToInt(candidate -> candidate.item().snippet().trim().length())
                .sum();
        return new Packed(candidates, candidates.size(), usedChars, false);
    }

    private static List<Evidence> dedupedCandidates(List<SearchResultItem> results) {
        if (results == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<Evidence> candidates = new ArrayList<>();
        for (int rank = 0; rank < results.size(); rank++) {
            SearchResultItem item = results.get(rank);
            if (item == null || item.snippet() == null || item.snippet().isBlank()) {
                continue;
            }
            if (!seen.add(evidenceKey(item))) {
                continue;
            }
            candidates.add(new Evidence(item, 0.0, rank));
        }
        return candidates;
    }

    static String evidenceKey(SearchResultItem result) {
        String snippet = result.snippet() == null ? "" : result.snippet().replaceAll("\\s+", " ").trim();
        return result.citation() + "\n" + snippet;
    }

    /**
     * Fraction of distinct meaningful query tokens found in the snippet. Token containment (not
     * exact word match) keeps the score usable for agglutinative Korean text where query terms
     * appear inside longer tokens.
     */
    private static double queryOverlapScore(Set<String> queryTokens, String snippet) {
        if (queryTokens.isEmpty() || snippet == null || snippet.isBlank()) {
            return 0.0;
        }
        String normalizedSnippet = snippet.toLowerCase(Locale.ROOT);
        int matched = 0;
        for (String token : queryTokens) {
            if (normalizedSnippet.contains(token)) {
                matched++;
            }
        }
        return (double) matched / queryTokens.size();
    }

    private static Set<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= MIN_TOKEN_LENGTH && !STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    static Map<String, Object> packingStats(Packed packed) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("replayCandidateCount", packed.candidateCount());
        stats.put("replaySelectedCount", packed.selected().size());
        stats.put("replaySnippetChars", packed.replaySnippetChars());
        stats.put("replayLexicallyGrounded", packed.lexicallyGrounded());
        return stats;
    }
}
