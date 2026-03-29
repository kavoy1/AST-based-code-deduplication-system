package com.ast.back.service.dto;

public record TeacherAssignmentStatsView(
        int classCount,
        int studentCount,
        int submittedStudentCount,
        int unsubmittedStudentCount,
        int lateSubmissionCount,
        int latestSubmissionCount
) {
}
