package com.ast.back.modules.plagiarism.domain;

import java.util.Map;

public record TeacherComparableCodeBlock(
        String kind,
        String fileName,
        String summary,
        String normalizedSnippet,
        Integer localStartLine,
        Integer localEndLine,
        Integer fullStartLine,
        Integer fullEndLine,
        Integer statementCount,
        String methodKey,
        Integer relativeDepth,
        Integer structuralNodeTotal,
        Map<String, Integer> structuralSignatureCounts
) {
}
