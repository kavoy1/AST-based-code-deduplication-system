package com.ast.back.service.dto;

public record TeacherPlagiarismEvidenceView(
        Long id,
        String type,
        String summary,
        Integer weight,
        String payloadJson
) {
}
