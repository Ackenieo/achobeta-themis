package com.achobeta.themis.infrastructure.laws.repo;


import com.achobeta.themis.domain.chat.model.entity.QuestionRegulationRelations;
import com.achobeta.themis.domain.chat.model.entity.Questions;
import com.achobeta.themis.domain.laws.model.entity.KnowledgeBaseReviewDTO;
import com.achobeta.themis.domain.laws.model.entity.KnowledgeBaseSearchHistory;
import com.achobeta.themis.domain.laws.model.entity.LawCategory;
import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import com.achobeta.themis.domain.laws.repo.IKnowledgeBaseRepository;
import com.achobeta.themis.infrastructure.chat.mapper.QuestionMapper;
import com.achobeta.themis.infrastructure.chat.mapper.QuestionRegulationRelationsMapper;
import com.achobeta.themis.infrastructure.laws.mapper.LawCategoryMapper;
import com.achobeta.themis.infrastructure.laws.mapper.LawRegulationMapper;
import com.achobeta.themis.infrastructure.laws.mapper.SearchHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeBaseRepository implements IKnowledgeBaseRepository {
    private final QuestionMapper questionMapper;
    private final QuestionRegulationRelationsMapper questionRegulationRelationsMapper;
    private final LawRegulationMapper lawRegulationMapper;
    private final LawCategoryMapper lawCategoriesMapper;
    private final SearchHistoryMapper searchHistoryMapper;



    /**
     * 根据用户问题内容查询问题
     * @param userQuestionContent
     * @return
     */
    @Override
    public Questions findQuestionByUserQuestionContent(String userQuestionContent) {
        try {
            List<Questions> questionsList = questionMapper.selectList(new LambdaQueryWrapper<Questions>()
                    .like(Questions::getQuestionContent, userQuestionContent));
            if (questionsList.isEmpty()) {
                return null;
            }
            return questionsList.getFirst();
        } catch (Exception e) {
            throw new RuntimeException("查询问题失败{}", e);
        }
    }

    /**
     * 保存问题
     * @param questions
     * @return
     */
    @Override
    public Long saveQuestions(Questions questions) {
        questionMapper.insert(questions);
        return questions.getId();
    }

    /**
     * 保存问题法规关联关系
     * @param questionRegulationRelations
     */
    @Override
    public void saveQuestionRegulationRelations(QuestionRegulationRelations questionRegulationRelations) {
        questionRegulationRelationsMapper.insert(questionRegulationRelations);
    }

    /**
     * 根据法规ID和问题ID查询知识库审核详情
     * @param regulationID
     * @param userQuestionId
     * @return
     */
    @Override
    public KnowledgeBaseReviewDTO findKnowledgeBaseReviewDetailsById(Long regulationID, Long userQuestionId) {KnowledgeBaseReviewDTO knowledgeBaseReviewDTO = new KnowledgeBaseReviewDTO();
        LawRegulation lawRegulation = lawRegulationMapper.selectOne(new LambdaQueryWrapper<LawRegulation>()
                .eq(LawRegulation::getRegulationId, regulationID));
        LawCategory lawCategory = lawCategoriesMapper.selectOne(new LambdaQueryWrapper<LawCategory>()
                .eq(LawCategory::getLawId, lawRegulation.getLawCategoryId()));

        // 获取关联法条数量，防御性编程处理 null 情况
        List<Integer> relatedRegulationIds = lawCategory.getRelatedRegulationIds();
        int totalArticles = (relatedRegulationIds != null) ? relatedRegulationIds.size() : 0;

        knowledgeBaseReviewDTO.setLawName(lawCategory.getLawName())
                .setOriginalText(lawRegulation.getOriginalText())
                .setArticleNumber(lawRegulation.getArticleNumber())
                .setTotalArticles(totalArticles)
                .setIssueYear(lawRegulation.getIssueYear());
        if (userQuestionId != null) {
            QuestionRegulationRelations questionRegulationRelations = questionRegulationRelationsMapper.selectOne(new LambdaQueryWrapper<QuestionRegulationRelations>()
                    .eq(QuestionRegulationRelations::getQuestionId, userQuestionId)
                    .eq(QuestionRegulationRelations::getRegulationId, regulationID));
            knowledgeBaseReviewDTO.setAiTranslation(questionRegulationRelations.getAiTranslation())
                    .setRelevantCases(questionRegulationRelations.getRelevantCases())
                    .setRelevantQuestions(questionRegulationRelations.getRelevantQuestions())
                    .setRelatedRegulationList(questionRegulationRelations.getRelevantRegulations());
        }
        return knowledgeBaseReviewDTO;
    }

    /**
     * 根据问题ID查询关联的法规ID列表
     * @param questionId
     * @return
     */
    @Override
    public List<Long> findRegulationIdsByQuestionId(Long questionId) {
        return questionRegulationRelationsMapper.selectList(new LambdaQueryWrapper<QuestionRegulationRelations>()
                .eq(QuestionRegulationRelations::getQuestionId, questionId))
                .stream()
                .map(QuestionRegulationRelations::getRegulationId)
                .toList();
    }

    @Override
    public void saveSearchHistory(String userQuestion, Long userId) {
        searchHistoryMapper.insert(KnowledgeBaseSearchHistory.builder()
                .userId(userId)
                .userQuestion(userQuestion)
                .build()
        );

    }

    @Override
    public List<String> findSearchHistoryByUserId(Long currentUserId, int limit) {
        return searchHistoryMapper.selectList(new LambdaQueryWrapper<KnowledgeBaseSearchHistory>()
                .eq(KnowledgeBaseSearchHistory::getUserId, currentUserId)
                .orderByDesc(KnowledgeBaseSearchHistory::getCreateTime)
                .last("LIMIT " + limit))
                .stream()
                .map(KnowledgeBaseSearchHistory::getUserQuestion)
                .toList();
    }

    @Override
    public void removeSearchHistory(Long historyId) {
        searchHistoryMapper.delete(new LambdaQueryWrapper<KnowledgeBaseSearchHistory>()
                .eq(KnowledgeBaseSearchHistory::getId, historyId));
    }

    @Override
    public KnowledgeBaseSearchHistory findSearchHistoryByUserIdAndUserQuestionContent(Long currentUserId, String historyQuery) {
        return searchHistoryMapper.selectOne(new LambdaQueryWrapper<KnowledgeBaseSearchHistory>()
                .eq(KnowledgeBaseSearchHistory::getUserId, currentUserId)
                .eq(KnowledgeBaseSearchHistory::getUserQuestion, historyQuery));
    }
}
