package com.achobeta.themis.domain.laws.model.entity;

import com.achobeta.themis.common.annotation.FieldDesc;
import com.achobeta.themis.domain.user.handler.JsonListTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "law_categories", autoResultMap = true)
public class LawCategory {
    @TableId(value = "law_id", type = IdType.AUTO)
    @FieldDesc(name = "法律分类ID")
    private Long lawId;
    @FieldDesc(name = "法律分类名称")
    private String lawName;
    @FieldDesc(name = "关联法条ID列表")
    @TableField(
            value = "related_regulation_ids",
            typeHandler = JsonListTypeHandler.class,
            jdbcType = JdbcType.OTHER
    )
    private List<Integer> relatedRegulationIds;
    @FieldDesc(name = "法律类型")
    private Integer categoryType;
    @FieldDesc(name = "创建时间")
    private LocalDateTime createTime;
    @FieldDesc(name = "更新时间")
    private LocalDateTime updateTime;
}
