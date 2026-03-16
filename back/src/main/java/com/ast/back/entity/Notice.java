package com.ast.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notices")
public class Notice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    
    /**
     * 接收者ID，null表示全局公告
     */
    private Long receiverId;
    
    /**
     * 是否已读：0未读，1已读
     */
    private Integer isRead;
    
    /**
     * 类型：SYSTEM, ANNOUNCEMENT
     */
    private String type;
    
    private LocalDateTime createTime;
}
