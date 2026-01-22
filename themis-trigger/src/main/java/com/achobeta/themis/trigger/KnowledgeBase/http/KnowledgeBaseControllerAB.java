package com.achobeta.themis.trigger.KnowledgeBase.http;

import com.achobeta.themis.common.ApiResponse;
import com.achobeta.themis.common.agent.service.IAiKnowledgeService;
import com.achobeta.themis.common.annotation.LoginRequired;
import com.achobeta.themis.common.util.SecurityUtils;
import com.achobeta.themis.domain.laws.model.entity.KnowledgeSearchRecord;
import com.achobeta.themis.domain.chat.service.IConversationHistoryService;
import com.achobeta.themis.domain.laws.service.IKnowledgeQueryService;
import com.achobeta.themis.domain.laws.service.IKnowledgeSearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库查询Controller
 * 提供法律知识库查询和对话历史管理功能
 *
 * @Author: ZGjie20
 * @version: 2.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@LoginRequired
public class KnowledgeBaseControllerAB {
    
    @Autowired
    @Qualifier("Knowledge")
    private IAiKnowledgeService knowledgeService;
    
    private final IKnowledgeQueryService knowledgeQueryService;
    private final IConversationHistoryService conversationHistoryService;
    private final IKnowledgeSearchHistoryService knowledgeSearchHistoryService;

    /**
     * 知识库查询接口（新版）
     * 根据用户问题查询相关法律文档并返回AI解析
     *//*
    @PostMapping("/query")
    public ApiResponse<KnowledgeQueryResponseVO> queryKnowledge(
            @Valid @RequestBody KnowledgeQueryRequestVO request
    ) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            log.info("用户 {} 查询知识库: {}", userId, request.getQuestion());

            KnowledgeQueryResponseVO response = knowledgeQueryService.queryKnowledge(userId, request);

            return ApiResponse.success(response);
        } catch (BusinessException e) {
            throw e;

            log.error("知识库查询失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }*/


    


    /**
     * 获取用户的知识库搜索历史记录
     */
    @GetMapping("/search-history")
    public ApiResponse<List<KnowledgeSearchRecord>> getSearchHistory(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            List<KnowledgeSearchRecord> records = knowledgeSearchHistoryService.getUserSearchRecords(userId, offset, limit);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("获取知识库搜索历史失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 获取指定的搜索历史记录详情
     */
    @PostMapping("/search-history/")
    public ApiResponse<KnowledgeSearchRecord> getSearchHistoryDetail(
             @RequestParam("recordId") String recordId
    ) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            KnowledgeSearchRecord record = knowledgeSearchHistoryService.getSearchRecord(userId, recordId);
            if (record == null) {
                return ApiResponse.error("搜索记录不存在");
            }
            return ApiResponse.success(record);
        } catch (Exception e) {
            log.error("获取搜索历史详情失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 删除指定的搜索历史记录
     */
    @DeleteMapping("/search-history/")
    public ApiResponse<Void> deleteSearchHistory(
            @RequestParam("recordId") String recordId
    ) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            knowledgeSearchHistoryService.deleteSearchRecord(userId, recordId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除搜索历史失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    
    /**
     * 清空用户的所有搜索历史记录
     */
    @DeleteMapping("/search-history")
    public ApiResponse<Void> clearSearchHistory() {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            knowledgeSearchHistoryService.clearUserSearchRecords(userId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("清空搜索历史失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
    

    

    

}
