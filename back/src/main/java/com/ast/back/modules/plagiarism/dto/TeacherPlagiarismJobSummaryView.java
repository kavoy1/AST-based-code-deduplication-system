package com.ast.back.modules.plagiarism.dto;

import java.time.LocalDateTime;

public record TeacherPlagiarismJobSummaryView(
        Long jobId,
        String status,
        Integer progressTotal,
        Integer progressDone,
        LocalDateTime createTime,
        Integer thresholdScore,
        Integer topKPerStudent,
        String plagiarismMode,
        String executionMode,
        Long reusedFromJobId,
        Integer thresholdMatchedPairs
) {
}
