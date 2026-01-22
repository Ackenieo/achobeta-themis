package com.achobeta.themis.domain.laws.repo;

import com.achobeta.themis.common.component.entity.LawDocument;

import java.util.List;

/**
 * 知识库查询Repository接口
 */
public interface IKnowledgeQueryRepository {
    
    /**
     * 根据查询关键词搜索法律文档
     * @param query 查询关键词（已分词）
     * @param lawCategoryId 法律分类ID（可选）
     * @param limit 返回数量限制
     * @return 法律文档列表
     */
    List<LawDocument> searchLawDocuments(String query, Integer lawCategoryId, Integer limit) throws Exception;
}
