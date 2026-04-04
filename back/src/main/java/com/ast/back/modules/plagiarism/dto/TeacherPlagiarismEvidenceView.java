package com.ast.back.modules.plagiarism.dto;

public record TeacherPlagiarismEvidenceView(
        Long id,
        String type,
        String summary,
        Integer weight,
        String payloadJson
) {
}
