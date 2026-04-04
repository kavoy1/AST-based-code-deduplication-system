package com.ast.back.infra.ai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ast.back.shared.common.BusinessException;
import com.ast.back.modules.ai.dto.AiRuntimeConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QwenAiExplanationProvider implements AiExplanationProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private final QwenPromptBuilder promptBuilder;

    public QwenAiExplanationProvider(QwenPromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    @Override
    public String providerKey() {
        return "QWEN";
    }

    @Override
    public AiExplanationResponse generate(AiExplanationRequest request, AiRuntimeConfig config) {
        String apiKey = config.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("未配置 QWEN API Key");
        }
        String baseUrl = config.baseUrl() == null || config.baseUrl().isBlank() ? DEFAULT_BASE_URL : config.baseUrl();
        String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

        String prompt = promptBuilder.buildUserPrompt(request, config.promptVersion());
        String requestPayload = buildRequestPayload(config.model(), prompt);

        long start = System.currentTimeMillis();
        try (HttpResponse response = HttpRequest.post(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(config.timeoutMs())
                .body(requestPayload)
                .execute()) {
            int latencyMs = (int) (System.currentTimeMillis() - start);
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                throw new BusinessException("QWEN 调用失败，HTTP " + response.getStatus() + "：" + response.body());
            }
            String responseBody = response.body();
            String content = extractContent(responseBody);
            if (content == null || content.isBlank()) {
                throw new BusinessException("QWEN 返回内容为空");
            }
            return new AiExplanationResponse(
                    providerKey(),
                    config.model(),
                    config.promptVersion(),
                    content.trim(),
                    requestPayload,
                    responseBody,
                    latencyMs
            );
        }
    }

    private String buildRequestPayload(String model, String prompt) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.2);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", "你是代码查重解释助手，只做解释增强，不做相似度判定。"),
                    Map.of("role", "user", "content", prompt)
            ));
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("构建 QWEN 请求失败", ex);
        }
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }
            return choices.get(0).path("message").path("content").asText(null);
        } catch (Exception ex) {
            throw new BusinessException("解析 QWEN 响应失败");
        }
    }
}
