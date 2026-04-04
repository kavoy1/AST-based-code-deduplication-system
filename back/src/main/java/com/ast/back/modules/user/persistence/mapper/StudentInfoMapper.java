package com.ast.back.modules.user.persistence.mapper;

import com.ast.back.modules.user.persistence.entity.StudentInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudentInfoMapper extends BaseMapper<StudentInfo> {
    
    // Custom query to join users and student_info
    @Select("<script>" +
            "SELECT u.id as user_id, u.nickname, u.username, u.email, " +
            "si.student_number, si.school, si.college, si.major, si.class_name " +
            "FROM users u " +
            "LEFT JOIN sys_student_info si ON u.id = si.user_id " +
            "WHERE u.role = 'STUDENT' " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (u.nickname LIKE CONCAT('%', #{keyword}, '%') OR si.student_number LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='college != null and college != \"\"'>" +
            "AND si.college = #{college} " +
            "</if>" +
            "<if test='className != null and className != \"\"'>" +
            "AND si.class_name = #{className} " +
            "</if>" +
            "ORDER BY si.student_number ASC" +
            "</script>")
    List<Map<String, Object>> getStudentList(@Param("keyword") String keyword, 
                                            @Param("college") String college, 
                                            @Param("className") String className);

    @Select("SELECT DISTINCT college FROM sys_student_info WHERE college IS NOT NULL AND college != ''")
    List<String> getDistinctColleges();

    @Select("<script>" +
            "SELECT DISTINCT class_name FROM sys_student_info WHERE class_name IS NOT NULL AND class_name != '' " +
            "<if test='college != null and college != \"\"'>" +
            "AND college = #{college} " +
            "</if>" +
            "</script>")
    List<String> getDistinctClasses(@Param("college") String college);
}
