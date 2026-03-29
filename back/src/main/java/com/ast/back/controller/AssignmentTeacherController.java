package com.ast.back.controller;

import com.ast.back.common.Result;
import com.ast.back.entity.Assignment;
import com.ast.back.entity.AssignmentMaterial;
import com.ast.back.service.CurrentUserService;
import com.ast.back.service.TeacherAssignmentService;
import com.ast.back.service.dto.TeacherAssignmentCreateRequest;
import com.ast.back.service.dto.TeacherAssignmentDetailView;
import com.ast.back.service.dto.TeacherAssignmentReopenRequest;
import com.ast.back.service.dto.TeacherAssignmentStatsView;
import com.ast.back.service.dto.TeacherAssignmentSummaryView;
import com.ast.back.service.dto.TeacherSubmissionOverview;
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
    public Result<List<TeacherAssignmentSummaryView>> listAssignments(
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
