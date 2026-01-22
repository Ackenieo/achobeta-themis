package com.achobeta.themis.domain.chat.service;

import com.achobeta.themis.domain.user.model.vo.QuestionTitleResponseVO;

import java.util.List;

public interface IChatService {
    /**
     * 搜索问题标题
     * @return
     */
    List<List<QuestionTitleResponseVO>> searchQuestionTitles(Integer userType);

}
