package com.ast.back.service;

import com.ast.back.entity.AiExplanation;
import com.ast.back.entity.SimilarityEvidence;
import com.ast.back.entity.SimilarityPair;

public interface AiExplanationService {

    AiExplanation createExplanation(SimilarityPair pair, SimilarityEvidence evidence);
}
