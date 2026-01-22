package com.achobeta.themis.domain.chat.service.impl;

import com.achobeta.themis.common.component.MeiliSearchComponent;
import com.achobeta.themis.common.component.entity.QuestionTitleDocument;
import com.achobeta.themis.common.constants.PrimaryTagEnum;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.domain.user.model.vo.QuestionTitleResponseVO;
import com.achobeta.themis.domain.chat.service.IChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {
    private final MeiliSearchComponent meiliSearchComponent;
    private final String QUESTION_TITLE_DOCUMENTS = "question_title_documents";

    @Override
    public List<List<QuestionTitleResponseVO>> searchQuestionTitles(Integer userType) {
        List<List<QuestionTitleResponseVO>> secondaryTitleDocuments = new ArrayList<>();
        if(userType == 1) {
            for(int i = 1; i <= 7; i++ )
                addSecondaryTitleDocument(secondaryTitleDocuments, i);
        } else {
            for(int i = 8; i <= 15; i++ )
                addSecondaryTitleDocument(secondaryTitleDocuments, i);
        }
        return secondaryTitleDocuments;
    }

    private void addSecondaryTitleDocument(List<List<QuestionTitleResponseVO>> secondaryTitleDocuments, Integer i) {
        secondaryTitleDocuments.add(meiliSearchComponent.searchFilteredAndSortedDocuments(
                QUESTION_TITLE_DOCUMENTS,
                new String[]{"primaryTag=" + i},
                new String[]{"count:desc"},
                4,
                QuestionTitleDocument.class).stream().map(document -> QuestionTitleResponseVO.builder()
                .title(document.getTitle())
                .primaryTag(String.valueOf(PrimaryTagEnum.of(document.getPrimaryTag()).orElseThrow(() -> new BusinessException("未找到对应的一级标题")).getTag()))
                .build()).toList());
    }
}
