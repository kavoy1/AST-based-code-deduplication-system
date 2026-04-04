package com.ast.back.modules.plagiarism.dto;

import java.util.List;

public record TeacherIncomparableSubmissionView(
        Long submissionId,
        Integer classId,
        Long studentId,
        Integer version,
        String reason,
        Integer parseOkFiles,
        Integer totalFiles,
        List<TeacherParseFailureView> parseFailures
) {
}
