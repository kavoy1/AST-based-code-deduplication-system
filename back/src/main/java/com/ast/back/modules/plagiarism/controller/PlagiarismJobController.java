package com.ast.back.modules.plagiarism.controller;

import com.ast.back.modules.ai.dto.AiExplanationMode;
import com.ast.back.shared.common.Result;
import com.ast.back.modules.ai.persistence.entity.AiExplanation;
import com.ast.back.modules.plagiarism.persistence.entity.PlagiarismJob;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityPair;
import com.ast.back.shared.security.CurrentUserService;
import com.ast.back.modules.plagiarism.application.TeacherPlagiarismJobService;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismJobCreateRequest;
import com.ast.back.modules.plagiarism.dto.TeacherAiExplanationCreateRequest;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismJobReport;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismJobSummaryView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismPairDetailView;
import com.ast.back.modules.plagiarism.dto.TeacherPlagiarismPairStatusUpdateRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
public class PlagiarismJobController {

    private final TeacherPlagiarismJobService teacherPlagiarismJobService;
    private final CurrentUserService currentUserService;

    public PlagiarismJobController(
            TeacherPlagiarismJobService teacherPlagiarismJobService,
            CurrentUserService currentUserService
    ) {
        this.teacherPlagiarismJobService = teacherPlagiarismJobService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/assignments/{assignmentId}/plagiarism-jobs")
    public Result<PlagiarismJob> createJob(
            @PathVariable Long assignmentId,
            @RequestBody(required = false) TeacherPlagiarismJobCreateRequest request
    ) {
        TeacherPlagiarismJobCreateRequest body = request == null ? new TeacherPlagiarismJobCreateRequest() : request;
        return Result.success(teacherPlagiarismJobService.createSkeletonJob(
                currentUserService.getCurrentUserId(),
                assignmentId,
                body.getThresholdScore(),
                body.getTopKPerStudent(),
                body.getPlagiarismMode()
        ));
    }

    @GetMapping("/assignments/{assignmentId}/plagiarism-jobs")
    public Result<List<TeacherPlagiarismJobSummaryView>> listJobsByAssignment(@PathVariable Long assignmentId) {
        return Result.success(teacherPlagiarismJobService.listJobsByAssignment(currentUserService.getCurrentUserId(), assignmentId));
    }

    @GetMapping("/plagiarism-jobs/{jobId}")
    public Result<PlagiarismJob> getJob(@PathVariable Long jobId) {
        return Result.success(teacherPlagiarismJobService.getJob(currentUserService.getCurrentUserId(), jobId));
    }

    @GetMapping("/plagiarism-jobs/{jobId}/report")
    public Result<TeacherPlagiarismJobReport> getReport(
            @PathVariable Long jobId,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer perStudentTopK,
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        return Result.success(teacherPlagiarismJobService.getReport(
                currentUserService.getCurrentUserId(),
                jobId,
                minScore,
                perStudentTopK,
                statuses,
                sortBy,
                sortDirection
        ));
    }

    @GetMapping("/similarity-pairs/{pairId}")
    public Result<TeacherPlagiarismPairDetailView> getPairDetail(@PathVariable Long pairId) {
        return Result.success(teacherPlagiarismJobService.getPairDetail(currentUserService.getCurrentUserId(), pairId));
    }

    @GetMapping("/plagiarism-jobs/{jobId}/export")
    public ResponseEntity<ByteArrayResource> exportReport(
            @PathVariable Long jobId,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer perStudentTopK,
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false, defaultValue = "csv") String format
    ) {
        boolean zip = "zip".equalsIgnoreCase(format);
        byte[] bytes = zip
                ? teacherPlagiarismJobService.exportReportZip(
                currentUserService.getCurrentUserId(),
                jobId,
                minScore,
                perStudentTopK,
                statuses,
                sortBy,
                sortDirection
        )
                : teacherPlagiarismJobService.exportReportCsv(
                currentUserService.getCurrentUserId(),
                jobId,
                minScore,
                perStudentTopK,
                statuses,
                sortBy,
                sortDirection
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plagiarism-report-" + jobId + (zip ? ".zip" : ".csv"))
                .contentType(MediaType.parseMediaType(zip ? "application/zip" : "text/csv;charset=UTF-8"))
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/similarity-pairs/{pairId}/status")
    public Result<SimilarityPair> updatePairStatus(
            @PathVariable Long pairId,
            @RequestBody TeacherPlagiarismPairStatusUpdateRequest request
    ) {
        return Result.success(teacherPlagiarismJobService.updatePairStatus(
                currentUserService.getCurrentUserId(),
                pairId,
                request.getStatus(),
                request.getTeacherNote()
        ));
    }

    @PostMapping("/similarity-pairs/{pairId}/ai-explanations")
    public Result<AiExplanation> createAiExplanation(
            @PathVariable Long pairId,
            @RequestBody(required = false) TeacherAiExplanationCreateRequest request
    ) {
        TeacherAiExplanationCreateRequest body = request == null ? new TeacherAiExplanationCreateRequest() : request;
        AiExplanationMode mode = body.resolveMode();
        boolean includeTeacherNote = body.resolveIncludeTeacherNote();
        return Result.success(teacherPlagiarismJobService.createAiExplanation(
                currentUserService.getCurrentUserId(),
                pairId,
                mode,
                includeTeacherNote
        ));
    }

    @GetMapping("/similarity-pairs/{pairId}/ai-explanations")
    public Result<List<AiExplanation>> listAiExplanations(@PathVariable Long pairId) {
        return Result.success(teacherPlagiarismJobService.listAiExplanations(currentUserService.getCurrentUserId(), pairId));
    }

    @GetMapping("/similarity-pairs/{pairId}/ai-explanations/latest")
    public Result<AiExplanation> getLatestAiExplanation(@PathVariable Long pairId) {
        return Result.success(teacherPlagiarismJobService.getLatestAiExplanation(currentUserService.getCurrentUserId(), pairId));
    }
}
