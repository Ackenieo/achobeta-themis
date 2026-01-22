package com.achobeta.themis.domain.laws.model.entity;

import com.achobeta.themis.common.annotation.FieldDesc;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("law_regulations")
public class LawRegulation {
    @TableId(value = "regulation_id", type = IdType.AUTO)
    @FieldDesc(name = "法律条文ID")
    private Long regulationId;
    @FieldDesc(name = "法律分类ID")
    private Long lawCategoryId;
    @FieldDesc(name = "发布年份")
    private String issueYear;
    @FieldDesc(name = "条款号")
    private Integer articleNumber;
    @FieldDesc(name = "法条原文")
    private String originalText;
    @FieldDesc(name = "创建时间")
    private LocalDateTime createTime;
    @FieldDesc(name = "更新时间")
    private LocalDateTime updateTime;
}
