package com.ast.back.infra.ai;

import com.ast.back.modules.ai.dto.AiRuntimeConfig;

public interface AiExplanationProvider {

    String providerKey();

    AiExplanationResponse generate(AiExplanationRequest request, AiRuntimeConfig config);
}
