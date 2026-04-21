package com.ast.back.modules.ai.application.impl;

import com.ast.back.infra.ai.AiExplanationProvider;
import com.ast.back.infra.ai.AiExplanationRequest;
import com.ast.back.infra.ai.AiExplanationResponse;
import com.ast.back.modules.admin.application.SystemConfigService;
import com.ast.back.modules.ai.application.AiExplanationService;
import com.ast.back.modules.ai.dto.AiExplanationMode;
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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiExplanationServiceImpl implements AiExplanationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> ALLOWED_BANDS = List.of("HIGH", "MEDIUM", "LOW");

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
            AiExplanationStructuredResult structuredResult = parseStructuredResult(response.content(), request);
            explanation.setProvider(response.provider());
            explanation.setModel(response.model());
            explanation.setPromptVersion(response.promptVersion());
            explanation.setStatus("SUCCESS");
            explanation.setLatencyMs(response.latencyMs());
            explanation.setRequestPayload(buildStoredRequestPayload(request, response.requestPayload()));
            explanation.setResponsePayload(buildStoredResponsePayload(structuredResult, response.responsePayload()));
            explanation.setResult(structuredResult.summary());
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
        }
        return explanation;
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

    private AiExplanationStructuredResult parseStructuredResult(String content, AiExplanationRequest request) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(content);
            int normalizedSystemScore = clampScore(request.pair().getScore() == null ? 0 : request.pair().getScore());

            Integer estimatedSimilarity = readNullableInt(root, "estimatedSimilarity");
            Integer rangeMin = readNullableInt(root, "rangeMin");
            Integer rangeMax = readNullableInt(root, "rangeMax");

            if (estimatedSimilarity == null && (rangeMin == null || rangeMax == null)) {
                throw new BusinessException("AI explanation must contain estimatedSimilarity or a complete range");
            }

            if (rangeMin == null && estimatedSimilarity != null) {
                rangeMin = estimatedSimilarity;
            }
            if (rangeMax == null && estimatedSimilarity != null) {
                rangeMax = estimatedSimilarity;
            }
            if (estimatedSimilarity == null && rangeMin != null && rangeMax != null) {
                estimatedSimilarity = Math.round((rangeMin + rangeMax) / 2.0f);
            }

            int normalizedEstimatedSimilarity = clampScore(Objects.requireNonNull(estimatedSimilarity));
            int normalizedRangeMin = clampScore(Objects.requireNonNull(rangeMin));
            int normalizedRangeMax = clampScore(Objects.requireNonNull(rangeMax));
            if (normalizedRangeMin > normalizedRangeMax) {
                int temp = normalizedRangeMin;
                normalizedRangeMin = normalizedRangeMax;
                normalizedRangeMax = temp;
            }

            Boolean useRange = readNullableBoolean(root, "useRange");
            boolean normalizedUseRange = useRange != null ? useRange : normalizedRangeMin != normalizedRangeMax;

            String summary = requiredText(root, "summary");
            String lowerBoundReason = requiredText(root, "lowerBoundReason");
            String upperBoundReason = requiredText(root, "upperBoundReason");

            List<String> coreEvidence = readStringList(root, "coreEvidence");
            List<String> differenceAdjustments = readStringList(root, "differenceAdjustments");
            List<String> systemEvidenceEffects = readStringList(root, "systemEvidenceEffects");

            if (coreEvidence.isEmpty()) {
                throw new BusinessException("AI explanation must contain coreEvidence");
            }
            if (differenceAdjustments.isEmpty()) {
                throw new BusinessException("AI explanation must contain differenceAdjustments");
            }

            String confidence = normalizeBand(textOrDefault(root.path("confidence").asText(null), deriveConfidence(normalizedRangeMin, normalizedRangeMax)));
            String level = normalizeBand(textOrDefault(root.path("level").asText(null), deriveLevel(normalizedEstimatedSimilarity)));

            validateModeRules(request.mode(), systemEvidenceEffects);

            int scoreDiff = Math.abs(normalizedEstimatedSimilarity - normalizedSystemScore);
            return new AiExplanationStructuredResult(
                    normalizedEstimatedSimilarity,
                    normalizedRangeMin,
                    normalizedRangeMax,
                    normalizedUseRange,
                    confidence,
                    level,
                    summary,
                    coreEvidence,
                    differenceAdjustments,
                    lowerBoundReason,
                    upperBoundReason,
                    systemEvidenceEffects,
                    normalizedSystemScore,
                    scoreDiff,
                    deriveDiffDirection(normalizedEstimatedSimilarity, normalizedSystemScore)
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("AI explanation returned invalid structured JSON: " + ex.getMessage());
        }
    }

    private void validateModeRules(AiExplanationMode mode, List<String> systemEvidenceEffects) {
        boolean hasSystemEvidenceEffects = systemEvidenceEffects != null && !systemEvidenceEffects.isEmpty();
        if (mode == AiExplanationMode.CODE_ONLY && hasSystemEvidenceEffects) {
            throw new BusinessException("CODE_ONLY result must not contain systemEvidenceEffects");
        }
        if (mode == AiExplanationMode.CODE_WITH_SYSTEM_EVIDENCE && !hasSystemEvidenceEffects) {
            throw new BusinessException("CODE_WITH_SYSTEM_EVIDENCE result must explain system evidence effects");
        }
    }

    private Integer readNullableInt(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        String text = node.asText(null);
        if (text == null || text.isBlank()) {
            return null;
        }
        return Integer.parseInt(text.trim());
    }

    private Boolean readNullableBoolean(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        String text = node.asText(null);
        if (text == null || text.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(text.trim());
    }

    private List<String> readStringList(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (!node.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asText)
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String requiredText(JsonNode root, String fieldName) {
        String value = root.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            throw new BusinessException("AI explanation is missing required field: " + fieldName);
        }
        return value.trim();
    }

    private String normalizeBand(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_BANDS.contains(normalized)) {
            throw new BusinessException("AI explanation returned invalid band value: " + value);
        }
        return normalized;
    }

    private String deriveConfidence(int rangeMin, int rangeMax) {
        int width = Math.abs(rangeMax - rangeMin);
        if (width <= 4) {
            return "HIGH";
        }
        if (width <= 10) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String deriveLevel(int estimatedSimilarity) {
        if (estimatedSimilarity >= 85) {
            return "HIGH";
        }
        if (estimatedSimilarity >= 60) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String deriveDiffDirection(int aiScore, int systemScore) {
        if (aiScore > systemScore) {
            return "AI_HIGHER";
        }
        if (aiScore < systemScore) {
            return "AI_LOWER";
        }
        return "MATCHED";
    }

    private String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record ResolvedRuntime(AiRuntimeConfig config, AiExplanationProvider provider) {
    }
}
