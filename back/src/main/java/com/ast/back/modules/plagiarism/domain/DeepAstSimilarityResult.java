package com.ast.back.modules.plagiarism.domain;

import java.util.List;

public record DeepAstSimilarityResult(
        int matchedNodes,
        double coarseScore,
        double deepStructuralScore,
        double methodScore,
        double finalScore,
        List<DeepAstMethodMatch> methodMatches
) {
}
