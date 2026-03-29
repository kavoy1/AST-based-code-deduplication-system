package com.ast.back.service.dto;

import java.util.List;

public record TeacherPlagiarismPairDetailView(
        Long pairId,
        Long jobId,
        Long studentA,
        Long studentB,
        Integer score,
        String status,
        String teacherNote,
        List<TeacherPlagiarismEvidenceView> evidences,
        com.ast.back.entity.AiExplanation latestAiExplanation
) {
}
