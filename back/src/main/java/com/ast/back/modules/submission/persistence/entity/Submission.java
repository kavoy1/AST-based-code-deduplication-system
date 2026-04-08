package com.ast.back.modules.submission.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生当前作业提交。
 */
@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Integer classId;

    private Long studentId;

    private LocalDateTime submitTime;

    private Integer isValid;

    private Integer parseOkFiles;

    private Integer totalFiles;

    private Integer isLate;

    private LocalDateTime deadlineAt;

    private Integer isLatest;

    private Integer version;
}
