package com.achobeta.themis.infrastructure.user.mapper;

import com.achobeta.themis.domain.chat.model.entity.QuestionsForDataInserted;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestMapper extends BaseMapper<QuestionsForDataInserted> {
}