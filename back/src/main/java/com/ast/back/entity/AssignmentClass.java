package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业与班级关联。
 */
@Data
@TableName("assignment_class")
public class AssignmentClass {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Integer classId;

    private String publishStatus;

    private LocalDateTime createTime;
}
