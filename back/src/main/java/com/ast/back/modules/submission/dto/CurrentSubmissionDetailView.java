package com.ast.back.modules.submission.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CurrentSubmissionDetailView(
        Long id,
        Long assignmentId,
        Integer classId,
        Long studentId,
        LocalDateTime submitTime,
        Integer isValid,
        Integer parseOkFiles,
        Integer totalFiles,
        Integer isLate,
        LocalDateTime deadlineAt,
        List<CurrentSubmissionFileView> files
) {
}
