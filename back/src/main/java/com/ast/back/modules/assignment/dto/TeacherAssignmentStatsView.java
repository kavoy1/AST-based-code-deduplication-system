package com.ast.back.modules.assignment.dto;

public record TeacherAssignmentStatsView(
        int classCount,
        int studentCount,
        int submittedStudentCount,
        int unsubmittedStudentCount,
        int lateSubmissionCount,
        int latestSubmissionCount
) {
}
