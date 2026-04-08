package com.ast.back.modules.submission.dto;

public record CurrentSubmissionFileView(
        String filename,
        Long bytes,
        String parseStatus,
        String parseError,
        String content
) {
}
