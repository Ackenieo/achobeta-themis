package com.achobeta.themis.domain.review.service;

import java.util.List;

public interface IFileReviewHistoryService {
    /**
     * 保存文件审查记录
     * @param userId 用户ID
     * @param reviewRecord 审查记录
     */
    void saveReviewRecord(String userId, ReviewRecord reviewRecord);
    
    /**
     * 获取用户的所有文件审查记录
     * @param userId 用户ID
     * @return 审查记录列表
     */
    List<ReviewRecord> getUserReviewRecords(String userId);
    
    /**
     * 获取指定的审查记录详情
     * @param userId 用户ID
     * @param recordId 记录ID
     * @return 审查记录
     */
    ReviewRecord getReviewRecord(String userId, String recordId);
    
    /**
     * 删除审查记录
     * @param userId 用户ID
     * @param recordId 记录ID
     */
    void deleteReviewRecord(String userId, String recordId);
    
    /**
     * 文件审查记录
     */
    class ReviewRecord {
        private String recordId;
        private String fileName;

        private String reviewContent;
        private Long createTime;
        private Long updateTime;
        
        public ReviewRecord() {}
        
        public ReviewRecord(String recordId, String fileName,  String reviewContent, Long createTime, Long updateTime) {
            this.recordId = recordId;
            this.fileName = fileName;

            this.reviewContent = reviewContent;
            this.createTime = createTime;
            this.updateTime = updateTime;
        }

        // Getters and Setters
        public String getRecordId() {
            return recordId;
        }
        
        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        

        

        
        public String getReviewContent() {
            return reviewContent;
        }
        
        public void setReviewContent(String reviewContent) {
            this.reviewContent = reviewContent;
        }
        
        public Long getCreateTime() {
            return createTime;
        }
        
        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }
        
        public Long getUpdateTime() {
            return updateTime;
        }
        
        public void setUpdateTime(Long updateTime) {
            this.updateTime = updateTime;
        }
    }
}

