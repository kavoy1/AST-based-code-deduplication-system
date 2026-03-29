package com.ast.back.service.impl;

import com.ast.back.ai.AiExplanationProvider;
import com.ast.back.ai.AiExplanationRequest;
import com.ast.back.ai.AiExplanationResponse;
import com.ast.back.common.BusinessException;
import com.ast.back.entity.AiExplanation;
import com.ast.back.entity.SimilarityEvidence;
import com.ast.back.entity.SimilarityPair;
import com.ast.back.mapper.AiExplanationMapper;
import com.ast.back.service.AiExplanationService;
import com.ast.back.service.SystemConfigService;
import com.ast.back.service.dto.AiRuntimeConfig;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiExplanationServiceImpl implements AiExplanationService {

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
    public AiExplanation createExplanation(SimilarityPair pair, SimilarityEvidence evidence) {
        AiRuntimeConfig config = systemConfigService.getAiRuntimeConfig();
        if (!config.enabled()) {
            throw new BusinessException("AI 解释功能未启用");
        }
        String providerKey = config.provider() == null ? "QWEN" : config.provider().trim().toUpperCase(Locale.ROOT);
        AiExplanationProvider provider = providerMap.get(providerKey);
        if (provider == null) {
            throw new BusinessException("不支持的 AI Provider: " + providerKey);
        }

        AiExplanation explanation = new AiExplanation();
        explanation.setPairId(pair.getId());
        explanation.setProvider(provider.providerKey());
        explanation.setModel(config.model());
        explanation.setPromptVersion(config.promptVersion());
        explanation.setCreateTime(LocalDateTime.now());
        try {
            AiExplanationResponse response = provider.generate(new AiExplanationRequest(pair, evidence), config);
            explanation.setProvider(response.provider());
            explanation.setModel(response.model());
            explanation.setPromptVersion(response.promptVersion());
            explanation.setStatus("SUCCESS");
            explanation.setLatencyMs(response.latencyMs());
            explanation.setRequestPayload(response.requestPayload());
            explanation.setResponsePayload(response.responsePayload());
            explanation.setResult(response.content());
            aiExplanationMapper.insert(explanation);
            return explanation;
        } catch (Exception ex) {
            explanation.setStatus("FAILED");
            explanation.setLatencyMs(0);
            explanation.setErrorMsg(ex.getMessage());
            aiExplanationMapper.insert(explanation);
            throw ex instanceof BusinessException businessException ? businessException : new BusinessException("AI 解释生成失败：" + ex.getMessage());
        }
    }
}
