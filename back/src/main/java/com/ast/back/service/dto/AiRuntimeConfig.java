package com.ast.back.service.dto;

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
