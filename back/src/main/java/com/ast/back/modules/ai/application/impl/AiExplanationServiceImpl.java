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
        ResolvedRuntime runtime = resolveRuntime();
        AiExplanation explanation = buildBaseExplanation(request, runtime);
        return persistCompletedExplanation(explanation, request, runtime, true);
    }

    @Override
    public AiExplanation createPendingExplanation(AiExplanationRequest request) {
        ResolvedRuntime runtime = resolveRuntime();
        AiExplanation explanation = buildBaseExplanation(request, runtime);
        explanation.setStatus("GENERATING");
        explanation.setLatencyMs(0);
        explanation.setRequestPayload(buildStoredRequestPayload(request, null));
        aiExplanationMapper.insert(explanation);
        return explanation;
    }

    @Override
    public void completePendingExplanation(Long explanationId, AiExplanationRequest request) {
        if (explanationId == null) {
            return;
        }
        AiExplanation explanation = aiExplanationMapper.selectById(explanationId);
        if (explanation == null) {
            return;
        }
        ResolvedRuntime runtime = resolveRuntime();
        explanation.setProvider(runtime.provider().providerKey());
        explanation.setModel(runtime.config().model());
        explanation.setPromptVersion(runtime.config().promptVersion());
        persistCompletedExplanation(explanation, request, runtime, false);
    }

    private ResolvedRuntime resolveRuntime() {
        AiRuntimeConfig config = systemConfigService.getAiRuntimeConfig();
        if (!config.enabled()) {
            throw new BusinessException("AI explanation is disabled");
        }
        String providerKey = config.provider() == null ? "QWEN" : config.provider().trim().toUpperCase(Locale.ROOT);
        AiExplanationProvider provider = providerMap.get(providerKey);
        if (provider == null) {
            throw new BusinessException("Unsupported AI provider: " + providerKey);
        }
        return new ResolvedRuntime(config, provider);
    }

    private AiExplanation buildBaseExplanation(AiExplanationRequest request, ResolvedRuntime runtime) {
        AiRuntimeConfig config = runtime.config();
        AiExplanationProvider provider = runtime.provider();

        AiExplanation explanation = new AiExplanation();
        explanation.setPairId(request.pair().getId());
        explanation.setProvider(provider.providerKey());
        explanation.setModel(config.model());
        explanation.setPromptVersion(config.promptVersion());
        explanation.setCreateTime(LocalDateTime.now());
        return explanation;
    }

    private AiExplanation persistCompletedExplanation(
            AiExplanation explanation,
            AiExplanationRequest request,
            ResolvedRuntime runtime,
            boolean insertWhenDone
    ) {
        AiRuntimeConfig config = runtime.config();
        AiExplanationProvider provider = runtime.provider();
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
            explanation.setErrorMsg(null);
            if (insertWhenDone) {
                aiExplanationMapper.insert(explanation);
            } else {
                aiExplanationMapper.updateById(explanation);
            }
            return explanation;
        } catch (Exception ex) {
            explanation.setStatus("FAILED");
            explanation.setLatencyMs(0);
            explanation.setRequestPayload(buildStoredRequestPayload(request, null));
            explanation.setResponsePayload(null);
            explanation.setResult(null);
            explanation.setErrorMsg(ex.getMessage());
            if (insertWhenDone) {
                aiExplanationMapper.insert(explanation);
                throw ex instanceof BusinessException businessException
                        ? businessException
                        : new BusinessException("Failed to generate AI explanation: " + ex.getMessage());
            }
            aiExplanationMapper.updateById(explanation);
            return explanation;
        }
    }

    private record ResolvedRuntime(AiRuntimeConfig config, AiExplanationProvider provider) {
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
