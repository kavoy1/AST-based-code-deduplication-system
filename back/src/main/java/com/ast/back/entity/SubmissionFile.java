package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 提交文件记录。
 */
@Data
@TableName("submission_file")
public class SubmissionFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private String filename;

    /** 实际存储路径（服务端路径） */
    private String storagePath;

    /** 文件内容 SHA-256 */
    private String sha256;

    private Long bytes;

    /** 解析状态：OK / FAILED / SKIPPED 等 */
    private String parseStatus;

    private String parseError;
}