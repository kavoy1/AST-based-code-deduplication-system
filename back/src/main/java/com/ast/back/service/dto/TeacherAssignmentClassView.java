package com.ast.back.service.dto;

public record TeacherAssignmentClassView(
        Integer classId,
        String className,
        int studentCount,
        int submittedStudentCount
) {
}
