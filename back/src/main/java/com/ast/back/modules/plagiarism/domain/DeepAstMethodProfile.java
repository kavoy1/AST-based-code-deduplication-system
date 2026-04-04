package com.ast.back.modules.plagiarism.domain;

import java.util.Map;

public record DeepAstMethodProfile(
        String methodKey,
        int totalNodes,
        Map<String, Integer> signatureCounts
) {
}
