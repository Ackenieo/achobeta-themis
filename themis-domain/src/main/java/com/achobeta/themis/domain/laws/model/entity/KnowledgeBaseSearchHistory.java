package com.achobeta.themis.domain.laws.model.entity;

import com.achobeta.themis.common.annotation.FieldDesc;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("search_histories")
public class KnowledgeBaseSearchHistory {
    @TableId(value = "id", type = IdType.AUTO)
    @FieldDesc(name = "搜索历史ID")
    private Long id;
    @FieldDesc(name = "用户ID")
    private Long userId;
    @FieldDesc(name = "用户问题")
    private String userQuestion;
    @FieldDesc(name = "创建时间")
    private LocalDateTime createTime;
}
