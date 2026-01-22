package com.achobeta.themis.domain.laws.repo;

import com.achobeta.themis.domain.laws.model.entity.KnowledgeBaseReviewDTO;
import com.achobeta.themis.domain.laws.model.entity.KnowledgeBaseSearchHistory;
import com.achobeta.themis.domain.chat.model.entity.QuestionRegulationRelations;
import com.achobeta.themis.domain.chat.model.entity.Questions;

import java.util.List;

/**
 * 知识库仓库接口
 */
public interface IKnowledgeBaseRepository {
    /**
     * 根据问题内容查询问题实体
     * @param userQuestionContent
     * @return
     */
    Questions findQuestionByUserQuestionContent(String userQuestionContent);

    /**
     * 保存问题
     * @param questions
     * @return
     */
    Long saveQuestions(Questions questions);

    /**
     * 保存问题法规关联关系
     * @param questionRegulationRelations
     */
    void saveQuestionRegulationRelations(QuestionRegulationRelations questionRegulationRelations);

    /**
     * 根据法规ID和问题ID查询知识库审核详情
     * @param regulationID
     * @param userQuestionId
     * @return
     */
    KnowledgeBaseReviewDTO findKnowledgeBaseReviewDetailsById(Long regulationID, Long userQuestionId);

    /**
     * 根据问题ID查询关联的法规ID列表
     * @param questionId
     * @return
     */
    List<Long> findRegulationIdsByQuestionId(Long questionId);

    /**
     * 保存搜索历史
     * @param userQuestion 用户问题内容
     * @param userId 用户ID
     */
    void saveSearchHistory(String userQuestion, Long userId);

    /**
     * 根据用户ID查询搜索历史
     * @param currentUserId 用户ID
     * @param limit 限制数量
     * @return 搜索历史列表
     */
    List<String> findSearchHistoryByUserId(Long currentUserId, int limit);

    /**
     * 删除搜索历史
     * @param historyId 搜索历史ID
     */
    void removeSearchHistory(Long historyId);

    /**
     * 根据用户ID和用户问题内容查询搜索历史
     * @param currentUserId 用户ID
     * @param historyQuery 用户问题内容
     * @return 搜索历史实体
     */
    KnowledgeBaseSearchHistory findSearchHistoryByUserIdAndUserQuestionContent(Long currentUserId, String historyQuery);
}
