package com.ast.back.domain.plagiarism;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AcSimilarityCalculator {

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
        double ac = denominator == 0 ? 0.0 : (2.0 * matched) / denominator;
        return new AcSimilarityResult(left.totalNodes(), right.totalNodes(), matched, ac);
    }
}
