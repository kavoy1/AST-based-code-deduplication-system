package com.ast.back.controller;

import com.ast.back.common.Result;
import com.ast.back.entity.Assignment;
import com.ast.back.service.AssignmentService;
import com.ast.back.service.CurrentUserService;
import com.ast.back.service.StudentPlagiarismSummaryService;
import com.ast.back.service.dto.StudentPlagiarismSummary;
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
