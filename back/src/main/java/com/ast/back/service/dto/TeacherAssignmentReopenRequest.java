package com.ast.back.service.dto;

import java.time.LocalDateTime;

public record TeacherAssignmentReopenRequest(
        LocalDateTime startAt,
        LocalDateTime endAt,
        String reason
) {
}
