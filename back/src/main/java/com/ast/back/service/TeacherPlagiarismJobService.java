package com.ast.back.service;

import com.ast.back.entity.AiExplanation;
import com.ast.back.entity.PlagiarismJob;
import com.ast.back.entity.SimilarityPair;
import com.ast.back.service.dto.TeacherPlagiarismJobReport;
import com.ast.back.service.dto.TeacherPlagiarismJobSummaryView;
import com.ast.back.service.dto.TeacherPlagiarismPairDetailView;

import java.util.List;

public interface TeacherPlagiarismJobService {

    PlagiarismJob createSkeletonJob(Long teacherId, Long assignmentId, Integer thresholdScore, Integer topKPerStudent);

    List<TeacherPlagiarismJobSummaryView> listJobsByAssignment(Long teacherId, Long assignmentId);

    PlagiarismJob getJob(Long teacherId, Long jobId);

    TeacherPlagiarismJobReport getReport(Long teacherId, Long jobId, Integer minScore, Integer perStudentTopK);

    TeacherPlagiarismPairDetailView getPairDetail(Long teacherId, Long pairId);

    SimilarityPair updatePairStatus(Long teacherId, Long pairId, String status, String teacherNote);

    byte[] exportReportCsv(Long teacherId, Long jobId, Integer minScore, Integer perStudentTopK);

    AiExplanation createAiExplanation(Long teacherId, Long pairId);

    AiExplanation getLatestAiExplanation(Long teacherId, Long pairId);
}
