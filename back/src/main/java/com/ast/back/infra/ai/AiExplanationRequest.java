package com.ast.back.infra.ai;

import com.ast.back.modules.ai.dto.AiExplanationMode;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismCodeCompareView;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityEvidence;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityPair;

public record AiExplanationRequest(
        SimilarityPair pair,
        SimilarityEvidence evidence,
        TeacherPlagiarismCodeCompareView codeCompare,
        AiExplanationMode mode,
        boolean includeTeacherNote,
        String teacherNote
) {
}
