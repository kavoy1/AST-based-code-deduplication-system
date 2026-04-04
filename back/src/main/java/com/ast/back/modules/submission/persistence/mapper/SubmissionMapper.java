package com.ast.back.modules.submission.persistence.mapper;

import com.ast.back.modules.submission.persistence.entity.Submission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {
}