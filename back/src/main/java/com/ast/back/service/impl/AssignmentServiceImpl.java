package com.ast.back.service.impl;

import com.ast.back.common.BusinessException;
import com.ast.back.entity.Assignment;
import com.ast.back.entity.AssignmentClass;
import com.ast.back.entity.ClassStudent;
import com.ast.back.mapper.AssignmentClassMapper;
import com.ast.back.mapper.AssignmentMapper;
import com.ast.back.mapper.ClassStudentMapper;
import com.ast.back.service.AssignmentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentMapper assignmentMapper;
    private final AssignmentClassMapper assignmentClassMapper;
    private final ClassStudentMapper classStudentMapper;

    public AssignmentServiceImpl(
            AssignmentMapper assignmentMapper,
            AssignmentClassMapper assignmentClassMapper,
            ClassStudentMapper classStudentMapper
    ) {
        this.assignmentMapper = assignmentMapper;
        this.assignmentClassMapper = assignmentClassMapper;
        this.classStudentMapper = classStudentMapper;
    }

    @Override
    public List<Assignment> listAssignmentsForStudent(Long studentId, Integer classId) {
        requireApprovedMembership(studentId, classId);
        QueryWrapper<AssignmentClass> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("class_id", classId);
        List<Long> assignmentIds = assignmentClassMapper.selectList(relationWrapper).stream()
                .map(AssignmentClass::getAssignmentId)
                .toList();
        if (assignmentIds.isEmpty()) {
            return List.of();
        }

        QueryWrapper<Assignment> assignmentWrapper = new QueryWrapper<>();
        assignmentWrapper.in("id", assignmentIds).orderByDesc("end_at").orderByDesc("create_time");
        return assignmentMapper.selectList(assignmentWrapper);
    }

    @Override
    public Assignment getStudentAssignmentDetail(Long studentId, Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        QueryWrapper<AssignmentClass> relationWrapper = new QueryWrapper<>();
        relationWrapper.eq("assignment_id", assignmentId);
        List<AssignmentClass> relations = assignmentClassMapper.selectList(relationWrapper);
        boolean hasAccess = relations.stream().anyMatch(relation -> hasApprovedMembership(studentId, relation.getClassId()));
        if (!hasAccess) {
            throw new BusinessException(403, "无权访问该作业");
        }
        return assignment;
    }

    private void requireApprovedMembership(Long studentId, Integer classId) {
        if (!hasApprovedMembership(studentId, classId)) {
            throw new BusinessException(403, "无权访问该班级作业");
        }
    }

    private boolean hasApprovedMembership(Long studentId, Integer classId) {
        QueryWrapper<ClassStudent> wrapper = new QueryWrapper<>();
        wrapper.eq("student_id", studentId).eq("class_id", classId).eq("status", 1);
        return classStudentMapper.selectOne(wrapper) != null;
    }
}
