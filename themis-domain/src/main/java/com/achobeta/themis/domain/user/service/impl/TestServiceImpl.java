package com.achobeta.themis.domain.user.service.impl;

import com.achobeta.themis.domain.chat.model.entity.QuestionsForDataInserted;
import com.achobeta.themis.domain.user.repo.ITestRepository;
import com.achobeta.themis.domain.user.service.ITestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements ITestService {
    private final ITestRepository testRepository;

    @Override
    public List<QuestionsForDataInserted> queryQuestions() {
        return testRepository.queryQuestions();
    }
}
