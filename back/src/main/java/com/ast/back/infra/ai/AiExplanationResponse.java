package com.ast.back.infra.ai;

public record AiExplanationResponse(
        String provider,
        String model,
        String promptVersion,
        String content,
        String requestPayload,
        String responsePayload,
        int latencyMs
) {
}
