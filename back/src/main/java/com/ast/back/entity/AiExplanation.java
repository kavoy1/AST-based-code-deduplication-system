package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 解释（与相似对绑定，缓存）。
 */
@Data
@TableName("ai_explanation")
public class AiExplanation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pairId;

    private String model;

    /** SUCCESS / FAILED */
    private String status;

    private Integer latencyMs;

    private String result;

    private String errorMsg;

    private LocalDateTime createTime;
}