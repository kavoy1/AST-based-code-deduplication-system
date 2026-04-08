package com.ast.back.modules.classmgmt.persistence.mapper;

import com.ast.back.modules.classmgmt.persistence.entity.Clazz;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClassMapper extends BaseMapper<Clazz> {

    IPage<Clazz> selectClassesWithStudentCount(IPage<Clazz> page, @Param("teacherId") Long teacherId);

    Long countStudentsByTeacherId(Long teacherId);

    List<Map<String, Object>> selectClassStudentDistribution(Long teacherId);

    List<Map<String, Object>> selectStudentsByTeacherId(@Param("teacherId") Long teacherId);

    List<Map<String, Object>> selectStudentsByClassId(@Param("classId") Integer classId, @Param("teacherId") Long teacherId);

    List<Clazz> selectClassesByIdsWithStudentCount(@Param("classIds") List<Integer> classIds);

    int deleteStudentFromClass(@Param("classId") Integer classId, @Param("studentId") Long studentId, @Param("teacherId") Long teacherId);
}
