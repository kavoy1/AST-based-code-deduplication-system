package com.ast.back.modules.plagiarism.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查重任务（按作业维度）。
 */
@Data
@TableName("plagiarism_job")
public class PlagiarismJob {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    /** QUEUED / RUNNING / DONE / FAILED */
    private String status;

    private String paramsJson;

    @TableField("config_snapshot")
    private String configSnapshot;

    private Integer progressTotal;

    private Integer progressDone;

    private LocalDateTime createTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String errorMsg;
}
