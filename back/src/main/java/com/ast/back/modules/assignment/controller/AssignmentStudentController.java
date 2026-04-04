package com.ast.back.modules.assignment.controller;

import com.ast.back.shared.common.Result;
import com.ast.back.modules.assignment.persistence.entity.Assignment;
import com.ast.back.modules.assignment.application.AssignmentService;
import com.ast.back.shared.security.CurrentUserService;
import com.ast.back.modules.plagiarism.application.StudentPlagiarismSummaryService;
import com.ast.back.modules.plagiarism.dto.StudentPlagiarismSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class AssignmentStudentController {

    private final AssignmentService assignmentService;
    private final CurrentUserService currentUserService;
    private final StudentPlagiarismSummaryService studentPlagiarismSummaryService;

    public AssignmentStudentController(
            AssignmentService assignmentService,
            CurrentUserService currentUserService,
            StudentPlagiarismSummaryService studentPlagiarismSummaryService
    ) {
        this.assignmentService = assignmentService;
        this.currentUserService = currentUserService;
        this.studentPlagiarismSummaryService = studentPlagiarismSummaryService;
    }

    @GetMapping("/classes/{classId}/assignments")
    public Result<List<Assignment>> listAssignments(@PathVariable Integer classId) {
        Long studentId = currentUserService.getCurrentUserId();
        return Result.success(assignmentService.listAssignmentsForStudent(studentId, classId));
    }

    @GetMapping("/assignments/{assignmentId}")
    public Result<Assignment> getAssignment(@PathVariable Long assignmentId) {
        Long studentId = currentUserService.getCurrentUserId();
        return Result.success(assignmentService.getStudentAssignmentDetail(studentId, assignmentId));
    }

    @GetMapping("/assignments/{assignmentId}/plagiarism-summary")
    public Result<StudentPlagiarismSummary> getPlagiarismSummary(@PathVariable Long assignmentId) {
        Long studentId = currentUserService.getCurrentUserId();
        return Result.success(studentPlagiarismSummaryService.getSummary(studentId, assignmentId));
    }
}
