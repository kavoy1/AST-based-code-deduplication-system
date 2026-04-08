package com.ast.back.modules.submission.dto;

import java.time.LocalDateTime;

public record TeacherSubmissionOverview(
        Long submissionId,
        Long studentId,
        Integer isLate,
        Integer isValid,
        Integer parseOkFiles,
        Integer totalFiles,
        LocalDateTime lastSubmittedAt
) {
}
