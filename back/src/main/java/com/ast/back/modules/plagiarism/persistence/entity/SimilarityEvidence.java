package com.ast.back.modules.plagiarism.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 相似对证据（结构化摘要）。
 */
@Data
@TableName("similarity_evidence")
public class SimilarityEvidence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pairId;

    /** 证据类型，例如 SUBTREE / HOTSPOT / PATTERN */
    private String type;

    /** 面向教师的简短摘要 */
    private String summary;

    /** 权重（越大越重要） */
    private Integer weight;

    /** 结构化载荷（JSON） */
    private String payloadJson;
}