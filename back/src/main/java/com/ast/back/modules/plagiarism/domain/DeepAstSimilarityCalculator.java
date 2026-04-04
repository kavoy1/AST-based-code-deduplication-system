package com.ast.back.modules.plagiarism.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DeepAstSimilarityCalculator {

    private static final double COARSE_WEIGHT = 0.35;
    private static final double DEEP_WEIGHT = 0.45;
    private static final double METHOD_WEIGHT = 0.20;

    private final AcSimilarityCalculator calculator = new AcSimilarityCalculator();

    public DeepAstSimilarityResult calculate(DeepAstProfile left, DeepAstProfile right) {
        AcSimilarityResult coarseResult = calculator.calculate(left.coarseProfile(), right.coarseProfile());
        AcSimilarityResult deepResult = calculator.calculate(left.deepProfile(), right.deepProfile());

        List<DeepAstMethodMatch> methodMatches = buildMethodMatches(left.methodProfiles(), right.methodProfiles());
        double methodScore = methodMatches.isEmpty()
                ? 0.0
                : methodMatches.stream().mapToDouble(DeepAstMethodMatch::score).average().orElse(0.0);

        double finalScore = (coarseResult.ac() * COARSE_WEIGHT)
                + (deepResult.ac() * DEEP_WEIGHT)
                + (methodScore * METHOD_WEIGHT);

        return new DeepAstSimilarityResult(
                deepResult.matchedNodes(),
                coarseResult.ac(),
                deepResult.ac(),
                methodScore,
                finalScore,
                methodMatches
        );
    }

    private List<DeepAstMethodMatch> buildMethodMatches(List<DeepAstMethodProfile> leftMethods, List<DeepAstMethodProfile> rightMethods) {
        List<DeepAstMethodMatch> matches = new ArrayList<>();
        List<DeepAstMethodProfile> remaining = new ArrayList<>(rightMethods);
        for (DeepAstMethodProfile leftMethod : leftMethods) {
            DeepAstMethodProfile bestMethod = null;
            double bestScore = 0.0;
            for (DeepAstMethodProfile candidate : remaining) {
                AcSimilarityResult result = calculator.calculate(
                        new AstSignatureProfile(leftMethod.totalNodes(), leftMethod.signatureCounts()),
                        new AstSignatureProfile(candidate.totalNodes(), candidate.signatureCounts())
                );
                if (result.ac() > bestScore) {
                    bestScore = result.ac();
                    bestMethod = candidate;
                }
            }
            if (bestMethod != null) {
                remaining.remove(bestMethod);
                matches.add(new DeepAstMethodMatch(leftMethod.methodKey(), bestMethod.methodKey(), bestScore));
            }
        }
        matches.sort(Comparator.comparingDouble(DeepAstMethodMatch::score).reversed());
        return matches;
    }
}
