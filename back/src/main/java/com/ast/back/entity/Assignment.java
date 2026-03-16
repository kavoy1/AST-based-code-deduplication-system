package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业实体（以班级为单位发布）。
 */
@Data
@TableName("assignment")
public class Assignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级 ID（Clazz.id） */
    private Integer clazzId;

    /** 作业标题 */
    private String title;

    /** 作业描述 */
    private String description;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 是否允许多次提交（1 是 / 0 否） */
    private Integer allowResubmit;

    /** 单次提交最多文件数 */
    private Integer maxFiles;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}