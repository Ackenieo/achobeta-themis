package com.achobeta.themis.common.component.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LawDocument {
    /**
     * 法条唯一ID (regulation_id)
     */
    private Long id;
    
    /**
     * 法律分类名称 (law_name)
     */
    private String lawName;
    
    /**
     * 法律分类ID (law_category_id)
     */
    private Long lawCategoryId;
    
    /**
     * 条款号 (article_number)
     */
    private Integer articleNumber;
    
    /**
     * 法条原文 (original_text)
     */
    private String originalText;
    
    /**
     * 法条原文分词后的文本（用于中文搜索）
     */
    private String originalTextSegmented;
    
    /**
     * 发布年月日
     */
    private String issueYear;
    
    /**
     * 法律类型：1-国家法规，0-地方法规
     */
    private Integer categoryType;
    
    /**
     * 关联法条ID列表
     */
    private List<Integer> relatedRegulationIds;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
