package com.ast.back.modules.ai.dto;

public record AiRuntimeConfig(
        boolean enabled,
        String provider,
        String baseUrl,
        String model,
        int timeoutMs,
        String apiKey,
        String promptVersion
) {
}
