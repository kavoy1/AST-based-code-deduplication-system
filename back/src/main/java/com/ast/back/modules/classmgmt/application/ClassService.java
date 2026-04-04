package com.ast.back.modules.classmgmt.application;

import com.ast.back.modules.classmgmt.persistence.entity.Clazz;
import com.ast.back.modules.user.persistence.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;


import com.baomidou.mybatisplus.core.metadata.IPage;

public interface ClassService extends IService<Clazz> {
    /**
     * 创建班级
     * @param clazz 班级信息
     * @return 是否成功
     */
    boolean createNewClass(Clazz clazz);

    /**
     * 获取老师的班级列表（包含学生人数统计）
     * @param teacherId 老师ID
     * @param page 页码
     * @param size 每页大小
     * @return 班级列表
     */
    IPage<Clazz> getClassesByTeacherId(Long teacherId, int page, int size);

    /**
     * 获取教师端首页统计数据
     * @param teacherId 老师ID
     * @return 统计数据Map
     */
    Map<String, Object> getTeacherStats(Long teacherId);

    /**
     * 删除班级
     * @param id 班级ID
     * @return 是否成功
     */
    boolean deleteClass(Integer id);

    // 新增方法
    List<Map<String, Object>> getStudentsByClassId(Integer classId, Long teacherId);
    boolean removeStudentFromClass(Integer classId, Long studentId, Long teacherId);

    List<Map<String, Object>> getPendingApplications(Long teacherId);
    boolean approveApplication(Integer applicationId);
    boolean rejectApplication(Integer applicationId);

    /**
     * 邀请学生直接加入班级
     * @param classId 班级ID
     * @param studentId 学生ID
     * @return 是否成功
     */
    boolean inviteStudentToClass(Integer classId, Long studentId);

    // 学生端相关方法
    /**
     * 获取学生已加入的班级列表
     * @param studentId 学生ID
     * @return 班级列表
     */
    List<Map<String, Object>> getStudentClasses(Long studentId);

    /**
     * 学生申请加入班级
     * @param studentId 学生ID
     * @param inviteCode 邀请码
     * @return 结果消息
     */
    String joinClass(Long studentId, String inviteCode);
}
