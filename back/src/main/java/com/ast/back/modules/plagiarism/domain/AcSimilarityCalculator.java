package com.ast.back.modules.plagiarism.domain;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AcSimilarityCalculator {

    private static final double COUNT_OVERLAP_WEIGHT = 0.5;
    private static final double DISTINCT_OVERLAP_WEIGHT = 0.2;
    private static final double DISTRIBUTION_OVERLAP_WEIGHT = 0.3;

    public AcSimilarityResult calculate(AstSignatureProfile left, AstSignatureProfile right) {
        int matched = 0;
        Set<String> signatures = new HashSet<>();
        signatures.addAll(left.signatureCounts().keySet());
        signatures.addAll(right.signatureCounts().keySet());
        for (String signature : signatures) {
            int leftCount = left.signatureCounts().getOrDefault(signature, 0);
            int rightCount = right.signatureCounts().getOrDefault(signature, 0);
            matched += Math.min(leftCount, rightCount);
        }

        int denominator = left.totalNodes() + right.totalNodes();
        double countOverlap = denominator == 0 ? 0.0 : (2.0 * matched) / denominator;
        double distinctOverlap = calculateDistinctOverlap(left.signatureCounts(), right.signatureCounts(), signatures);
        double distributionOverlap = calculateDistributionOverlap(left.signatureCounts(), right.signatureCounts(), signatures);
        double ac = (countOverlap * COUNT_OVERLAP_WEIGHT)
                + (distinctOverlap * DISTINCT_OVERLAP_WEIGHT)
                + (distributionOverlap * DISTRIBUTION_OVERLAP_WEIGHT);
        return new AcSimilarityResult(left.totalNodes(), right.totalNodes(), matched, ac);
    }

    private double calculateDistinctOverlap(
            Map<String, Integer> leftCounts,
            Map<String, Integer> rightCounts,
            Set<String> signatures
    ) {
        if (signatures.isEmpty()) {
            return 0.0;
        }
        long intersection = signatures.stream()
                .filter(signature -> leftCounts.containsKey(signature) && rightCounts.containsKey(signature))
                .count();
        return ((double) intersection) / signatures.size();
    }

    private double calculateDistributionOverlap(
            Map<String, Integer> leftCounts,
            Map<String, Integer> rightCounts,
            Set<String> signatures
    ) {
        if (signatures.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (String signature : signatures) {
            int leftCount = leftCounts.getOrDefault(signature, 0);
            int rightCount = rightCounts.getOrDefault(signature, 0);
            int maxCount = Math.max(leftCount, rightCount);
            if (maxCount == 0) {
                continue;
            }
            total += ((double) Math.min(leftCount, rightCount)) / maxCount;
        }
        return total / signatures.size();
    }
}
