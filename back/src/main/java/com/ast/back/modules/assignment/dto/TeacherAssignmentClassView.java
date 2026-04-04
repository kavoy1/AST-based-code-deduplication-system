package com.ast.back.modules.assignment.dto;

public record TeacherAssignmentClassView(
        Integer classId,
        String className,
        int studentCount,
        int submittedStudentCount
) {
}
