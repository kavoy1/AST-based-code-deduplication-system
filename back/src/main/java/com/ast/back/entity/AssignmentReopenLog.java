package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业重开审计日志。
 */
@Data
@TableName("assignment_reopen_log")
public class AssignmentReopenLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private LocalDateTime oldStartAt;

    private LocalDateTime oldEndAt;

    private LocalDateTime newStartAt;

    private LocalDateTime newEndAt;

    private String reason;

    private Long operatorId;

    private LocalDateTime createdAt;
}
