package com.ast.back.modules.feedback.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private String content;

    /**
     * PENDING: 待处理, RESOLVED: 已解决
     */
    private String status;
    
    private String reply; // 管理员回复
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String submitterName; // 用于前端显示提交人姓名
    
    @TableField(exist = false)
    private String submitterUid; // 用于前端显示提交人UID
    
    @TableField(exist = false)
    private String submitterRole; // 用于前端显示提交人角色
}
