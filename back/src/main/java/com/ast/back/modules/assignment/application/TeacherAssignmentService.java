package com.ast.back.modules.assignment.application;

import com.ast.back.modules.assignment.persistence.entity.Assignment;
import com.ast.back.modules.assignment.persistence.entity.AssignmentMaterial;
import com.ast.back.modules.assignment.dto.TeacherAssignmentCreateRequest;
import com.ast.back.modules.assignment.dto.TeacherAssignmentDetailView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentPageView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentReopenRequest;
import com.ast.back.modules.assignment.dto.TeacherAssignmentStatsView;
import com.ast.back.modules.assignment.dto.TeacherAssignmentSummaryView;
import com.ast.back.modules.submission.dto.TeacherSubmissionOverview;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TeacherAssignmentService {

    Assignment createAssignment(Long teacherId, TeacherAssignmentCreateRequest request);

    TeacherAssignmentPageView listAssignments(Long teacherId, String keyword, String status, Integer page, Integer size);

    TeacherAssignmentDetailView getAssignmentDetail(Long teacherId, Long assignmentId);

    Assignment updateAssignment(Long teacherId, Long assignmentId, TeacherAssignmentCreateRequest request);

    Assignment reopenAssignment(Long teacherId, Long assignmentId, TeacherAssignmentReopenRequest request);

    Assignment closeAssignmentNow(Long teacherId, Long assignmentId);

    Assignment archiveAssignment(Long teacherId, Long assignmentId);

    Assignment restoreArchivedAssignment(Long teacherId, Long assignmentId);

    void deleteAssignment(Long teacherId, Long assignmentId);

    List<AssignmentMaterial> uploadMaterials(Long teacherId, Long assignmentId, List<MultipartFile> files);

    AssignmentMaterial getMaterial(Long teacherId, Long assignmentId, Long materialId);

    Resource loadMaterialResource(Long teacherId, Long assignmentId, Long materialId);

    void deleteMaterial(Long teacherId, Long assignmentId, Long materialId);

    TeacherAssignmentStatsView getAssignmentStats(Long teacherId, Long assignmentId, Integer classId);

    List<TeacherSubmissionOverview> listAssignmentSubmissions(Long teacherId, Long assignmentId);
}
