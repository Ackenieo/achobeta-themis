package com.achobeta.themis.domain.chat.model.entity;

import com.achobeta.themis.common.annotation.FieldDesc;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("questions")
public class Questions {
    @FieldDesc(name = "问题ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @FieldDesc(name = "问题内容")
    private String questionContent;
    @FieldDesc(name = "主题")
    private String topic;
    @FieldDesc(name = "所属场景")
    private String caseBackground;
    @FieldDesc(name = "创建时间")
    private LocalDateTime createTime;
    @FieldDesc(name = "更新时间")
    private LocalDateTime updateTime;
}