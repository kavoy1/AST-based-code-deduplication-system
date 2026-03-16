package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生对某次作业的提交（支持版本）。
 */
@Data
@TableName("submission")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long studentId;

    /** 版本号（从 1 开始递增） */
    private Integer version;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 是否为最新提交（1 是 / 0 否） */
    private Integer isLatest;

    /** 是否为有效提交（1 是 / 0 否） */
    private Integer isValid;

    /** AST 解析成功的文件数 */
    private Integer parseOkFiles;

    /** 提交文件总数 */
    private Integer totalFiles;
}