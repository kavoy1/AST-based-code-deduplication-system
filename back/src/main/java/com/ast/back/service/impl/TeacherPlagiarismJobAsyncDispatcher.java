package com.ast.back.service.impl;

import com.ast.back.entity.PlagiarismJob;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TeacherPlagiarismJobAsyncDispatcher implements TeacherPlagiarismJobDispatcher {

    private final TeacherPlagiarismExecutionService teacherPlagiarismExecutionService;

    public TeacherPlagiarismJobAsyncDispatcher(TeacherPlagiarismExecutionService teacherPlagiarismExecutionService) {
        this.teacherPlagiarismExecutionService = teacherPlagiarismExecutionService;
    }

    @Override
    @Async("plagiarismJobExecutor")
    public void dispatch(PlagiarismJob job, int thresholdScore, int topKPerStudent) {
        teacherPlagiarismExecutionService.execute(job, thresholdScore, topKPerStudent);
    }
}