package com.ast.back.modules.plagiarism.dto;

public record TeacherCodeSegmentView(
        String id,
        String label,
        String summary,
        Integer score,
        String leftFile,
        Integer leftStartLine,
        Integer leftEndLine,
        String rightFile,
        Integer rightStartLine,
        Integer rightEndLine
) {
}
