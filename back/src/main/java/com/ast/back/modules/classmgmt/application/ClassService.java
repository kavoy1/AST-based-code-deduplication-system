package com.ast.back.modules.classmgmt.application;

import com.ast.back.modules.classmgmt.persistence.entity.Clazz;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface ClassService extends IService<Clazz> {

    boolean createNewClass(Clazz clazz);

    IPage<Clazz> getClassesByTeacherId(Long teacherId, int page, int size);

    Map<String, Object> getTeacherStats(Long teacherId);

    boolean deleteClass(Integer id, Long teacherId);

    List<Map<String, Object>> getStudentsByTeacherId(Long teacherId);

    List<Map<String, Object>> getStudentsByClassId(Integer classId, Long teacherId);

    boolean removeStudentFromClass(Integer classId, Long studentId, Long teacherId);

    List<Map<String, Object>> getPendingApplications(Long teacherId);

    boolean approveApplication(Integer applicationId);

    boolean rejectApplication(Integer applicationId);

    boolean inviteStudentToClass(Integer classId, Long studentId);

    List<Map<String, Object>> getStudentClasses(Long studentId);

    String joinClass(Long studentId, String inviteCode);
}
