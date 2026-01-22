package com.achobeta.themis.infrastructure.chat.mapper;

import com.achobeta.themis.domain.chat.model.entity.Questions;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Questions> {
}
