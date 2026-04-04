package com.ast.back.modules.plagiarism.application;

import com.ast.back.modules.ai.dto.AiExplanationMode;
import com.ast.back.modules.ai.persistence.entity.AiExplanation;
import com.ast.back.modules.plagiarism.persistence.entity.PlagiarismJob;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityPair;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismJobReport;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismJobSummaryView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismPairDetailView;

import java.util.List;

public interface TeacherPlagiarismJobService {

    PlagiarismJob createSkeletonJob(Long teacherId, Long assignmentId, Integer thresholdScore, Integer topKPerStudent, String plagiarismMode);

    List<TeacherPlagiarismJobSummaryView> listJobsByAssignment(Long teacherId, Long assignmentId);

    PlagiarismJob getJob(Long teacherId, Long jobId);

    TeacherPlagiarismJobReport getReport(
            Long teacherId,
            Long jobId,
            Integer minScore,
            Integer perStudentTopK,
            String statuses,
            String sortBy,
            String sortDirection
    );

    TeacherPlagiarismPairDetailView getPairDetail(Long teacherId, Long pairId);

    SimilarityPair updatePairStatus(Long teacherId, Long pairId, String status, String teacherNote);

    List<AiExplanation> listAiExplanations(Long teacherId, Long pairId);

    byte[] exportReportCsv(
            Long teacherId,
            Long jobId,
            Integer minScore,
            Integer perStudentTopK,
            String statuses,
            String sortBy,
            String sortDirection
    );

    byte[] exportReportZip(
            Long teacherId,
            Long jobId,
            Integer minScore,
            Integer perStudentTopK,
            String statuses,
            String sortBy,
            String sortDirection
    );

    AiExplanation createAiExplanation(Long teacherId, Long pairId, AiExplanationMode mode, boolean includeTeacherNote);

    AiExplanation getLatestAiExplanation(Long teacherId, Long pairId);
}
