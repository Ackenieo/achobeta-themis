package com.achobeta.themis.infrastructure.review.mapper;

import com.achobeta.themis.domain.review.model.entity.FileRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper extends BaseMapper<FileRecord> {
}
