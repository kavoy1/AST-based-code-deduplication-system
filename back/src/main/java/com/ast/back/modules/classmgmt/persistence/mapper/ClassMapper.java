package com.ast.back.modules.classmgmt.persistence.mapper;

import com.ast.back.modules.classmgmt.persistence.entity.Clazz;
import com.ast.back.modules.user.persistence.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClassMapper extends BaseMapper<Clazz> {

    // 获取每个班级的学生数量
    IPage<Clazz> selectClassesWithStudentCount(IPage<Clazz> page, @Param("teacherId") Long teacherId);

    // 统计某位老师名下所有班级的学生总数
    Long countStudentsByTeacherId(Long teacherId);

    // 获取班级人数分布
    List<Map<String, Object>> selectClassStudentDistribution(Long teacherId);

    // 获取班级的所有学生
    List<Map<String, Object>> selectStudentsByClassId(@Param("classId") Integer classId, @Param("teacherId") Long teacherId);

    // 从班级移除学生
    int deleteStudentFromClass(@Param("classId") Integer classId, @Param("studentId") Long studentId, @Param("teacherId") Long teacherId);
}