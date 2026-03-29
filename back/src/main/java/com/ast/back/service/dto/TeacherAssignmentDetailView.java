package com.ast.back.service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherAssignmentDetailView(
        Long id,
        String title,
        String language,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        boolean allowResubmit,
        boolean allowLateSubmit,
        int maxFiles,
        List<TeacherAssignmentClassView> classes,
        List<TeacherAssignmentMaterialView> materials,
        TeacherAssignmentStatsView stats
) {
}
