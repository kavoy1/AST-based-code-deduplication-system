package com.ast.back.service.dto;

public record TeacherPlagiarismJobStatsView(
        Integer comparableSubmissionCount,
        Integer comparablePairCount,
        Integer sizeSkippedPairs,
        Integer bucketSkippedPairs,
        Integer fullCalculatedPairs,
        Integer thresholdMatchedPairs,
        String executionMode,
        Long reusedFromJobId
) {
}
