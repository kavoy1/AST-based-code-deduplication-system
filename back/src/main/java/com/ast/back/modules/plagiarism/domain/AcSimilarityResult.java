package com.ast.back.modules.plagiarism.domain;

public record AcSimilarityResult(
        int leftNodes,
        int rightNodes,
        int matchedNodes,
        double ac
) {
}
