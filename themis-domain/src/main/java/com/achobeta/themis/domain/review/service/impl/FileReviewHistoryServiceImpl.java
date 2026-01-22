package com.achobeta.themis.domain.review.service.impl;

import com.achobeta.themis.common.redis.service.IRedisService;
import com.achobeta.themis.domain.review.service.IFileReviewHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileReviewHistoryServiceImpl implements IFileReviewHistoryService {
    
    private final IRedisService redissonService;
    
    // 1天的过期时间（毫秒）
    private static final long ONE_DAY_MILLIS = Duration.ofDays(1).toMillis();
    
    private static final String REVIEW_LIST_KEY_PREFIX = "file_review:list:";
    private static final String REVIEW_DETAIL_KEY_PREFIX = "file_review:detail:";
    
    @Override
    public void saveReviewRecord(String userId, ReviewRecord reviewRecord) {
        try {
            String recordId = reviewRecord.getRecordId();
            
            // 保存审查记录详情
            String detailKey = REVIEW_DETAIL_KEY_PREFIX + recordId;
            Map<String, String> detailMap = new HashMap<>();
            detailMap.put("recordId", recordId);
            detailMap.put("fileName", reviewRecord.getFileName() != null ? reviewRecord.getFileName() : "");

            detailMap.put("reviewContent", reviewRecord.getReviewContent() != null ? reviewRecord.getReviewContent() : "");
            detailMap.put("createTime", String.valueOf(reviewRecord.getCreateTime()));
            detailMap.put("updateTime", String.valueOf(reviewRecord.getUpdateTime()));
            
            redissonService.addAllToMap(detailKey, detailMap);
            redissonService.setMapExpired(detailKey, ONE_DAY_MILLIS);
            
            // 将记录添加到用户的记录列表
            String listKey = REVIEW_LIST_KEY_PREFIX + userId;
            redissonService.addToSet(listKey, recordId);
            redissonService.setSetExpired(listKey, ONE_DAY_MILLIS);
            
            log.info("用户 {} 的审查记录 {} 已保存", userId, recordId);
        } catch (Exception e) {
            log.error("保存审查记录失败", e);
            throw new RuntimeException("保存审查记录失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<ReviewRecord> getUserReviewRecords(String userId) {
        String listKey = REVIEW_LIST_KEY_PREFIX + userId;
        Set<String> recordIds = redissonService.getSetMembers(listKey);
        
        if (recordIds == null || recordIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return recordIds.stream()
                .map(recordId -> getReviewRecord(userId, recordId))
                .filter(Objects::nonNull)
                .sorted((a, b) -> Long.compare(b.getUpdateTime(), a.getUpdateTime())) // 按更新时间倒序
                .collect(Collectors.toList());
    }
    
    @Override
    public ReviewRecord getReviewRecord(String userId, String recordId) {
        // 验证记录是否属于该用户
        String listKey = REVIEW_LIST_KEY_PREFIX + userId;
        if (!redissonService.isSetMember(listKey, recordId)) {
            log.warn("用户 {} 尝试访问不属于自己的审查记录 {}", userId, recordId);
            return null;
        }
        
        String detailKey = REVIEW_DETAIL_KEY_PREFIX + recordId;
        Map<String, String> detailMap = redissonService.getMapToJavaMap(detailKey);
        
        if (detailMap == null || detailMap.isEmpty()) {
            return null;
        }
        
        ReviewRecord record = new ReviewRecord();
        record.setRecordId(detailMap.get("recordId"));
        record.setFileName(detailMap.get("fileName"));

        record.setReviewContent(detailMap.get("reviewContent"));
        
        if (detailMap.get("createTime") != null) {
            record.setCreateTime(Long.parseLong(detailMap.get("createTime")));
        }
        if (detailMap.get("updateTime") != null) {
            record.setUpdateTime(Long.parseLong(detailMap.get("updateTime")));
        }
        
        return record;
    }
    
    @Override
    public void deleteReviewRecord(String userId, String recordId) {
        // 从用户的记录列表中移除
        String listKey = REVIEW_LIST_KEY_PREFIX + userId;
        redissonService.removeFromSet(listKey, recordId);
        
        // 删除记录详情
        String detailKey = REVIEW_DETAIL_KEY_PREFIX + recordId;
        redissonService.remove(detailKey);
        
        log.info("用户 {} 删除审查记录 {}", userId, recordId);
    }
}

