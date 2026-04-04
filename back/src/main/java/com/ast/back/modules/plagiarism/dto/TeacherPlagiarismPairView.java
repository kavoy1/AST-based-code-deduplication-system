package com.ast.back.modules.plagiarism.dto;

public record TeacherPlagiarismPairView(
        Long pairId,
        Long studentA,
        Long studentB,
        Integer score,
        String status,
        String teacherNote
) {
}
