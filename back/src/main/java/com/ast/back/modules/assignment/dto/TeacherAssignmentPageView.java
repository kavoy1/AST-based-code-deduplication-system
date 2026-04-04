package com.ast.back.modules.assignment.dto;

import java.util.List;

public record TeacherAssignmentPageView(
        List<TeacherAssignmentSummaryView> records,
        long total
) {
}
