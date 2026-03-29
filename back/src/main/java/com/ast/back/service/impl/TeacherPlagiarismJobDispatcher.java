package com.ast.back.service.impl;

import com.ast.back.entity.PlagiarismJob;

public interface TeacherPlagiarismJobDispatcher {

    void dispatch(PlagiarismJob job, int thresholdScore, int topKPerStudent);
}