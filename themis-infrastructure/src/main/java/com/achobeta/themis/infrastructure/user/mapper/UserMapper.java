package com.achobeta.themis.infrastructure.user.mapper;

import com.achobeta.themis.domain.user.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Aseubel
 * @date 2025/10/27 上午2:11
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
