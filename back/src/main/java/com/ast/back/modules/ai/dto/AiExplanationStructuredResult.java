package com.ast.back.modules.ai.dto;

import java.util.List;

public record AiExplanationStructuredResult(
        Integer estimatedSimilarity,
        Integer rangeMin,
        Integer rangeMax,
        Boolean useRange,
        String confidence,
        String level,
        String summary,
        List<String> coreEvidence,
        List<String> differenceAdjustments,
        String lowerBoundReason,
        String upperBoundReason,
        List<String> systemEvidenceEffects,
        Integer systemScore,
        Integer scoreDiff,
        String diffDirection
) {
}
