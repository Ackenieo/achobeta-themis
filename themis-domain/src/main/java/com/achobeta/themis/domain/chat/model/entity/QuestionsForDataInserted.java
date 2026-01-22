package com.achobeta.themis.domain.chat.model.entity;

import com.achobeta.themis.common.annotation.FieldDesc;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("questions_for_data_inserted")
@Data
@Builder
public class QuestionsForDataInserted {
    @FieldDesc(name = "问题ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @FieldDesc(name = "问题内容")
    private String questionContent;
    @FieldDesc(name = "创建时间")
    private LocalDateTime createTime;
    @FieldDesc(name = "更新时间")
    private LocalDateTime updateTime;
}
