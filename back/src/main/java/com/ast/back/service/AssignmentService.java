package com.ast.back.service;

import com.ast.back.entity.Assignment;

import java.util.List;

public interface AssignmentService {

    List<Assignment> listAssignmentsForStudent(Long studentId, Integer classId);

    Assignment getStudentAssignmentDetail(Long studentId, Long assignmentId);
}
