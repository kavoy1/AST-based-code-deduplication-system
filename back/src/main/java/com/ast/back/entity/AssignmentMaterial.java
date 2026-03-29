package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业资料附件元数据。
 */
@Data
@TableName("assignment_material")
public class AssignmentMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private String storageType;

    private String relativePath;

    private String originalName;

    private String contentType;

    private Long sizeBytes;

    private String sha256;

    private Integer sortNo;

    private Long createdBy;

    private LocalDateTime createdAt;
}
