package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_teacher_info")
public class TeacherInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联的用户ID
     */
    private Long userId;

    /**
     * 教师编号
     */
    private String teacherNumber;

    /**
     * 学校
     */
    private String school;

    /**
     * 学院
     */
    private String college;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
