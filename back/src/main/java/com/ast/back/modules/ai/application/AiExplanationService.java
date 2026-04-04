package com.ast.back.modules.ai.application;

import com.ast.back.infra.ai.AiExplanationRequest;
import com.ast.back.modules.ai.persistence.entity.AiExplanation;

public interface AiExplanationService {

    AiExplanation createExplanation(AiExplanationRequest request);
}
