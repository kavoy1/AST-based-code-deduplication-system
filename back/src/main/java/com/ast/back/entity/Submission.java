package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生某次作业提交版本。
 */
@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Integer classId;

    private Long studentId;

    private Integer version;

    private LocalDateTime submitTime;

    private Integer isLatest;

    private Integer isValid;

    private Integer parseOkFiles;

    private Integer totalFiles;

    private Integer isLate;

    private LocalDateTime deadlineAt;
}
