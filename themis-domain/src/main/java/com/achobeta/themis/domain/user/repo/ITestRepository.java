package com.achobeta.themis.domain.user.repo;

import com.achobeta.themis.domain.chat.model.entity.QuestionsForDataInserted;

import java.util.List;

public interface ITestRepository {
     List<QuestionsForDataInserted> queryQuestions();

}
