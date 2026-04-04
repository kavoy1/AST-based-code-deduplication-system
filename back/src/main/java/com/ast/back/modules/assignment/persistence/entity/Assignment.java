package com.ast.back.modules.assignment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业模板主表。
 */
@Data
@TableName("assignment")
public class Assignment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 发布教师。
     */
    private Long teacherId;

    /**
     * 兼容旧版单班级字段，新的多班级关联使用 assignment_class。
     */
    private Integer clazzId;

    private String title;

    private String language;

    private String description;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /**
     * 兼容旧版字段。
     */
    private LocalDateTime deadline;

    private String status;

    private Integer allowResubmit;

    private Integer allowLateSubmit;

    private Integer maxFiles;

    private Integer materialCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
