package com.achobeta.themis.domain.user.service;

import com.achobeta.themis.domain.chat.model.entity.QuestionsForDataInserted;

import java.util.List;

public interface ITestService {
    List<QuestionsForDataInserted> queryQuestions();

}
