package com.ast.back.modules.plagiarism.dto;

import java.util.List;

public record TeacherPlagiarismPairDetailView(
        Long pairId,
        Long jobId,
        Long studentA,
        Long studentB,
        Integer score,
        String status,
        String teacherNote,
        String currentAiProvider,
        String currentAiModel,
        List<TeacherPlagiarismEvidenceView> evidences,
        com.ast.back.modules.ai.persistence.entity.AiExplanation latestAiExplanation,
        TeacherPlagiarismCodeCompareView codeCompare,
        List<TeacherPlagiarismCodeCompareView> matchedFilePairs,
        List<TeacherPlagiarismUnmatchedFileView> unmatchedLeftFiles,
        List<TeacherPlagiarismUnmatchedFileView> unmatchedRightFiles,
        List<TeacherPlagiarismCodeCompareView> crossFileSegments
) {
}
