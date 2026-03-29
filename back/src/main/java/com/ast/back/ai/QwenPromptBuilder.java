package com.ast.back.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class QwenPromptBuilder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String buildUserPrompt(AiExplanationRequest request, String promptVersion) {
        StringBuilder builder = new StringBuilder();
        builder.append("promptVersion=").append(promptVersion).append('\n');
        builder.append("你是高校代码作业查重系统中的解释助手。");
        builder.append("你只能基于给定证据解释“为什么这对提交被系统判为高相似”，");
        builder.append("不能修改分数，不能下最终作弊结论，不能建议惩罚。");
        builder.append("输出要求：\n");
        builder.append("1. 用中文，120到220字。\n");
        builder.append("2. 先给结论概述，再说明关键重合结构，再提示教师人工复核要点。\n");
        builder.append("3. 只能使用输入中的证据与统计，不得编造源码细节。\n");
        builder.append("4. 不要出现“我认为作弊”这类裁决性表述。\n\n");
        builder.append("输入数据：\n");
        builder.append("score=").append(request.pair().getScore()).append('\n');
        builder.append("status=").append(request.pair().getStatus()).append('\n');
        builder.append("teacherNote=").append(nullToEmpty(request.pair().getTeacherNote())).append('\n');
        builder.append("evidenceSummary=").append(nullToEmpty(request.evidence().getSummary())).append('\n');
        builder.append("payload=").append(buildPayloadSummary(request.evidence().getPayloadJson())).append('\n');
        return builder.toString();
    }

    private String buildPayloadSummary(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return "{}";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payloadJson);
            JsonNode totals = root.path("totals");
            JsonNode topN = root.path("topN");
            JsonNode parseFailures = root.path("parseFailures");
            List<String> highlights = new ArrayList<>();
            for (JsonNode node : topN) {
                String signature = node.path("signature").asText("");
                int matchedCount = node.path("matchedCount").asInt(0);
                if (!signature.isBlank()) {
                    highlights.add(signature + "×" + matchedCount);
                }
                if (highlights.size() >= 5) {
                    break;
                }
            }
            return "{"
                    + "\"algoVersion\":\"" + root.path("algoVersion").asText("") + "\","
                    + "\"N1\":" + totals.path("N1").asInt(0) + ","
                    + "\"N2\":" + totals.path("N2").asInt(0) + ","
                    + "\"M\":" + totals.path("M").asInt(0) + ","
                    + "\"AC\":" + totals.path("AC").asDouble(0.0) + ","
                    + "\"topHighlights\":\"" + String.join("；", highlights) + "\","
                    + "\"parseFailureCountA\":" + parseFailures.path("sideA").size() + ","
                    + "\"parseFailureCountB\":" + parseFailures.path("sideB").size()
                    + "}";
        } catch (Exception ex) {
            return payloadJson;
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
