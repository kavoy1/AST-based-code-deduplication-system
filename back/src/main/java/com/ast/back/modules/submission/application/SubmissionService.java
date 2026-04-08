package com.ast.back.modules.submission.application;

import com.ast.back.modules.submission.dto.CurrentSubmissionDetailView;
import com.ast.back.modules.submission.dto.TextSubmissionRequest;
import com.ast.back.modules.submission.persistence.entity.Submission;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SubmissionService {

    Submission createSubmission(Long studentId, Long assignmentId, List<MultipartFile> files);

    Submission createTextSubmission(Long studentId, Long assignmentId, TextSubmissionRequest request);

    CurrentSubmissionDetailView getCurrentSubmissionDetail(Long studentId, Long assignmentId);
}
