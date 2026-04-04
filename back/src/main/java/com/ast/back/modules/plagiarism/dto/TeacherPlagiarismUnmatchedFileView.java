package com.ast.back.modules.plagiarism.dto;

public record TeacherPlagiarismUnmatchedFileView(
        String key,
        String label,
        TeacherCodePaneView pane
) {
}
