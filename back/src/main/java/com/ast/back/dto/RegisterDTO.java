package com.ast.back.dto;

import com.ast.back.entity.StudentInfo;
import com.ast.back.entity.TeacherInfo;
import com.ast.back.entity.User;
import lombok.Data;

@Data
public class RegisterDTO {
    private User user;          // 包含用户名、密码、邮箱、角色等
    private String inviteCode;  // 教师邀请码
    private String emailCode;   // 邮箱验证码
    private StudentInfo studentInfo; // 学生详细信息
    private TeacherInfo teacherInfo; // 教师详细信息
}