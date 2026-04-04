package com.ast.back.modules.submission.dto;

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
