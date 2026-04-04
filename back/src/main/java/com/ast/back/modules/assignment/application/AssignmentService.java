package com.ast.back.modules.assignment.application;

import com.ast.back.modules.assignment.persistence.entity.Assignment;

import java.util.List;

public interface AssignmentService {

    List<Assignment> listAssignmentsForStudent(Long studentId, Integer classId);

    Assignment getStudentAssignmentDetail(Long studentId, Long assignmentId);
}
