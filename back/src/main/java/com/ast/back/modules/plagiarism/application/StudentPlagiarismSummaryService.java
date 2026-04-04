package com.ast.back.modules.plagiarism.application;

import com.ast.back.modules.plagiarism.dto.StudentPlagiarismSummary;

public interface StudentPlagiarismSummaryService {

    StudentPlagiarismSummary getSummary(Long studentId, Long assignmentId);
}
