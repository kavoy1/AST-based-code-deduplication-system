package com.ast.back.modules.plagiarism.application.impl;

import com.ast.back.modules.plagiarism.persistence.entity.PlagiarismJob;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityPair;
import com.ast.back.modules.plagiarism.persistence.mapper.PlagiarismJobMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SimilarityPairMapper;
import com.ast.back.modules.plagiarism.application.StudentPlagiarismSummaryService;
import com.ast.back.modules.plagiarism.dto.StudentPlagiarismSummary;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class StudentPlagiarismSummaryServiceImpl implements StudentPlagiarismSummaryService {

    private final PlagiarismJobMapper plagiarismJobMapper;
    private final SimilarityPairMapper similarityPairMapper;

    public StudentPlagiarismSummaryServiceImpl(
            PlagiarismJobMapper plagiarismJobMapper,
            SimilarityPairMapper similarityPairMapper
    ) {
        this.plagiarismJobMapper = plagiarismJobMapper;
        this.similarityPairMapper = similarityPairMapper;
    }

    @Override
    public StudentPlagiarismSummary getSummary(Long studentId, Long assignmentId) {
        QueryWrapper<PlagiarismJob> jobQuery = new QueryWrapper<>();
        jobQuery.eq("assignment_id", assignmentId).eq("status", "DONE").orderByDesc("id").last("LIMIT 1");
        PlagiarismJob latestDoneJob = plagiarismJobMapper.selectOne(jobQuery);
        if (latestDoneJob == null) {
            return StudentPlagiarismSummary.notGenerated("未生成报告");
        }

        QueryWrapper<SimilarityPair> pairQuery = new QueryWrapper<>();
        pairQuery.eq("job_id", latestDoneJob.getId())
                .and(wrapper -> wrapper.eq("student_a", studentId).or().eq("student_b", studentId));
        List<SimilarityPair> pairs = similarityPairMapper.selectList(pairQuery);
        if (pairs == null || pairs.isEmpty()) {
            return StudentPlagiarismSummary.generated(0, "PENDING", null);
        }

        SimilarityPair highestPair = pairs.stream()
                .max(Comparator.comparing(SimilarityPair::getScore))
                .orElseThrow();
        return StudentPlagiarismSummary.generated(
                highestPair.getScore(),
                highestPair.getStatus(),
                highestPair.getTeacherNote()
        );
    }
}
