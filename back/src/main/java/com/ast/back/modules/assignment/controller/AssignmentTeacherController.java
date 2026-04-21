package com.ast.back.modules.assignment.controller;

import com.ast.back.shared.common.Result;
import com.ast.back.modules.assignment.persistence.entity.Assignment;
import com.ast.back.modules.assignment.persistence.entity.AssignmentMaterial;
import com.ast.back.shared.security.CurrentUserService;
import com.ast.back.modules.assignment.application.TeacherAssignmentService;
import com.ast.back.modules.assignment.dto.TeacherAssignmentCreateRequest;
import com.ast.back.modules.assignment.dto.TeacherAssignmentDetailView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentPageView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentReopenRequest;
import com.ast.back.modules.assignment.dto.TeacherAssignmentStatsView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentSummaryView;
import com.ast.back.modules.submission.dto.TeacherSubmissionOverview;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/assignments")
public class AssignmentTeacherController {

    private final TeacherAssignmentService teacherAssignmentService;
    private final CurrentUserService currentUserService;

    public AssignmentTeacherController(TeacherAssignmentService teacherAssignmentService, CurrentUserService currentUserService) {
        this.teacherAssignmentService = teacherAssignmentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public Result<Assignment> createAssignment(@RequestBody TeacherAssignmentCreateRequest request) {
        return Result.success(teacherAssignmentService.createAssignment(currentUserService.getCurrentUserId(), request));
    }

    @GetMapping
    public Result<TeacherAssignmentPageView> listAssignments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return Result.success(teacherAssignmentService.listAssignments(currentUserService.getCurrentUserId(), keyword, status, page, size));
    }

    @GetMapping("/{assignmentId}")
    public Result<TeacherAssignmentDetailView> getAssignment(@PathVariable Long assignmentId) {
        return Result.success(teacherAssignmentService.getAssignmentDetail(currentUserService.getCurrentUserId(), assignmentId));
    }

    @PutMapping("/{assignmentId}")
    public Result<Assignment> updateAssignment(@PathVariable Long assignmentId, @RequestBody TeacherAssignmentCreateRequest request) {
        return Result.success(teacherAssignmentService.updateAssignment(currentUserService.getCurrentUserId(), assignmentId, request));
    }

    @PostMapping("/{assignmentId}/reopen")
    public Result<Assignment> reopenAssignment(@PathVariable Long assignmentId, @RequestBody TeacherAssignmentReopenRequest request) {
        return Result.success(teacherAssignmentService.reopenAssignment(currentUserService.getCurrentUserId(), assignmentId, request));
    }

    @PostMapping("/{assignmentId}/close-now")
    public Result<Assignment> closeAssignmentNow(@PathVariable Long assignmentId) {
        return Result.success(teacherAssignmentService.closeAssignmentNow(currentUserService.getCurrentUserId(), assignmentId));
    }

    @PostMapping("/{assignmentId}/archive")
    public Result<Assignment> archiveAssignment(@PathVariable Long assignmentId) {
        return Result.success(teacherAssignmentService.archiveAssignment(currentUserService.getCurrentUserId(), assignmentId));
    }

    @PostMapping("/{assignmentId}/restore")
    public Result<Assignment> restoreAssignment(@PathVariable Long assignmentId) {
        return Result.success(teacherAssignmentService.restoreArchivedAssignment(currentUserService.getCurrentUserId(), assignmentId));
    }

    @DeleteMapping("/{assignmentId}")
    public Result<Void> deleteAssignment(@PathVariable Long assignmentId) {
        teacherAssignmentService.deleteAssignment(currentUserService.getCurrentUserId(), assignmentId);
        return Result.success();
    }

    @PostMapping("/{assignmentId}/materials")
    public Result<List<AssignmentMaterial>> uploadMaterials(@PathVariable Long assignmentId, @RequestPart("files") List<MultipartFile> files) {
        return Result.success(teacherAssignmentService.uploadMaterials(currentUserService.getCurrentUserId(), assignmentId, files));
    }

    @DeleteMapping("/{assignmentId}/materials/{materialId}")
    public Result<Void> deleteMaterial(@PathVariable Long assignmentId, @PathVariable Long materialId) {
        teacherAssignmentService.deleteMaterial(currentUserService.getCurrentUserId(), assignmentId, materialId);
        return Result.success();
    }

    @GetMapping("/{assignmentId}/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long assignmentId, @PathVariable Long materialId) {
        Long teacherId = currentUserService.getCurrentUserId();
        AssignmentMaterial material = teacherAssignmentService.getMaterial(teacherId, assignmentId, materialId);
        Resource resource = teacherAssignmentService.loadMaterialResource(teacherId, assignmentId, materialId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(material.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : material.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getOriginalName() + "\"")
                .body(resource);
    }

    @GetMapping("/{assignmentId}/submissions")
    public Result<List<TeacherSubmissionOverview>> listSubmissions(@PathVariable Long assignmentId) {
        return Result.success(teacherAssignmentService.listAssignmentSubmissions(currentUserService.getCurrentUserId(), assignmentId));
    }

    @GetMapping("/{assignmentId}/stats")
    public Result<TeacherAssignmentStatsView> getStats(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) Integer classId
    ) {
        return Result.success(teacherAssignmentService.getAssignmentStats(currentUserService.getCurrentUserId(), assignmentId, classId));
    }
}
