package com.ast.back.ai;

import com.ast.back.service.dto.AiRuntimeConfig;

public interface AiExplanationProvider {

    String providerKey();

    AiExplanationResponse generate(AiExplanationRequest request, AiRuntimeConfig config);
}
