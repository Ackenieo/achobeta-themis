package com.achobeta.themis.domain.laws.service;

import com.achobeta.themis.domain.laws.model.entity.KnowledgeSearchRecord;

import java.util.List;

/**
 * 知识库搜索历史服务接口
 */
public interface IKnowledgeSearchHistoryService {
    
    /**
     * 保存知识库搜索记录
     * @param record 搜索记录
     */
    void saveSearchRecord(KnowledgeSearchRecord record);
    
    /**
     * 获取用户的所有搜索记录
     * @param userId 用户ID
     * @return 搜索记录列表（按时间倒序）
     */
    List<KnowledgeSearchRecord> getUserSearchRecords(String userId);
    
    /**
     * 获取用户的搜索记录（分页）
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 搜索记录列表（按时间倒序）
     */
    List<KnowledgeSearchRecord> getUserSearchRecords(String userId, int offset, int limit);
    
    /**
     * 获取指定的搜索记录详情
     * @param userId 用户ID
     * @param recordId 记录ID
     * @return 搜索记录
     */
    KnowledgeSearchRecord getSearchRecord(String userId, String recordId);
    
    /**
     * 删除搜索记录
     * @param userId 用户ID
     * @param recordId 记录ID
     */
    void deleteSearchRecord(String userId, String recordId);
    
    /**
     * 清空用户的所有搜索记录
     * @param userId 用户ID
     */
    void clearUserSearchRecords(String userId);
}
