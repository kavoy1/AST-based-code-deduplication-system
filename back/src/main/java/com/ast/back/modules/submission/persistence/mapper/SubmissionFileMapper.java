package com.ast.back.modules.submission.persistence.mapper;

import com.ast.back.modules.submission.persistence.entity.SubmissionFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubmissionFileMapper extends BaseMapper<SubmissionFile> {

    List<SubmissionFile> selectBySubmissionIds(@Param("submissionIds") List<Long> submissionIds);
}