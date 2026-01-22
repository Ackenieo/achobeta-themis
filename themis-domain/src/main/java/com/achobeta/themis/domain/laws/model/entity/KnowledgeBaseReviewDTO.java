package com.achobeta.themis.domain.laws.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class KnowledgeBaseReviewDTO {
    String lawName;
    String originalText;
    String aiTranslation;
    String relatedRegulationList;
    String relevantCases;
    String relevantQuestions;
    Integer articleNumber;
    Integer totalArticles;
    String issueYear;
}
