package com.ast.back.service.dto;

import java.time.LocalDateTime;

public record TeacherAssignmentSummaryView(
        Long id,
        String title,
        String language,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        int classCount,
        int studentCount,
        int submittedStudentCount,
        int unsubmittedStudentCount,
        int lateSubmissionCount,
        int materialCount
) {
}
