package com.ast.back.modules.plagiarism.domain;

public record DeepAstMethodMatch(
        String leftMethod,
        String rightMethod,
        double score
) {
}
