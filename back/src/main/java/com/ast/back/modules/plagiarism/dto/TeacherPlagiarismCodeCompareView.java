package com.ast.back.modules.plagiarism.dto;

import java.util.List;

public record TeacherPlagiarismCodeCompareView(
        String key,
        String label,
        String relationType,
        TeacherCodePaneView left,
        TeacherCodePaneView right,
        List<TeacherCodeSegmentView> segments
) {
}
