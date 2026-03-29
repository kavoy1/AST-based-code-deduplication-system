package com.ast.back.domain.plagiarism;

import java.util.Map;

public record AstSignatureProfile(
        int totalNodes,
        Map<String, Integer> signatureCounts
) {
}
