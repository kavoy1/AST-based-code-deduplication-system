package com.ast.back.modules.plagiarism.domain;

public record TeacherComparableCodeBlock(
        String kind,
        String fileName,
        String summary,
        String normalizedSnippet,
        Integer localStartLine,
        Integer localEndLine,
        Integer fullStartLine,
        Integer fullEndLine,
        Integer statementCount
) {
}
