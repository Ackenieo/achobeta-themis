package com.achobeta.themis.domain.laws.model.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库查询请求VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeQueryRequestVO {
    
    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    private String question;
    
    /**
     * 对话ID（可选，用于继续之前的对话）
     */
    private String conversationId;
    
    /**
     * 法律分类ID（可选，用于过滤特定法律）
     */
    private Integer lawCategoryId;
    
    /**
     * 返回结果数量限制（默认5）
     */
    private Integer limit = 5;
}
