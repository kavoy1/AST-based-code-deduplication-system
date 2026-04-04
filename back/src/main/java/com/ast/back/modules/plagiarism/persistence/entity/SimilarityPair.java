package com.ast.back.modules.plagiarism.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查重结果：一个相似对（两位学生）。
 */
@Data
@TableName("similarity_pair")
public class SimilarityPair {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;

    private Long studentA;

    private Long studentB;

    /** 0~100 分 */
    private Integer score;

    /** PENDING / CONFIRMED / FALSE_POSITIVE */
    private String status;

    private String teacherNote;

    private LocalDateTime createTime;
}