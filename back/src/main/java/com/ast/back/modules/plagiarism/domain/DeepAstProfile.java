package com.ast.back.modules.plagiarism.domain;

import java.util.List;
import java.util.Map;

public record DeepAstProfile(
        int coarseTotalNodes,
        Map<String, Integer> coarseSignatureCounts,
        int deepTotalNodes,
        Map<String, Integer> deepSignatureCounts,
        List<DeepAstMethodProfile> methodProfiles
) {

    public AstSignatureProfile coarseProfile() {
        return new AstSignatureProfile(coarseTotalNodes, coarseSignatureCounts);
    }

    public AstSignatureProfile deepProfile() {
        return new AstSignatureProfile(deepTotalNodes, deepSignatureCounts);
    }
}
