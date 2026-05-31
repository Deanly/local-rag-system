package com.localrag.retrieval;

import com.localrag.common.dto.SearchRequest;
import com.localrag.common.dto.SearchResultItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RetrievalRanker {
    private static final int TOP_WINDOW_FOR_DOCUMENT_CAP = 5;
    private static final int MAX_CHUNKS_PER_DOCUMENT_IN_TOP_WINDOW = 2;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("T\\d{4}", Pattern.CASE_INSENSITIVE);

    private RetrievalRanker() {
    }

    static List<SearchResultItem> rank(
            SearchRequest request,
            RankContext context,
            List<RankCandidate> candidates,
            int limit
    ) {
        if (candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        Set<String> queryTerms = queryTerms(request.query());
        List<WeightedCandidate> weighted = candidates.stream()
                .map(candidate -> weigh(request, context, queryTerms, candidate, candidates.size()))
                .sorted(Comparator.comparingDouble(WeightedCandidate::rerankScore).reversed()
                        .thenComparingInt(weightedCandidate -> weightedCandidate.candidate().rawRank()))
                .toList();
        List<WeightedCandidate> selected = diversify(weighted, limit);
        int finalResultCount = selected.size();
        return selected.stream()
                .map(weightedCandidate -> weightedCandidate.toSearchResultItem(candidates.size(), finalResultCount))
                .toList();
    }

    private static WeightedCandidate weigh(
            SearchRequest request,
            RankContext context,
            Set<String> queryTerms,
            RankCandidate candidate,
            int rawCandidateCount
    ) {
        SearchResultItem item = candidate.item();
        double baseScore = baseScore(item.score());
        double sourceWeight = sourceWeight(request, context, item);
        double pathWeight = pathWeight(request.query(), item);
        double matchWeight = matchWeight(queryTerms, item);
        double governanceWeight = governanceWeight(request, item);
        double rawRankTieBreak = Math.max(0.0, 0.02 - (candidate.rawRank() * 0.0005));
        double rerankScore = baseScore + sourceWeight + pathWeight + matchWeight + governanceWeight + rawRankTieBreak;
        return new WeightedCandidate(
                candidate,
                rerankScore,
                baseScore,
                sourceWeight,
                pathWeight,
                matchWeight,
                governanceWeight,
                rawCandidateCount
        );
    }

    private static double baseScore(Map<String, Object> score) {
        if (score == null) {
            return 0.0;
        }
        Object rawScore = score.get("score");
        if (rawScore instanceof Number number) {
            return number.doubleValue();
        }
        if (rawScore != null) {
            try {
                return Double.parseDouble(String.valueOf(rawScore));
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        Object distance = score.get("distance");
        if (distance instanceof Number number) {
            return 1.0 / (1.0 + Math.max(0.0, number.doubleValue()));
        }
        return 0.0;
    }

    private static double sourceWeight(SearchRequest request, RankContext context, SearchResultItem item) {
        SourceInfo source = context.sources().get(item.sourceId());
        double weight = 0.0;
        boolean primary = item.sourceId().equals(context.primarySourceId());
        boolean defaultContext = context.defaultContextSourceIds().contains(item.sourceId());
        if (primary) {
            weight += 0.50;
        } else if (request.projectId() != null && source != null && request.projectId().equals(source.projectId())) {
            weight += 0.18;
        }
        if (defaultContext && !primary) {
            weight -= 0.30;
        }
        if (source != null) {
            weight += Math.max(-0.05, Math.min(0.12, (source.priority() - 80) / 500.0));
            if ("project-current-truth".equalsIgnoreCase(source.ssotRole())) {
                weight += 0.12;
            } else if ("cross-project-support".equalsIgnoreCase(source.ssotRole())) {
                weight -= 0.10;
            }
            if (defaultContext && "compiled-wiki".equalsIgnoreCase(source.sourceType())) {
                weight -= 0.08;
            }
        }
        if (item.relativePath().startsWith("lint/") && defaultContext && !primary) {
            weight -= 0.20;
        }
        return weight;
    }

    private static double pathWeight(String query, SearchResultItem item) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        String path = item.relativePath().toLowerCase(Locale.ROOT);
        double weight = 0.0;

        if (hasAny(lowerQuery, "design", "architecture", "설계", "아키텍처") && path.startsWith("design/")) {
            weight += 0.20;
        }
        if (hasAny(lowerQuery, "retrieval quality", "검색 품질", "품질", "evaluation", "평가", "harness", "하네스", "rerank")
                && path.contains("retrieval-quality")) {
            weight += 0.14;
        }
        if (hasAny(lowerQuery, "task", "hardening", "t000", "작업", "강화") && path.startsWith("tasks/")) {
            weight += 0.18;
        }
        if (hasAny(lowerQuery, "report", "review", "investigation", "보고서", "리포트", "조사") && path.startsWith("reports/")) {
            weight += 0.18;
        }
        if (hasAny(lowerQuery, "guide", "spec", "스펙", "가이드") && path.startsWith("guide/")) {
            weight += 0.14;
        }
        if (hasAny(lowerQuery, "project", "product", "business", "프로젝트", "제품", "상품") && path.startsWith("projects/")) {
            weight += 0.10;
        }

        Matcher matcher = TASK_ID_PATTERN.matcher(query);
        while (matcher.find()) {
            if (path.contains(matcher.group().toLowerCase(Locale.ROOT))) {
                weight += 0.40;
            }
        }
        return weight;
    }

    private static double matchWeight(Set<String> queryTerms, SearchResultItem item) {
        Set<String> documentTerms = tokenize(item.relativePath() + " " + item.headingPath());
        int overlap = 0;
        for (String term : queryTerms) {
            if (documentTerms.contains(term)) {
                overlap++;
            }
        }
        return Math.min(0.35, overlap * 0.05);
    }

    private static double governanceWeight(SearchRequest request, SearchResultItem item) {
        Map<String, Object> metadata = item.metadata() == null ? Map.of() : item.metadata();
        String status = metadataText(metadata, "frontmatterStatus");
        String authority = metadataText(metadata, "authority");
        boolean superseded = !metadataList(metadata, "supersededBy").isEmpty();
        boolean historicalAllowed = historicalAllowed(request);

        double weight = 0.0;
        if ("canonical".equals(authority)) {
            weight += 0.22;
        } else if ("accepted".equals(authority)) {
            weight += 0.16;
        } else if ("reference".equals(authority)) {
            weight -= historicalAllowed ? 0.0 : 0.08;
        } else if ("raw".equals(authority)) {
            weight -= historicalAllowed ? 0.0 : 0.30;
        }

        if ("current".equals(status) || "active".equals(status)) {
            weight += 0.14;
        } else if ("done".equals(status) || "accepted".equals(status)) {
            weight += 0.08;
        } else if ("draft".equals(status)) {
            weight -= historicalAllowed ? 0.0 : 0.12;
        }

        if (!historicalAllowed) {
            if ("deprecated".equals(status)) {
                weight -= 1.25;
            }
            if ("superseded".equals(status) || superseded) {
                weight -= 1.10;
            }
        }
        return weight;
    }

    private static boolean historicalAllowed(SearchRequest request) {
        if (request == null) {
            return false;
        }
        if (truthyFilter(request.filters(), "includeHistorical") || truthyFilter(request.filters(), "historical")) {
            return true;
        }
        String query = request.query() == null ? "" : request.query().toLowerCase(Locale.ROOT);
        return hasAny(query, "historical", "history", "archive", "deprecated", "superseded", "migration", "이력", "과거", "폐기", "대체", "마이그레이션");
    }

    private static boolean truthyFilter(Map<String, List<String>> filters, String key) {
        if (filters == null || !filters.containsKey(key)) {
            return false;
        }
        return filters.get(key).stream()
                .filter(value -> value != null)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(value -> Set.of("true", "yes", "1", "include", "on").contains(value));
    }

    private static String metadataText(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> metadataList(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return List.of();
        }
        return List.of(String.valueOf(value));
    }

    private static boolean hasAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> queryTerms(String query) {
        Set<String> terms = tokenize(query);
        String lower = query.toLowerCase(Locale.ROOT);
        addKoreanAliases(lower, terms);
        return terms;
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() > 1) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static void addKoreanAliases(String query, Set<String> terms) {
        Map<String, String[]> aliases = new HashMap<>();
        aliases.put("텔레그램", new String[]{"telegram"});
        aliases.put("워크룸", new String[]{"workroom"});
        aliases.put("아키텍처", new String[]{"architecture"});
        aliases.put("채널", new String[]{"channel"});
        aliases.put("설계", new String[]{"design"});
        aliases.put("크립토", new String[]{"crypto"});
        aliases.put("봇", new String[]{"bot"});
        aliases.put("사실", new String[]{"fact"});
        aliases.put("권한", new String[]{"authority"});
        aliases.put("리서치", new String[]{"research"});
        aliases.put("표면", new String[]{"surface"});
        aliases.put("지갑", new String[]{"wallet"});
        aliases.put("라이프사이클", new String[]{"lifecycle"});
        aliases.put("조사", new String[]{"investigation", "report"});
        aliases.put("보고서", new String[]{"report"});
        aliases.put("리포트", new String[]{"report"});
        aliases.put("유료", new String[]{"paid"});
        aliases.put("상품", new String[]{"product"});
        aliases.put("기준", new String[]{"threshold"});
        aliases.put("임계값", new String[]{"threshold"});
        aliases.put("백엔드", new String[]{"backend"});
        aliases.put("로컬", new String[]{"local"});
        aliases.put("그래들", new String[]{"gradle"});
        aliases.put("스프링", new String[]{"spring"});
        aliases.put("스켈레톤", new String[]{"skeleton"});
        aliases.put("소스", new String[]{"source"});
        aliases.put("레지스트리", new String[]{"registry"});
        aliases.put("쓰기", new String[]{"write"});
        aliases.put("정책", new String[]{"policy"});
        aliases.put("원격", new String[]{"remote"});
        aliases.put("브릿지", new String[]{"bridge"});
        aliases.put("에이전트", new String[]{"agent"});
        aliases.put("스펙", new String[]{"spec"});
        aliases.put("데일리", new String[]{"daily"});
        aliases.put("운영", new String[]{"operating", "operations"});
        aliases.put("모델", new String[]{"model"});
        aliases.put("코덱스", new String[]{"codex"});
        aliases.put("강화", new String[]{"hardening"});
        aliases.put("에러", new String[]{"error"});
        aliases.put("전략", new String[]{"strategy"});
        aliases.put("노트", new String[]{"notes"});
        aliases.put("검색", new String[]{"search", "retrieval"});
        aliases.put("품질", new String[]{"quality"});
        aliases.put("평가", new String[]{"evaluation"});
        aliases.put("하네스", new String[]{"harness"});

        aliases.forEach((korean, mappedTerms) -> {
            if (query.contains(korean)) {
                terms.addAll(List.of(mappedTerms));
            }
        });
    }

    private static List<WeightedCandidate> diversify(List<WeightedCandidate> weighted, int limit) {
        List<WeightedCandidate> selected = new ArrayList<>();
        Set<String> seenContent = new HashSet<>();
        Map<String, Integer> documentCounts = new HashMap<>();
        int topWindowLimit = Math.min(TOP_WINDOW_FOR_DOCUMENT_CAP, limit);

        for (WeightedCandidate candidate : weighted) {
            if (selected.size() >= limit) {
                break;
            }
            if (isDuplicateContent(candidate, seenContent)) {
                continue;
            }
            int documentCount = documentCounts.getOrDefault(candidate.documentKey(), 0);
            if (selected.size() < topWindowLimit && documentCount >= MAX_CHUNKS_PER_DOCUMENT_IN_TOP_WINDOW) {
                continue;
            }
            selected.add(candidate);
            seenContent.add(candidate.contentKey());
            documentCounts.merge(candidate.documentKey(), 1, Integer::sum);
        }

        if (selected.size() < limit && selected.size() >= topWindowLimit) {
            for (WeightedCandidate candidate : weighted) {
                if (selected.size() >= limit) {
                    break;
                }
                if (selected.contains(candidate) || isDuplicateContent(candidate, seenContent)) {
                    continue;
                }
                selected.add(candidate);
                seenContent.add(candidate.contentKey());
            }
        }
        return selected;
    }

    private static boolean isDuplicateContent(WeightedCandidate candidate, Set<String> seenContent) {
        return seenContent.contains(candidate.contentKey());
    }

    record RankContext(
            String projectId,
            String primarySourceId,
            Set<String> defaultContextSourceIds,
            Map<String, SourceInfo> sources
    ) {
        RankContext {
            defaultContextSourceIds = defaultContextSourceIds == null ? Set.of() : Set.copyOf(defaultContextSourceIds);
            sources = sources == null ? Map.of() : Map.copyOf(sources);
        }
    }

    record SourceInfo(String sourceId, String projectId, String sourceType, String ssotRole, int priority) {
    }

    record RankCandidate(SearchResultItem item, String contentHash, int rawRank) {
    }

    private record WeightedCandidate(
            RankCandidate candidate,
            double rerankScore,
            double baseScore,
            double sourceWeight,
            double pathWeight,
            double matchWeight,
            double governanceWeight,
            int rawCandidateCount
    ) {
        String documentKey() {
            String documentId = candidate.item().documentId();
            return documentId == null || documentId.isBlank()
                    ? candidate.item().sourceId() + ":" + candidate.item().relativePath()
                    : documentId;
        }

        String contentKey() {
            String contentHash = candidate.contentHash();
            return contentHash == null || contentHash.isBlank()
                    ? candidate.item().chunkId()
                    : contentHash;
        }

        SearchResultItem toSearchResultItem(int rawCandidateCount, int finalResultCount) {
            SearchResultItem item = candidate.item();
            Map<String, Object> score = new LinkedHashMap<>();
            if (item.score() != null) {
                score.putAll(item.score());
            }
            score.put("baseScore", baseScore);
            score.put("sourceWeight", sourceWeight);
            score.put("pathWeight", pathWeight);
            score.put("matchWeight", matchWeight);
            score.put("governanceWeight", governanceWeight);
            score.put("rerankScore", rerankScore);
            score.put("rawRank", candidate.rawRank());
            score.put("rawCandidateCount", rawCandidateCount);
            score.put("finalResultCount", finalResultCount);
            return new SearchResultItem(
                    item.chunkId(),
                    item.documentId(),
                    item.projectId(),
                    item.sourceId(),
                    item.ssotRole(),
                    item.relativePath(),
                    item.headingPath(),
                    item.citation(),
                    item.snippet(),
                    item.metadata(),
                    score
            );
        }
    }
}
