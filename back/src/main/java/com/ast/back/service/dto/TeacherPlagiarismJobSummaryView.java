package com.ast.back.service.dto;

import java.time.LocalDateTime;

public record TeacherPlagiarismJobSummaryView(
        Long jobId,
        String status,
        Integer progressTotal,
        Integer progressDone,
        LocalDateTime createTime,
        Integer thresholdScore,
        Integer topKPerStudent,
        String executionMode,
        Long reusedFromJobId,
        Integer thresholdMatchedPairs
) {
}
