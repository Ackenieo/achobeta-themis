package com.achobeta.themis.common.component.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeBaseQuestionDocument {
    private String id;
    // 数据库问题ID
    private Long questionId;
    private String question;
    private String questionSegmented;
    private Integer count;
    // 所属热点专题
    private String topic;
    // 所属常见场景
    private String caseBackground;
}
