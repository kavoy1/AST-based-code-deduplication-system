package com.ast.back.infra.ai;

import com.ast.back.modules.ai.dto.AiExplanationMode;
import com.ast.back.modules.plagiarism.dto.TeacherCodeSegmentView;
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
        builder.append("你是高校代码查重系统中的 AI 复核助手。\n");
        builder.append("你的任务是对两段代码给出 0 到 100 的 AI 相似度评分，并解释相似的原因。\n");
        builder.append("请严格输出 JSON，不要输出 Markdown，不要输出代码块。\n");
        builder.append("JSON 必须包含字段：aiScore, riskLevel, conclusion, reasoning, evidenceSummary。\n");
        builder.append("riskLevel 只能是 HIGH、MEDIUM、LOW。\n");
        builder.append("其中 aiScore 是你独立给出的相似度评分。\n\n");

        builder.append("【解释模式】\n");
        builder.append(request.mode() == AiExplanationMode.CODE_WITH_SYSTEM_EVIDENCE
                ? "代码 + 系统证据\n\n"
                : "仅代码\n\n");

        builder.append("【代码 A】\n");
        builder.append("文件名：").append(nullToEmpty(request.codeCompare().left().title())).append('\n');
        builder.append(request.codeCompare().left().code()).append("\n\n");

        builder.append("【代码 B】\n");
        builder.append("文件名：").append(nullToEmpty(request.codeCompare().right().title())).append('\n');
        builder.append(request.codeCompare().right().code()).append("\n\n");

        builder.append("【匹配片段摘要】\n");
        builder.append(buildSegmentSummary(request)).append("\n\n");

        if (request.mode() == AiExplanationMode.CODE_WITH_SYSTEM_EVIDENCE && request.evidence() != null) {
            builder.append("【系统查重证据】\n");
            builder.append("系统分数=").append(request.pair().getScore()).append('\n');
            builder.append("证据摘要=").append(nullToEmpty(request.evidence().getSummary())).append('\n');
            builder.append("证据 payload 摘要=").append(buildPayloadSummary(request.evidence().getPayloadJson())).append("\n\n");
        }

        if (request.includeTeacherNote() && request.teacherNote() != null && !request.teacherNote().isBlank()) {
            builder.append("【老师备注】\n");
            builder.append(request.teacherNote()).append("\n\n");
        }

        builder.append("请在 reasoning 中说明相似的核心结构和实现逻辑。\n");
        builder.append("如果当前模式包含系统证据，请在 conclusion 中顺带说明系统分数与你的 AI 分数是否接近。\n");
        return builder.toString();
    }

    private String buildSegmentSummary(AiExplanationRequest request) {
        List<TeacherCodeSegmentView> segments = request.codeCompare().segments();
        if (segments == null || segments.isEmpty()) {
            return "暂无明确命中片段，请基于完整代码的整体结构做判断。";
        }
        List<String> lines = new ArrayList<>();
        for (TeacherCodeSegmentView segment : segments) {
            lines.add(String.format(
                    "%s | A:%s(%d-%d) | B:%s(%d-%d) | score=%s | %s",
                    nullToEmpty(segment.label()),
                    nullToEmpty(segment.leftFile()),
                    zeroIfNull(segment.leftStartLine()),
                    zeroIfNull(segment.leftEndLine()),
                    nullToEmpty(segment.rightFile()),
                    zeroIfNull(segment.rightStartLine()),
                    zeroIfNull(segment.rightEndLine()),
                    segment.score() == null ? "-" : segment.score().toString(),
                    nullToEmpty(segment.summary())
            ));
            if (lines.size() >= 6) {
                break;
            }
        }
        return String.join("\n", lines);
    }

    private String buildPayloadSummary(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return "{}";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payloadJson);
            JsonNode totals = root.path("totals");
            JsonNode topN = root.path("topN");
            List<String> highlights = new ArrayList<>();
            for (JsonNode node : topN) {
                String signature = node.path("signature").asText("");
                int matchedCount = node.path("matchedCount").asInt(0);
                if (!signature.isBlank()) {
                    highlights.add(signature + " x" + matchedCount);
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
                    + "\"topHighlights\":\"" + String.join("; ", highlights) + "\""
                    + "}";
        } catch (Exception ex) {
            return payloadJson;
        }
    }

    private int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
