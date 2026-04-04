package com.ast.back.modules.assignment.dto;

import java.time.LocalDateTime;

public record TeacherAssignmentReopenRequest(
        LocalDateTime startAt,
        LocalDateTime endAt,
        String reason
) {
}
