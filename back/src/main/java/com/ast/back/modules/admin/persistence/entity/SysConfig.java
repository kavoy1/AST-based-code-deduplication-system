package com.ast.back.modules.admin.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(value = "`key`", type = IdType.INPUT)
    private String key;

    private String value;

    private String type;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    @TableField(exist = false)
    private String remark;
}

