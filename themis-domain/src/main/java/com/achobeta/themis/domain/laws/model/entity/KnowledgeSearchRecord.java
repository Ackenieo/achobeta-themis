package com.achobeta.themis.domain.laws.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库搜索历史记录实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeSearchRecord {
    
    /**
     * 记录ID
     */
    private String recordId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 搜索问题
     */
    private String question;
    
    /**
     * 对话ID
     */
    private String conversationId;
    
    /**
     * 法律分类ID
     */
    private Integer lawCategoryId;
    
    /**
     * 搜索结果数量
     */
    private Integer resultCount;
    
    /**
     * 创建时间（时间戳）
     */
    private Long createTime;
    
    /**
     * 更新时间（时间戳）
     */
    private Long updateTime;
}
