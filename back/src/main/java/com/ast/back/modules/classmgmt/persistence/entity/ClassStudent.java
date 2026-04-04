package com.ast.back.modules.classmgmt.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_class_student")
public class ClassStudent {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer classId;
    private Long studentId;
    private LocalDateTime joinTime;
    
    /**
     * 0: Pending, 1: Approved, 2: Rejected
     */
    private Integer status;
}
