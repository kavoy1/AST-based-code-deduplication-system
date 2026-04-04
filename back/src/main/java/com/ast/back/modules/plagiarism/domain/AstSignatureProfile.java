package com.ast.back.modules.plagiarism.domain;

import java.util.Map;

public record AstSignatureProfile(
        int totalNodes,
        Map<String, Integer> signatureCounts
) {
}
