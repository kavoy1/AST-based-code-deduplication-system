package com.ast.back.mapper;

import com.ast.back.entity.SimilarityPair;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SimilarityPairMapper extends BaseMapper<SimilarityPair> {

    int batchInsert(@Param("pairs") List<SimilarityPair> pairs);
}