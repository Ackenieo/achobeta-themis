package com.achobeta.themis.common.component.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class QuestionTitleDocument {
        private String id;
        private String title;
        private String titleSegmented;
        private Integer primaryTag;
        private Integer count;
        private LocalDateTime createTime;
}
