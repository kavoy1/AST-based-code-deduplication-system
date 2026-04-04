package com.ast.back.modules.ai.application.impl;

import com.ast.back.infra.ai.AiExplanationProvider;
import com.ast.back.infra.ai.AiExplanationRequest;
import com.ast.back.infra.ai.AiExplanationResponse;
import com.ast.back.modules.admin.application.SystemConfigService;
import com.ast.back.modules.ai.application.AiExplanationService;
import com.ast.back.modules.ai.dto.AiExplanationStructuredResult;
import com.ast.back.modules.ai.dto.AiRuntimeConfig;
import com.ast.back.modules.ai.persistence.entity.AiExplanation;
import com.ast.back.modules.ai.persistence.mapper.AiExplanationMapper;
import com.ast.back.shared.common.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiExplanationServiceImpl implements AiExplanationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, AiExplanationProvider> providerMap;
    private final SystemConfigService systemConfigService;
    private final AiExplanationMapper aiExplanationMapper;

    public AiExplanationServiceImpl(
            List<AiExplanationProvider> providers,
            SystemConfigService systemConfigService,
            AiExplanationMapper aiExplanationMapper
    ) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        provider -> provider.providerKey().toUpperCase(Locale.ROOT),
                        Function.identity()
                ));
        this.systemConfigService = systemConfigService;
        this.aiExplanationMapper = aiExplanationMapper;
    }

    @Override
    public AiExplanation createExplanation(AiExplanationRequest request) {
        AiRuntimeConfig config = systemConfigService.getAiRuntimeConfig();
        if (!config.enabled()) {
            throw new BusinessException("AI explanation is disabled");
        }
        String providerKey = config.provider() == null ? "QWEN" : config.provider().trim().toUpperCase(Locale.ROOT);
        AiExplanationProvider provider = providerMap.get(providerKey);
        if (provider == null) {
            throw new BusinessException("Unsupported AI provider: " + providerKey);
        }

        AiExplanation explanation = new AiExplanation();
        explanation.setPairId(request.pair().getId());
        explanation.setProvider(provider.providerKey());
        explanation.setModel(config.model());
        explanation.setPromptVersion(config.promptVersion());
        explanation.setCreateTime(LocalDateTime.now());
        try {
            AiExplanationResponse response = provider.generate(request, config);
            AiExplanationStructuredResult structuredResult = parseStructuredResult(response.content(), request.pair().getScore());
            explanation.setProvider(response.provider());
            explanation.setModel(response.model());
            explanation.setPromptVersion(response.promptVersion());
            explanation.setStatus("SUCCESS");
            explanation.setLatencyMs(response.latencyMs());
            explanation.setRequestPayload(buildStoredRequestPayload(request, response.requestPayload()));
            explanation.setResponsePayload(buildStoredResponsePayload(structuredResult, response.responsePayload()));
            explanation.setResult(structuredResult.conclusion());
            aiExplanationMapper.insert(explanation);
            return explanation;
        } catch (Exception ex) {
            explanation.setStatus("FAILED");
            explanation.setLatencyMs(0);
            explanation.setRequestPayload(buildStoredRequestPayload(request, null));
            explanation.setErrorMsg(ex.getMessage());
            aiExplanationMapper.insert(explanation);
            throw ex instanceof BusinessException businessException
                    ? businessException
                    : new BusinessException("Failed to generate AI explanation: " + ex.getMessage());
        }
    }

    private String buildStoredRequestPayload(AiExplanationRequest request, String rawRequestPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mode", request.mode().name());
        payload.put("includeTeacherNote", request.includeTeacherNote());
        payload.put("systemScore", request.pair().getScore());
        payload.put("rawRequest", rawRequestPayload == null ? "" : rawRequestPayload);
        return toJson(payload, rawRequestPayload);
    }

    private String buildStoredResponsePayload(AiExplanationStructuredResult structuredResult, String rawResponsePayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("structured", structuredResult);
        payload.put("rawResponse", rawResponsePayload == null ? "" : rawResponsePayload);
        return toJson(payload, rawResponsePayload);
    }

    private String toJson(Object payload, String fallback) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return fallback;
        }
    }

    private AiExplanationStructuredResult parseStructuredResult(String content, Integer systemScore) {
        int normalizedSystemScore = clampScore(systemScore == null ? 0 : systemScore);
        try {
            JsonNode root = OBJECT_MAPPER.readTree(content);
            int aiScore = clampScore(root.path("aiScore").asInt(normalizedSystemScore));
            int scoreDiff = Math.abs(aiScore - normalizedSystemScore);
            return new AiExplanationStructuredResult(
                    aiScore,
                    normalizedSystemScore,
                    scoreDiff,
                    aiScore > normalizedSystemScore ? "AI_HIGHER" : aiScore < normalizedSystemScore ? "AI_LOWER" : "MATCHED",
                    textOrDefault(root.path("riskLevel").asText(null), deriveRiskLevel(aiScore)),
                    textOrDefault(root.path("conclusion").asText(null), content),
                    textOrDefault(root.path("reasoning").asText(null), content),
                    textOrDefault(root.path("evidenceSummary").asText(null), "")
            );
        } catch (Exception ignored) {
            return new AiExplanationStructuredResult(
                    normalizedSystemScore,
                    normalizedSystemScore,
                    0,
                    "MATCHED",
                    deriveRiskLevel(normalizedSystemScore),
                    content == null || content.isBlank() ? "AI explanation generated" : content,
                    content == null || content.isBlank() ? "No detailed reasoning returned." : content,
                    ""
            );
        }
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String deriveRiskLevel(int aiScore) {
        if (aiScore >= 85) {
            return "HIGH";
        }
        if (aiScore >= 60) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
