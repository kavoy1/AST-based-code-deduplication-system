package com.ast.back.modules.user.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_student_info")
public class StudentInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联的用户ID
     */
    private Long userId;

    /**
     * 学号
     */
    private String studentNumber;

    /**
     * 学校
     */
    private String school;

    /**
     * 学院
     */
    private String college;

    /**
     * 专业
     */
    private String major;

    /**
     * 行政班级
     */
    private String className;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
