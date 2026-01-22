package com.achobeta.themis.infrastructure.laws.mapper;

import com.achobeta.themis.domain.laws.model.entity.UserFavoriteRegulation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户收藏法律法规Mapper接口
 * 
 * @author Cascade AI
 * @date 2025/11/20
 */
@Mapper
public interface UserFavoriteRegulationMapper extends BaseMapper<UserFavoriteRegulation> {
}
