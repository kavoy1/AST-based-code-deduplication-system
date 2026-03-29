package com.ast.back.ai;

import com.ast.back.entity.SimilarityEvidence;
import com.ast.back.entity.SimilarityPair;

public record AiExplanationRequest(
        SimilarityPair pair,
        SimilarityEvidence evidence
) {
}
