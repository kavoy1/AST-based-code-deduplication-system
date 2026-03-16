package com.ast.back.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO) // 声明主键，并设置为自增
    private Long id;
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    /**真实姓名**/
    private String nickname;

    /**
     * 角色：ADMIN, TEACHER, STUDENT
     */
    private String role;

    private String email;

    /**
     * 创建时间（MyBatis-Plus 会自动匹配数据库的 create_time）
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 状态：1启用, 0禁用
     */
    private Integer status;

    /**
     * 用户编号UID
     */
    private String uid;

    /**
     * 加入班级的时间（非数据库字段）
     */
    @TableField(exist = false)
    private LocalDateTime joinTime;
}

