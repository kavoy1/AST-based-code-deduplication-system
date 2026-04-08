package com.ast.back.modules.submission.controller;

import com.ast.back.modules.submission.application.SubmissionService;
import com.ast.back.modules.submission.dto.CurrentSubmissionDetailView;
import com.ast.back.modules.submission.dto.TextSubmissionRequest;
import com.ast.back.modules.submission.persistence.entity.Submission;
import com.ast.back.shared.common.Result;
import com.ast.back.shared.security.CurrentUserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/student/assignments")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final CurrentUserService currentUserService;

    public SubmissionController(SubmissionService submissionService, CurrentUserService currentUserService) {
        this.submissionService = submissionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping(value = "/{assignmentId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Submission> createSubmission(
            @PathVariable Long assignmentId,
            @RequestPart("files") List<MultipartFile> files
    ) {
        Long studentId = currentUserService.getCurrentUserId();
        return Result.success(submissionService.createSubmission(studentId, assignmentId, files));
    }

    @PostMapping("/{assignmentId}/submissions/text")
    public Result<Submission> createTextSubmission(
            @PathVariable Long assignmentId,
            @RequestBody TextSubmissionRequest request
    ) {
        Long studentId = currentUserService.getCurrentUserId();
        return Result.success(submissionService.createTextSubmission(studentId, assignmentId, request));
    }

    @GetMapping("/{assignmentId}/current-submission")
    public Result<CurrentSubmissionDetailView> getCurrentSubmission(@PathVariable Long assignmentId) {
        Long studentId = currentUserService.getCurrentUserId();
        return Result.success(submissionService.getCurrentSubmissionDetail(studentId, assignmentId));
    }
}
