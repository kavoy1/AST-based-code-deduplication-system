package com.ast.back.modules.ai.dto;

public record AiExplanationStructuredResult(
        Integer aiScore,
        Integer systemScore,
        Integer scoreDiff,
        String diffDirection,
        String riskLevel,
        String conclusion,
        String reasoning,
        String evidenceSummary
) {
}
