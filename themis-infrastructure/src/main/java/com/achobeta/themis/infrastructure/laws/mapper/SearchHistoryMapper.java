package com.achobeta.themis.infrastructure.laws.mapper;

import com.achobeta.themis.domain.laws.model.entity.KnowledgeBaseSearchHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SearchHistoryMapper extends BaseMapper<KnowledgeBaseSearchHistory> {
}
