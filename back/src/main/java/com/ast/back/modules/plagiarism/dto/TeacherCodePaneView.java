package com.ast.back.modules.plagiarism.dto;

import java.util.List;

public record TeacherCodePaneView(
        String title,
        String code,
        List<TeacherCodeHighlightRangeView> highlights
) {
}
