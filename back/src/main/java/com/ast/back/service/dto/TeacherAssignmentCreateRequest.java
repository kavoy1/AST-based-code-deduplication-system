package com.ast.back.service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherAssignmentCreateRequest(
        String title,
        String language,
        List<Integer> classIds,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String description,
        Boolean allowResubmit,
        Boolean allowLateSubmit,
        Integer maxFiles
) {
}
