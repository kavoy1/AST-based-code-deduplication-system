package com.ast.back.modules.plagiarism.domain;

import com.ast.back.modules.plagiarism.dto.TeacherCodeSegmentView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TeacherComparableCodeSegmentLocator {

    private static final String FAST_MODE = "FAST";
    private static final String DEEP_MODE = "DEEP";

    public List<TeacherCodeSegmentView> locate(
            List<TeacherComparableCodeBlock> leftBlocks,
            List<TeacherComparableCodeBlock> rightBlocks,
            Integer defaultScore
    ) {
        return locate(leftBlocks, rightBlocks, defaultScore, FAST_MODE);
    }

    public List<TeacherCodeSegmentView> locate(
            List<TeacherComparableCodeBlock> leftBlocks,
            List<TeacherComparableCodeBlock> rightBlocks,
            Integer defaultScore,
            String plagiarismMode
    ) {
        if (leftBlocks == null || rightBlocks == null || leftBlocks.isEmpty() || rightBlocks.isEmpty()) {
            return List.of();
        }

        List<TeacherCodeSegmentView> candidates = DEEP_MODE.equalsIgnoreCase(plagiarismMode)
                ? locateDeep(leftBlocks, rightBlocks, defaultScore)
                : locateFast(leftBlocks, rightBlocks, defaultScore);
        if (candidates.isEmpty()) {
            return List.of();
        }

        candidates.sort(Comparator
                .comparingInt((TeacherCodeSegmentView segment) -> segmentSpan(segment)).reversed()
                .thenComparing(TeacherCodeSegmentView::leftStartLine)
                .thenComparing(TeacherCodeSegmentView::rightStartLine));

        List<TeacherCodeSegmentView> filtered = new ArrayList<>();
        for (TeacherCodeSegmentView candidate : candidates) {
            if (filtered.stream().anyMatch(existing -> contains(existing, candidate))) {
                continue;
            }
            filtered.add(candidate);
        }

        filtered.sort(Comparator
                .comparing(TeacherCodeSegmentView::leftStartLine)
                .thenComparing(TeacherCodeSegmentView::rightStartLine));
        return labelSegments(filtered);
    }

    private List<TeacherCodeSegmentView> locateFast(
            List<TeacherComparableCodeBlock> leftBlocks,
            List<TeacherComparableCodeBlock> rightBlocks,
            Integer defaultScore
    ) {
        Map<String, List<TeacherComparableCodeBlock>> leftBuckets = bucket(leftBlocks);
        Map<String, List<TeacherComparableCodeBlock>> rightBuckets = bucket(rightBlocks);
        List<TeacherCodeSegmentView> candidates = new ArrayList<>();

        for (Map.Entry<String, List<TeacherComparableCodeBlock>> entry : leftBuckets.entrySet()) {
            List<TeacherComparableCodeBlock> rightBucket = rightBuckets.get(entry.getKey());
            if (rightBucket == null || rightBucket.isEmpty()) {
                continue;
            }

            List<TeacherComparableCodeBlock> leftBucket = entry.getValue();
            leftBucket.sort(Comparator.comparing(TeacherComparableCodeBlock::fullStartLine));
            rightBucket.sort(Comparator.comparing(TeacherComparableCodeBlock::fullStartLine));

            int pairCount = Math.min(leftBucket.size(), rightBucket.size());
            for (int index = 0; index < pairCount; index++) {
                TeacherComparableCodeBlock left = leftBucket.get(index);
                TeacherComparableCodeBlock right = rightBucket.get(index);
                candidates.add(buildSegment(left, right, left.summary(), resolveSegmentScore(left, right, defaultScore)));
            }
        }
        return candidates;
    }

    private List<TeacherCodeSegmentView> locateDeep(
            List<TeacherComparableCodeBlock> leftBlocks,
            List<TeacherComparableCodeBlock> rightBlocks,
            Integer defaultScore
    ) {
        List<BlockMatchCandidate> candidates = new ArrayList<>();
        for (int leftIndex = 0; leftIndex < leftBlocks.size(); leftIndex++) {
            TeacherComparableCodeBlock left = leftBlocks.get(leftIndex);
            for (int rightIndex = 0; rightIndex < rightBlocks.size(); rightIndex++) {
                TeacherComparableCodeBlock right = rightBlocks.get(rightIndex);
                if (!isComparableFamily(left, right)) {
                    continue;
                }
                double similarity = scoreDeepPair(left, right);
                if (similarity < 0.64d) {
                    continue;
                }
                int score = resolveDeepScore(left, right, defaultScore, similarity);
                candidates.add(new BlockMatchCandidate(
                        leftIndex,
                        rightIndex,
                        buildSegment(left, right, buildSegmentSummary(left, right), score),
                        segmentSpan(left, right),
                        score,
                        "METHOD".equals(left.kind()) && "METHOD".equals(right.kind())
                ));
            }
        }

        candidates.sort(Comparator
                .comparingInt(BlockMatchCandidate::score).reversed()
                .thenComparingInt(BlockMatchCandidate::span).reversed()
                .thenComparing(candidate -> candidate.segment().leftStartLine())
                .thenComparing(candidate -> candidate.segment().rightStartLine()));

        Set<Integer> usedLeft = new LinkedHashSet<>();
        Set<Integer> usedRight = new LinkedHashSet<>();
        List<TeacherCodeSegmentView> chosen = new ArrayList<>();
        candidates.stream()
                .filter(BlockMatchCandidate::methodLevel)
                .findFirst()
                .ifPresent(candidate -> {
                    chosen.add(candidate.segment());
                    usedLeft.add(candidate.leftIndex());
                    usedRight.add(candidate.rightIndex());
                });
        for (BlockMatchCandidate candidate : candidates) {
            if (!usedLeft.add(candidate.leftIndex()) || !usedRight.add(candidate.rightIndex())) {
                continue;
            }
            chosen.add(candidate.segment());
        }
        return chosen;
    }

    private TeacherCodeSegmentView buildSegment(
            TeacherComparableCodeBlock left,
            TeacherComparableCodeBlock right,
            String summary,
            Integer score
    ) {
        return new TeacherCodeSegmentView(
                null,
                null,
                summary,
                score,
                left.fileName(),
                left.fullStartLine(),
                left.fullEndLine(),
                right.fileName(),
                right.fullStartLine(),
                right.fullEndLine()
        );
    }

    private Map<String, List<TeacherComparableCodeBlock>> bucket(List<TeacherComparableCodeBlock> blocks) {
        Map<String, List<TeacherComparableCodeBlock>> buckets = new LinkedHashMap<>();
        for (TeacherComparableCodeBlock block : blocks) {
            String key = block.kind() + "::" + block.normalizedSnippet();
            buckets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(block);
        }
        return buckets;
    }

    private String buildSegmentSummary(TeacherComparableCodeBlock left, TeacherComparableCodeBlock right) {
        String leftSummary = sanitizeSummary(left.summary());
        String rightSummary = sanitizeSummary(right.summary());
        if (leftSummary.equals(rightSummary)) {
            return leftSummary;
        }
        return leftSummary + " → " + rightSummary;
    }

    private String sanitizeSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "相似代码片段";
        }
        return summary
                .replace("鏂规硶", "方法")
                .replace("閫昏緫", "逻辑")
                .replace("寰幆", "循环")
                .replace("鍒嗘敮", "分支")
                .replace("寮傚父澶勭悊", "异常处理")
                .replace("鐩镐技浠ｇ爜鐗囨", "相似代码片段")
                .trim();
    }

    private boolean isComparableFamily(TeacherComparableCodeBlock left, TeacherComparableCodeBlock right) {
        return familyOf(left.kind()).equals(familyOf(right.kind()));
    }

    private String familyOf(String kind) {
        if (kind == null) {
            return "OTHER";
        }
        return switch (kind) {
            case "FOR", "FOREACH", "WHILE", "DO_WHILE" -> "LOOP";
            case "METHOD" -> "METHOD";
            case "TRY" -> "TRY";
            case "SWITCH" -> "SWITCH";
            default -> kind;
        };
    }

    private double scoreDeepPair(TeacherComparableCodeBlock left, TeacherComparableCodeBlock right) {
        double tokenSimilarity = tokenBagSimilarity(left.normalizedSnippet(), right.normalizedSnippet());
        double statementSimilarity = ratioSimilarity(left.statementCount(), right.statementCount());
        double spanSimilarity = ratioSimilarity(segmentSpan(left), segmentSpan(right));
        double familyBonus = left.kind().equals(right.kind()) ? 0.06d : 0.0d;
        return tokenSimilarity * 0.62d + statementSimilarity * 0.23d + spanSimilarity * 0.15d + familyBonus;
    }

    private double tokenBagSimilarity(String leftSnippet, String rightSnippet) {
        Map<String, Integer> leftTokens = tokenizeSnippet(leftSnippet);
        Map<String, Integer> rightTokens = tokenizeSnippet(rightSnippet);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }
        int shared = 0;
        int total = 0;
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(leftTokens.keySet());
        keys.addAll(rightTokens.keySet());
        for (String key : keys) {
            int leftCount = leftTokens.getOrDefault(key, 0);
            int rightCount = rightTokens.getOrDefault(key, 0);
            shared += Math.min(leftCount, rightCount);
            total += Math.max(leftCount, rightCount);
        }
        return total == 0 ? 0d : (double) shared / total;
    }

    private Map<String, Integer> tokenizeSnippet(String snippet) {
        Map<String, Integer> tokenCounts = new LinkedHashMap<>();
        if (snippet == null || snippet.isBlank()) {
            return tokenCounts;
        }
        for (String token : snippet.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+")) {
            if (token.isBlank()) {
                continue;
            }
            tokenCounts.merge(token, 1, Integer::sum);
        }
        return tokenCounts;
    }

    private double ratioSimilarity(Integer leftValue, Integer rightValue) {
        if (leftValue == null || rightValue == null || leftValue <= 0 || rightValue <= 0) {
            return 0d;
        }
        int min = Math.min(leftValue, rightValue);
        int max = Math.max(leftValue, rightValue);
        return max == 0 ? 0d : (double) min / max;
    }

    private Integer resolveSegmentScore(
            TeacherComparableCodeBlock left,
            TeacherComparableCodeBlock right,
            Integer defaultScore
    ) {
        if (defaultScore != null && defaultScore > 0) {
            return defaultScore;
        }
        int heuristic = 70 + Math.min(25, Math.max(left.statementCount(), right.statementCount()) * 4);
        return Math.min(99, heuristic);
    }

    private int resolveDeepScore(
            TeacherComparableCodeBlock left,
            TeacherComparableCodeBlock right,
            Integer defaultScore,
            double similarity
    ) {
        int baseline = defaultScore != null && defaultScore > 0 ? defaultScore : 78;
        int weighted = (int) Math.round(Math.max(baseline, similarity * 100));
        int spanBonus = Math.min(8, Math.max(left.statementCount(), right.statementCount()));
        return Math.min(99, weighted + spanBonus / 2);
    }

    private static int segmentSpan(TeacherCodeSegmentView segment) {
        return Math.max(
                Math.max(0, segment.leftEndLine() - segment.leftStartLine() + 1),
                Math.max(0, segment.rightEndLine() - segment.rightStartLine() + 1)
        );
    }

    private int segmentSpan(TeacherComparableCodeBlock block) {
        return Math.max(0, block.fullEndLine() - block.fullStartLine() + 1);
    }

    private int segmentSpan(TeacherComparableCodeBlock left, TeacherComparableCodeBlock right) {
        return Math.max(segmentSpan(left), segmentSpan(right));
    }

    private boolean contains(TeacherCodeSegmentView existing, TeacherCodeSegmentView candidate) {
        return existing.leftStartLine().equals(candidate.leftStartLine())
                && existing.leftEndLine().equals(candidate.leftEndLine())
                && existing.rightStartLine().equals(candidate.rightStartLine())
                && existing.rightEndLine().equals(candidate.rightEndLine());
    }

    private List<TeacherCodeSegmentView> labelSegments(List<TeacherCodeSegmentView> segments) {
        List<TeacherCodeSegmentView> labeled = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            TeacherCodeSegmentView segment = segments.get(index);
            labeled.add(new TeacherCodeSegmentView(
                    "seg-" + (index + 1),
                    "片段 " + (index + 1),
                    segment.summary(),
                    segment.score(),
                    segment.leftFile(),
                    segment.leftStartLine(),
                    segment.leftEndLine(),
                    segment.rightFile(),
                    segment.rightStartLine(),
                    segment.rightEndLine()
            ));
        }
        return labeled;
    }

    private record BlockMatchCandidate(
            int leftIndex,
            int rightIndex,
            TeacherCodeSegmentView segment,
            int span,
            int score,
            boolean methodLevel
    ) {
    }
}
