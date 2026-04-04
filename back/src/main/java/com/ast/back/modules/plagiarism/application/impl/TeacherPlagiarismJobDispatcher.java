package com.ast.back.modules.plagiarism.application.impl;

import com.ast.back.modules.plagiarism.persistence.entity.PlagiarismJob;

public interface TeacherPlagiarismJobDispatcher {

    void dispatch(PlagiarismJob job, int thresholdScore, int topKPerStudent, String plagiarismMode);
}
