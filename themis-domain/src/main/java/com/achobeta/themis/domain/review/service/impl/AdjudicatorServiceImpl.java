package com.achobeta.themis.domain.review.service.impl;

import com.achobeta.themis.common.agent.service.IAiAdjudicatorService;
import com.achobeta.themis.common.component.MeiliSearchComponent;
import com.achobeta.themis.common.component.entity.QuestionTitleDocument;
import com.achobeta.themis.common.util.IKPreprocessorUtil;
import com.achobeta.themis.domain.review.service.IAdjudicatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.achobeta.themis.common.Constant.QUESTION_TITLE_DOCUMENTS;


@Service
@RequiredArgsConstructor
public class AdjudicatorServiceImpl implements IAdjudicatorService {


    private final MeiliSearchComponent meiliSearchComponent;

    @Autowired
    @Qualifier("adjudicator")
    private IAiAdjudicatorService adjudicatorAgentService;

    @Override
    public void adjudicate(Integer userType, String conversationId, String question) {
        // 参数校验：如果问题为空，直接返回，不进行分类
        if (question == null || question.trim().isEmpty()) {
            return;
        }
        
        List<QuestionTitleDocument> questionTitleDocuments = null;
        try {
            // 使用 segment 方法，如果全是停用词会返回原文
            String segmentedQuery = IKPreprocessorUtil.segment(question, true);
            questionTitleDocuments = meiliSearchComponent.fuzzySearchFromQuestionTitle(QUESTION_TITLE_DOCUMENTS, segmentedQuery, new String[]{"title_segmented"}, 1, QuestionTitleDocument.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!questionTitleDocuments.isEmpty()) {
            QuestionTitleDocument questionTitleDocument = questionTitleDocuments.getFirst();
            meiliSearchComponent.updateCount(QUESTION_TITLE_DOCUMENTS, questionTitleDocument.getId(), questionTitleDocument.getCount() + 1);
        } else {
            // 查找问题上下文并打包为一个字符串
//            String context = "";
//            String chatContext = "用户类型" + userType + ";\n 当前问题：" + question + ";\n 讨论问题的上下文信息：" + context;
//            String adjudicate = adjudicatorAgentService.chat("adjudicate_" + conversationId, chatContext);
//            System.out.println(adjudicate);
            String adjudicate = adjudicatorAgentService.chat("adjudicate_" + conversationId, "UserType" + userType + ";\n 当前问题：" + question);
            // 提取 primaryTag
            int primaryTag = Integer.parseInt(adjudicate.replaceAll("\\D+", ""));
            try {
                meiliSearchComponent.addDocuments(QUESTION_TITLE_DOCUMENTS, List.of(QuestionTitleDocument.builder()
                        .id(UUID.randomUUID().toString())
                        .title(question)
                        .titleSegmented(IKPreprocessorUtil.segment(question, true))
                        .primaryTag(primaryTag)
                        .count(1)
                        .createTime(LocalDateTime.now())
                        .build()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

