package com.ast.back.modules.plagiarism.domain;

import com.ast.back.modules.plagiarism.dto.TeacherCodeSegmentView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TeacherComparableCodeSegmentLocator {

    private static final String FAST_MODE = "FAST";
    private static final String DEEP_MODE = "DEEP";

    private final AcSimilarityCalculator similarityCalculator = new AcSimilarityCalculator();

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
        List<TeacherComparableCodeBlock> leftMethods = blocksOfKind(leftBlocks, "METHOD");
        List<TeacherComparableCodeBlock> rightMethods = blocksOfKind(rightBlocks, "METHOD");
        List<MethodPairCandidate> methodPairs = selectMethodPairs(leftMethods, rightMethods, defaultScore);
        if (methodPairs.isEmpty()) {
            return List.of();
        }

        List<BlockMatchCandidate> allCandidates = new ArrayList<>();
        Set<String> methodPairsWithInnerMatches = new LinkedHashSet<>();
        for (MethodPairCandidate methodPair : methodPairs) {
            List<BlockMatchCandidate> innerCandidates = buildInnerBlockCandidates(
                    leftBlocks,
                    rightBlocks,
                    methodPair,
                    defaultScore
            );
            if (!innerCandidates.isEmpty()) {
                methodPairsWithInnerMatches.add(methodPair.pairKey());
                allCandidates.addAll(innerCandidates);
            }
        }

        for (MethodPairCandidate methodPair : methodPairs) {
            if (methodPairsWithInnerMatches.contains(methodPair.pairKey())) {
                continue;
            }
            allCandidates.add(new BlockMatchCandidate(
                    blockKey(methodPair.leftMethod()),
                    blockKey(methodPair.rightMethod()),
                    methodPair.segment(),
                    segmentSpan(methodPair.leftMethod(), methodPair.rightMethod()),
                    methodPair.segment().score(),
                    methodPair.pairKey()
            ));
        }

        allCandidates.sort(Comparator
                .comparingInt(BlockMatchCandidate::score).reversed()
                .thenComparingInt(BlockMatchCandidate::span).reversed()
                .thenComparing(candidate -> candidate.segment().leftStartLine())
                .thenComparing(candidate -> candidate.segment().rightStartLine()));

        Set<String> usedLeft = new LinkedHashSet<>();
        Set<String> usedRight = new LinkedHashSet<>();
        List<TeacherCodeSegmentView> chosen = new ArrayList<>();
        for (BlockMatchCandidate candidate : allCandidates) {
            if (!usedLeft.add(candidate.leftKey()) || !usedRight.add(candidate.rightKey())) {
                continue;
            }
            chosen.add(candidate.segment());
        }
        return chosen;
    }

    private List<BlockMatchCandidate> buildInnerBlockCandidates(
            List<TeacherComparableCodeBlock> leftBlocks,
            List<TeacherComparableCodeBlock> rightBlocks,
            MethodPairCandidate methodPair,
            Integer defaultScore
    ) {
        List<TeacherComparableCodeBlock> leftInner = leftBlocks.stream()
                .filter(block -> !"METHOD".equals(block.kind()))
                .filter(block -> methodPair.leftMethod().methodKey().equals(block.methodKey()))
                .toList();
        List<TeacherComparableCodeBlock> rightInner = rightBlocks.stream()
                .filter(block -> !"METHOD".equals(block.kind()))
                .filter(block -> methodPair.rightMethod().methodKey().equals(block.methodKey()))
                .toList();

        List<BlockMatchCandidate> candidates = new ArrayList<>();
        for (TeacherComparableCodeBlock left : leftInner) {
            for (TeacherComparableCodeBlock right : rightInner) {
                if (!isComparableFamily(left, right)) {
                    continue;
                }
                double similarity = scoreDeepPair(left, right);
                if (similarity < 0.63d) {
                    continue;
                }
                int score = resolveDeepScore(left, right, defaultScore, similarity);
                candidates.add(new BlockMatchCandidate(
                        blockKey(left),
                        blockKey(right),
                        buildSegment(left, right, buildSegmentSummary(left, right), score),
                        segmentSpan(left, right),
                        score,
                        methodPair.pairKey()
                ));
            }
        }

        candidates.sort(Comparator
                .comparingInt(BlockMatchCandidate::score).reversed()
                .thenComparingInt(BlockMatchCandidate::span).reversed());
        return filterContainedCandidates(candidates);
    }

    private List<MethodPairCandidate> selectMethodPairs(
            List<TeacherComparableCodeBlock> leftMethods,
            List<TeacherComparableCodeBlock> rightMethods,
            Integer defaultScore
    ) {
        List<MethodPairCandidate> candidates = new ArrayList<>();
        for (TeacherComparableCodeBlock left : leftMethods) {
            for (TeacherComparableCodeBlock right : rightMethods) {
                double similarity = scoreDeepPair(left, right);
                if (similarity < 0.58d) {
                    continue;
                }
                int score = resolveDeepScore(left, right, defaultScore, similarity);
                TeacherCodeSegmentView segment = buildSegment(left, right, buildSegmentSummary(left, right), score);
                candidates.add(new MethodPairCandidate(left, right, segment, pairKey(left, right), similarity));
            }
        }

        candidates.sort(Comparator
                .comparingDouble(MethodPairCandidate::similarity).reversed()
                .thenComparingInt(candidate -> segmentSpan(candidate.leftMethod(), candidate.rightMethod())).reversed());

        Set<String> usedLeft = new LinkedHashSet<>();
        Set<String> usedRight = new LinkedHashSet<>();
        List<MethodPairCandidate> chosen = new ArrayList<>();
        for (MethodPairCandidate candidate : candidates) {
            if (!usedLeft.add(blockKey(candidate.leftMethod())) || !usedRight.add(blockKey(candidate.rightMethod()))) {
                continue;
            }
            chosen.add(candidate);
        }
        return chosen;
    }

    private List<BlockMatchCandidate> filterContainedCandidates(List<BlockMatchCandidate> candidates) {
        List<BlockMatchCandidate> filtered = new ArrayList<>();
        for (BlockMatchCandidate candidate : candidates) {
            boolean covered = filtered.stream().anyMatch(existing ->
                    existing.methodPairKey().equals(candidate.methodPairKey())
                            && sameRange(existing.segment(), candidate.segment())
                            && existing.score() >= candidate.score());
            if (!covered) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    private List<TeacherComparableCodeBlock> blocksOfKind(List<TeacherComparableCodeBlock> blocks, String kind) {
        return blocks.stream().filter(block -> kind.equals(block.kind())).toList();
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
                left.kind(),
                left.methodKey(),
                right.methodKey(),
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
            case "IF_THEN", "IF_ELSE", "ELSE_IF" -> "IF";
            case "METHOD" -> "METHOD";
            case "TRY" -> "TRY";
            case "CATCH" -> "CATCH";
            case "SWITCH" -> "SWITCH";
            case "CASE_GROUP" -> "CASE_GROUP";
            default -> kind;
        };
    }

    private double scoreDeepPair(TeacherComparableCodeBlock left, TeacherComparableCodeBlock right) {
        double structuralScore = calculateStructuralScore(left, right);
        double statementSimilarity = ratioSimilarity(left.statementCount(), right.statementCount());
        double spanSimilarity = ratioSimilarity(segmentSpan(left), segmentSpan(right));
        double depthSimilarity = ratioSimilarity(left.relativeDepth() + 1, right.relativeDepth() + 1);
        double familyBonus = left.kind().equals(right.kind()) ? 0.05d : 0.02d;
        double score = structuralScore * 0.72d
                + statementSimilarity * 0.12d
                + spanSimilarity * 0.08d
                + depthSimilarity * 0.08d
                + familyBonus;
        return Math.min(0.99d, score);
    }

    private double calculateStructuralScore(TeacherComparableCodeBlock left, TeacherComparableCodeBlock right) {
        AstSignatureProfile leftProfile = new AstSignatureProfile(left.structuralNodeTotal(), left.structuralSignatureCounts());
        AstSignatureProfile rightProfile = new AstSignatureProfile(right.structuralNodeTotal(), right.structuralSignatureCounts());
        return similarityCalculator.calculate(leftProfile, rightProfile).ac();
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
        int baseline = defaultScore != null && defaultScore > 0 ? Math.max(78, defaultScore - 4) : 78;
        int weighted = (int) Math.round(Math.max(baseline, similarity * 100));
        int spanBonus = Math.min(6, Math.max(left.statementCount(), right.statementCount()));
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
        return sameRange(existing, candidate);
    }

    private boolean sameRange(TeacherCodeSegmentView existing, TeacherCodeSegmentView candidate) {
        return existing.leftFile().equals(candidate.leftFile())
                && existing.rightFile().equals(candidate.rightFile())
                && existing.leftStartLine().equals(candidate.leftStartLine())
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
                    segment.blockType(),
                    segment.leftMethodKey(),
                    segment.rightMethodKey(),
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

    private String blockKey(TeacherComparableCodeBlock block) {
        return String.join(
                "::",
                block.kind(),
                String.valueOf(block.fullStartLine()),
                String.valueOf(block.fullEndLine()),
                String.valueOf(block.methodKey())
        );
    }

    private String pairKey(TeacherComparableCodeBlock left, TeacherComparableCodeBlock right) {
        return blockKey(left) + "||" + blockKey(right);
    }

    private record MethodPairCandidate(
            TeacherComparableCodeBlock leftMethod,
            TeacherComparableCodeBlock rightMethod,
            TeacherCodeSegmentView segment,
            String pairKey,
            double similarity
    ) {
    }

    private record BlockMatchCandidate(
            String leftKey,
            String rightKey,
            TeacherCodeSegmentView segment,
            int span,
            int score,
            String methodPairKey
    ) {
    }
}
