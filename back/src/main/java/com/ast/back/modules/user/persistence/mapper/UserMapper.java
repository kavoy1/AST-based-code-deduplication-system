package com.ast.back.modules.user.persistence.mapper;

import com.ast.back.modules.user.persistence.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
