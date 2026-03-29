package com.ast.back.domain.plagiarism;

public record AcSimilarityResult(
        int leftNodes,
        int rightNodes,
        int matchedNodes,
        double ac
) {
}
