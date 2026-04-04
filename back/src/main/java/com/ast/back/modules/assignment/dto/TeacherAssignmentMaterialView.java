package com.ast.back.modules.assignment.dto;

public record TeacherAssignmentMaterialView(
        Long id,
        String originalName,
        Long sizeBytes,
        String contentType,
        String downloadUrl
) {
}
