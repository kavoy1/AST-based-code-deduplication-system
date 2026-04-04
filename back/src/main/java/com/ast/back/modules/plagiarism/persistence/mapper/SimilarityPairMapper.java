package com.ast.back.modules.plagiarism.persistence.mapper;

import com.ast.back.modules.plagiarism.persistence.entity.SimilarityPair;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SimilarityPairMapper extends BaseMapper<SimilarityPair> {

    int batchInsert(@Param("pairs") List<SimilarityPair> pairs);
}