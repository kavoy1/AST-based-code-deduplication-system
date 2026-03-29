package com.ast.back.service.dto;

public record TeacherAssignmentMaterialView(
        Long id,
        String originalName,
        Long sizeBytes,
        String contentType,
        String downloadUrl
) {
}
