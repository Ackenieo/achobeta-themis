package com.achobeta.themis.domain.laws.service.impl;

import com.achobeta.themis.common.redis.service.IRedisService;
import com.achobeta.themis.domain.laws.model.entity.KnowledgeSearchRecord;
import com.achobeta.themis.domain.laws.service.IKnowledgeSearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库搜索历史服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSearchHistoryServiceImpl implements IKnowledgeSearchHistoryService {
    
    private final IRedisService redisService;
    
    // 7天的过期时间（毫秒）
    private static final long SEVEN_DAYS_MILLIS = Duration.ofDays(7).toMillis();
    
    private static final String SEARCH_LIST_KEY_PREFIX = "knowledge_search:list:";
    private static final String SEARCH_DETAIL_KEY_PREFIX = "knowledge_search:detail:";
    
    @Override
    public void saveSearchRecord(KnowledgeSearchRecord record) {
        try {
            String recordId = record.getRecordId();
            String userId = record.getUserId();
            
            // 保存搜索记录详情
            String detailKey = SEARCH_DETAIL_KEY_PREFIX + recordId;
            Map<String, String> detailMap = new HashMap<>();
            detailMap.put("recordId", recordId);
            detailMap.put("userId", userId);
            detailMap.put("question", record.getQuestion() != null ? record.getQuestion() : "");
            detailMap.put("conversationId", record.getConversationId() != null ? record.getConversationId() : "");
            detailMap.put("lawCategoryId", record.getLawCategoryId() != null ? String.valueOf(record.getLawCategoryId()) : "");
            detailMap.put("resultCount", record.getResultCount() != null ? String.valueOf(record.getResultCount()) : "0");
            detailMap.put("createTime", String.valueOf(record.getCreateTime()));
            detailMap.put("updateTime", String.valueOf(record.getUpdateTime()));
            
            redisService.addAllToMap(detailKey, detailMap);
            redisService.setMapExpired(detailKey, SEVEN_DAYS_MILLIS);
            
            // 将记录添加到用户的记录列表（使用Set保证唯一性）
            String listKey = SEARCH_LIST_KEY_PREFIX + userId;
            redisService.addToSet(listKey, recordId);
            redisService.setSetExpired(listKey, SEVEN_DAYS_MILLIS);
            
            log.info("用户 {} 的知识库搜索记录 {} 已保存", userId, recordId);
        } catch (Exception e) {
            log.error("保存知识库搜索记录失败", e);
            throw new RuntimeException("保存知识库搜索记录失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<KnowledgeSearchRecord> getUserSearchRecords(String userId) {
        String listKey = SEARCH_LIST_KEY_PREFIX + userId;
        Set<String> recordIds = redisService.getSetMembers(listKey);
        
        if (recordIds == null || recordIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return recordIds.stream()
                .map(recordId -> getSearchRecord(userId, recordId))
                .filter(Objects::nonNull)
                .sorted((a, b) -> Long.compare(b.getUpdateTime(), a.getUpdateTime())) // 按更新时间倒序
                .collect(Collectors.toList());
    }
    
    @Override
    public List<KnowledgeSearchRecord> getUserSearchRecords(String userId, int offset, int limit) {
        List<KnowledgeSearchRecord> allRecords = getUserSearchRecords(userId);
        
        if (allRecords.isEmpty() || offset >= allRecords.size()) {
            return Collections.emptyList();
        }
        
        int toIndex = Math.min(offset + limit, allRecords.size());
        return allRecords.subList(offset, toIndex);
    }
    
    @Override
    public KnowledgeSearchRecord getSearchRecord(String userId, String recordId) {
        // 验证记录是否属于该用户
        String listKey = SEARCH_LIST_KEY_PREFIX + userId;
        if (!redisService.isSetMember(listKey, recordId)) {
            log.warn("用户 {} 尝试访问不属于自己的搜索记录 {}", userId, recordId);
            return null;
        }
        
        String detailKey = SEARCH_DETAIL_KEY_PREFIX + recordId;
        Map<String, String> detailMap = redisService.getMapToJavaMap(detailKey);
        
        if (detailMap == null || detailMap.isEmpty()) {
            return null;
        }
        
        KnowledgeSearchRecord record = new KnowledgeSearchRecord();
        record.setRecordId(detailMap.get("recordId"));
        record.setUserId(detailMap.get("userId"));
        record.setQuestion(detailMap.get("question"));
        record.setConversationId(detailMap.get("conversationId"));
        
        String lawCategoryIdStr = detailMap.get("lawCategoryId");
        if (lawCategoryIdStr != null && !lawCategoryIdStr.isEmpty()) {
            try {
                record.setLawCategoryId(Integer.parseInt(lawCategoryIdStr));
            } catch (NumberFormatException e) {
                log.warn("解析lawCategoryId失败: {}", lawCategoryIdStr);
            }
        }
        
        String resultCountStr = detailMap.get("resultCount");
        if (resultCountStr != null && !resultCountStr.isEmpty()) {
            try {
                record.setResultCount(Integer.parseInt(resultCountStr));
            } catch (NumberFormatException e) {
                log.warn("解析resultCount失败: {}", resultCountStr);
            }
        }
        
        if (detailMap.get("createTime") != null) {
            try {
                record.setCreateTime(Long.parseLong(detailMap.get("createTime")));
            } catch (NumberFormatException e) {
                log.warn("解析createTime失败: {}", detailMap.get("createTime"));
            }
        }
        
        if (detailMap.get("updateTime") != null) {
            try {
                record.setUpdateTime(Long.parseLong(detailMap.get("updateTime")));
            } catch (NumberFormatException e) {
                log.warn("解析updateTime失败: {}", detailMap.get("updateTime"));
            }
        }
        
        return record;
    }
    
    @Override
    public void deleteSearchRecord(String userId, String recordId) {
        // 从用户的记录列表中移除
        String listKey = SEARCH_LIST_KEY_PREFIX + userId;
        redisService.removeFromSet(listKey, recordId);
        
        // 删除记录详情
        String detailKey = SEARCH_DETAIL_KEY_PREFIX + recordId;
        redisService.remove(detailKey);
        
        log.info("用户 {} 删除知识库搜索记录 {}", userId, recordId);
    }
    
    @Override
    public void clearUserSearchRecords(String userId) {
        String listKey = SEARCH_LIST_KEY_PREFIX + userId;
        Set<String> recordIds = redisService.getSetMembers(listKey);
        
        if (recordIds != null && !recordIds.isEmpty()) {
            // 删除所有记录详情
            for (String recordId : recordIds) {
                String detailKey = SEARCH_DETAIL_KEY_PREFIX + recordId;
                redisService.remove(detailKey);
            }
        }
        
        // 删除用户的记录列表
        redisService.remove(listKey);
        
        log.info("用户 {} 清空所有知识库搜索记录", userId);
    }
}
