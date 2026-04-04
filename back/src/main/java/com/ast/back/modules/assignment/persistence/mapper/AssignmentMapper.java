package com.ast.back.modules.assignment.persistence.mapper;

import com.ast.back.modules.assignment.persistence.entity.Assignment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Assignment Mapper.
 */
@Mapper
public interface AssignmentMapper extends BaseMapper<Assignment> {
}