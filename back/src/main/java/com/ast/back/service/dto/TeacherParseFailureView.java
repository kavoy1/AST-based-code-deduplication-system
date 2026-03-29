package com.ast.back.service.dto;

public record TeacherParseFailureView(
        String file,
        String reason
) {
}
