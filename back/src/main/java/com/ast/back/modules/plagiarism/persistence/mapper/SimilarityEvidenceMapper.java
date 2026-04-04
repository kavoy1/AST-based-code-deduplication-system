package com.ast.back.modules.plagiarism.persistence.mapper;

import com.ast.back.modules.plagiarism.persistence.entity.SimilarityEvidence;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SimilarityEvidenceMapper extends BaseMapper<SimilarityEvidence> {

    int batchInsert(@Param("evidences") List<SimilarityEvidence> evidences);
}