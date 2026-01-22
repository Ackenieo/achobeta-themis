package com.achobeta.themis.api.review.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileReviewRecordVO {
    private String recordId;
    private String fileName;

    private String reviewContent;
    private Long createTime;
    private Long updateTime;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewRecordListVO {
        private List<FileReviewRecordVO> records;
    }
}

