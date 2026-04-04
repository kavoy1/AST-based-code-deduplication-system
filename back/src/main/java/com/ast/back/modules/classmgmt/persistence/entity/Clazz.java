package com.ast.back.modules.classmgmt.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_class")
public class Clazz {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String className;
    private String courseName;
    private Long teacherId; 
    private String inviteCode; 
    private Integer studentCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}