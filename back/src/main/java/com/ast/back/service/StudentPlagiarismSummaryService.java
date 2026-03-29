package com.ast.back.service;

import com.ast.back.service.dto.StudentPlagiarismSummary;

public interface StudentPlagiarismSummaryService {

    StudentPlagiarismSummary getSummary(Long studentId, Long assignmentId);
}
