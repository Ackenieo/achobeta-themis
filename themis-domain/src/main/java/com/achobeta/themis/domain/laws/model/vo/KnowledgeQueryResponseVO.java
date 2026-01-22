package com.achobeta.themis.domain.laws.model.vo;

import com.achobeta.themis.common.component.entity.LawDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库查询响应VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeQueryResponseVO {
    
    /**
     * 对话ID
     */
    private String conversationId;
    
    /**
     * 法律文档及其对应的AI解析列表
     */
    private List<LawDocumentWithAnalysis> lawDocumentsWithAnalysis;
    
    /**
     * 查询时间戳
     */
    private Long timestamp;
    
    /**
     * 法律文档及其AI解析的组合类
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LawDocumentWithAnalysis {
        /**
         * 法律文档
         */
        private LawDocument lawDocument;
        
        /**
         * 该法条的AI解析结果（根据prompt-zhishiku.txt格式化）
         */
        private String aiAnalysis;
    }
}
