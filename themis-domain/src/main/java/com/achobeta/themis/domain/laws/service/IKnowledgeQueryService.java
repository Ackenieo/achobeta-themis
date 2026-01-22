package com.achobeta.themis.domain.laws.service;

import com.achobeta.themis.domain.laws.model.vo.KnowledgeQueryRequestVO;

import java.util.Map;

/**
 * 知识库查询Service接口
 */
public interface IKnowledgeQueryService {
    
    /**
     * 查询知识库并返回相关法律文档和AI解析
     * @param userId 用户ID
     * @param request 查询请求
     * @return 查询响应
     */
//    KnowledgeQueryResponseVO queryKnowledge(String userId, KnowledgeQueryRequestVO request) throws Exception;

     /**
     * 查询知识库并返回相关法律文档ID和原始文本
     * @param request 查询请求
     * @return 法律文档ID到原始文本的映射
     */
    Map<Long, String> queryKnowledgeId(KnowledgeQueryRequestVO request) throws Exception;
}
