package com.ast.back.service.dto;

import java.util.List;

public record TeacherPlagiarismJobReport(
        Long jobId,
        String status,
        String message,
        Integer minScore,
        Integer perStudentTopK,
        TeacherPlagiarismJobStatsView jobStats,
        List<TeacherPlagiarismPairView> pairs,
        List<TeacherIncomparableSubmissionView> incomparableSubmissions
) {
}
