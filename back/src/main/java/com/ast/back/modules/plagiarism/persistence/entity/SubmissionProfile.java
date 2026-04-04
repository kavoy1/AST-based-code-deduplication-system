package com.ast.back.modules.plagiarism.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交级 AST 签名画像缓存。
 */
@Data
@TableName("submission_profile")
public class SubmissionProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private String algoVersion;

    private Integer totalNodes;

    private String signatureCountsJson;

    private String parseFailuresJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
