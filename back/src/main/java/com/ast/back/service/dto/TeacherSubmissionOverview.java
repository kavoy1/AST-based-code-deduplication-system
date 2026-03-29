package com.ast.back.service.dto;

public record TeacherSubmissionOverview(
        Long submissionId,
        Long studentId,
        Integer version,
        Integer isLate,
        Integer isValid,
        Integer parseOkFiles,
        Integer totalFiles
) {
}
