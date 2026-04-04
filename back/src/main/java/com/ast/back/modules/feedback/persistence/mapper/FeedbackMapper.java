package com.ast.back.modules.feedback.persistence.mapper;

import com.ast.back.modules.feedback.persistence.entity.Feedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
