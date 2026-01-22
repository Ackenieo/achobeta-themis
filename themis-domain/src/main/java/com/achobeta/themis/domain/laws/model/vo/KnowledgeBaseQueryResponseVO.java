package com.achobeta.themis.domain.laws.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KnowledgeBaseQueryResponseVO {
    // 法律名称
    private String lawName;
    // 法条id
    private Integer regulationId;
    // 法条内容
    private String regulationContent;
    // 法律翻译内容
    private String aiTranslateContent;
    // 关联法条
    private List<String> relatedRegulationList;
    // 相关案例
    private List<RelevantCases> relevantCases;
    // 相关问题
    private List<String> relevantQuestions;
    // 法律条款号
    private Integer articleNumber;
    // 法律总条款数
    private Integer totalArticles;
    // 发布年份
    private String issueYear;

    @Data
    @Builder
    public static class RelevantCases{
        private String caseContent;
        private String caseLink;
    }
}
