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
        builder.append("你是高校代码查重系统中的教师复核助手。\n");
        builder.append("你的任务是基于输入材料，输出一个有证据约束的结构化相似度判断。\n");
        builder.append("你必须严格输出 JSON，不要输出 Markdown，不要输出代码块，不要输出额外说明。\n");
        builder.append("JSON 必须包含字段：estimatedSimilarity, rangeMin, rangeMax, useRange, confidence, level, summary, coreEvidence, differenceAdjustments, lowerBoundReason, upperBoundReason, systemEvidenceEffects。\n");
        builder.append("confidence 只能是 HIGH、MEDIUM、LOW。\n");
        builder.append("level 只能是 HIGH、MEDIUM、LOW。\n");
        builder.append("coreEvidence、differenceAdjustments、systemEvidenceEffects 必须是字符串数组。\n");
        builder.append("如果证据不足，必须输出区间，并将 useRange 设为 true。\n");
        builder.append("如果输出单点值，则 rangeMin 和 rangeMax 必须等于 estimatedSimilarity，且 useRange 为 false。\n\n");

        builder.append("【解释模式】\n");
        if (request.mode() == AiExplanationMode.CODE_WITH_SYSTEM_EVIDENCE) {
            builder.append("代码 + 系统证据\n");
            builder.append("你必须先基于代码形成独立判断，再解释系统证据如何修正最终相似度。\n");
            builder.append("不得直接照抄系统相似度作为最终结论。\n");
            builder.append("systemEvidenceEffects 必须至少包含一条内容，说明系统证据如何推高、拉低或限制最终判断。\n\n");
        } else {
            builder.append("仅代码\n");
            builder.append("禁止引用系统原始相似度、AC 系数、命中特征数、证据权重、结构块命中数等系统指标。\n");
            builder.append("systemEvidenceEffects 必须返回空数组。\n");
            builder.append("你只能依据左右代码本身判断相似度。\n\n");
        }

        builder.append("【代码 A】\n");
        builder.append("文件名：").append(nullToEmpty(request.codeCompare().left().title())).append('\n');
        builder.append(nullToEmpty(request.codeCompare().left().code())).append("\n\n");

        builder.append("【代码 B】\n");
        builder.append("文件名：").append(nullToEmpty(request.codeCompare().right().title())).append('\n');
        builder.append(nullToEmpty(request.codeCompare().right().code())).append("\n\n");

        builder.append("【命中片段摘要】\n");
        builder.append(buildSegmentSummary(request)).append("\n\n");

        if (request.mode() == AiExplanationMode.CODE_WITH_SYSTEM_EVIDENCE && request.evidence() != null) {
            builder.append("【系统查重证据】\n");
            builder.append("系统分数=").append(request.pair().getScore()).append('\n');
            builder.append("证据摘要=").append(nullToEmpty(request.evidence().getSummary())).append('\n');
            builder.append("证据 payload 摘要=").append(buildPayloadSummary(request.evidence().getPayloadJson())).append("\n\n");
        }

        if (request.includeTeacherNote() && request.teacherNote() != null && !request.teacherNote().isBlank()) {
            builder.append("【教师备注】\n");
            builder.append(request.teacherNote()).append("\n\n");
        }

        builder.append("【评分要求】\n");
        builder.append("1. 必须给出 estimatedSimilarity。\n");
        builder.append("2. 必须解释为什么不是更高，写入 upperBoundReason。\n");
        builder.append("3. 必须解释为什么不是更低，写入 lowerBoundReason。\n");
        builder.append("4. coreEvidence 写 3 到 5 条最关键的支持理由。\n");
        builder.append("5. differenceAdjustments 写 2 到 4 条会拉低、限制或修正相似度的差异点。\n");
        builder.append("6. summary 用 1 到 2 句话概括最终判断，语言要适合教师复核。\n");
        builder.append("7. 不允许输出空字符串字段；如果某类内容很少，也要给出最简有效解释。\n");
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
