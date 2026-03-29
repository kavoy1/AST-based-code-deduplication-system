package com.ast.back.service;

import com.ast.back.entity.Assignment;
import com.ast.back.entity.AssignmentMaterial;
import com.ast.back.service.dto.TeacherAssignmentCreateRequest;
import com.ast.back.service.dto.TeacherAssignmentDetailView;
import com.ast.back.service.dto.TeacherAssignmentReopenRequest;
import com.ast.back.service.dto.TeacherAssignmentStatsView;
import com.ast.back.service.dto.TeacherAssignmentSummaryView;
import com.ast.back.service.dto.TeacherSubmissionOverview;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TeacherAssignmentService {

    Assignment createAssignment(Long teacherId, TeacherAssignmentCreateRequest request);

    List<TeacherAssignmentSummaryView> listAssignments(Long teacherId, String keyword, String status, Integer page, Integer size);

    TeacherAssignmentDetailView getAssignmentDetail(Long teacherId, Long assignmentId);

    Assignment updateAssignment(Long teacherId, Long assignmentId, TeacherAssignmentCreateRequest request);

    Assignment reopenAssignment(Long teacherId, Long assignmentId, TeacherAssignmentReopenRequest request);

    List<AssignmentMaterial> uploadMaterials(Long teacherId, Long assignmentId, List<MultipartFile> files);

    AssignmentMaterial getMaterial(Long teacherId, Long assignmentId, Long materialId);

    Resource loadMaterialResource(Long teacherId, Long assignmentId, Long materialId);

    void deleteMaterial(Long teacherId, Long assignmentId, Long materialId);

    TeacherAssignmentStatsView getAssignmentStats(Long teacherId, Long assignmentId, Integer classId);

    List<TeacherSubmissionOverview> listAssignmentSubmissions(Long teacherId, Long assignmentId);
}
