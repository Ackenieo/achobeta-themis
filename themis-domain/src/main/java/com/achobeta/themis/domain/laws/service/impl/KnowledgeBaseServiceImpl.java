package com.achobeta.themis.domain.laws.service.impl;

import cn.hutool.json.JSONUtil;
import com.achobeta.themis.common.agent.service.IAiKnowledgeService;
import com.achobeta.themis.common.component.MeiliSearchComponent;
import com.achobeta.themis.common.component.entity.KnowledgeBaseQuestionDocument;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.common.redis.service.IRedisService;
import com.achobeta.themis.common.util.IKPreprocessorUtil;
import com.achobeta.themis.domain.laws.model.entity.KnowledgeBaseReviewDTO;
import com.achobeta.themis.domain.laws.model.entity.KnowledgeBaseSearchHistory;
import com.achobeta.themis.domain.chat.model.entity.QuestionRegulationRelations;
import com.achobeta.themis.domain.chat.model.entity.Questions;
import com.achobeta.themis.domain.laws.model.vo.KnowledgeBaseQueryResponseVO;
import com.achobeta.themis.domain.laws.model.vo.KnowledgeQueryRequestVO;
import com.achobeta.themis.domain.laws.repo.IKnowledgeBaseRepository;
import com.achobeta.themis.domain.laws.service.IKnowledgeBaseService;
import com.achobeta.themis.domain.laws.service.IKnowledgeQueryService;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.achobeta.themis.common.Constant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements IKnowledgeBaseService {
    private final IRedisService redissonService;
    private final MeiliSearchComponent meiliSearchComponent;
    private final IKnowledgeBaseRepository knowledgeBaseRepository;
    private final IKnowledgeQueryService knowledgeQueryService;

    @Autowired
    @Qualifier("threadPoolExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    @Qualifier("Knowledge")
    private IAiKnowledgeService knowledgeAgentService;

    /**
     * 查询知识库问题
     * @param userQuestion 用户问题
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KnowledgeBaseQueryResponseVO> query(String userQuestion) {
        Long currentUserId = (Long) ((Map<String, Object>) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).get("id");
        threadPoolTaskExecutor.execute(() -> {
            KnowledgeBaseSearchHistory history = knowledgeBaseRepository.findSearchHistoryByUserIdAndUserQuestionContent(currentUserId, userQuestion);
            if (history == null) {
                knowledgeBaseRepository.saveSearchHistory(userQuestion, currentUserId);
                redissonService.addToList(KNOWLEDGE_BASE_SEARCH_HISTORY_KEY + currentUserId, userQuestion);
                redissonService.setListExpired(KNOWLEDGE_BASE_SEARCH_HISTORY_KEY + currentUserId, TimeUnit.HOURS.toMillis(3));
            }
        });
        Long questionId = null;
        // 先查MySQL的问题表
        Questions question = knowledgeBaseRepository.findQuestionByUserQuestionContent(userQuestion);
        if (question != null) {
            questionId = question.getId();
            // 用问题id找对应文档
            KnowledgeBaseQuestionDocument questionDocument = meiliSearchComponent.getKnowledgeBaseQuestionDocumentById(KNOWLEDGE_BASE_QUESTION_DOCUMENTS, questionId);
            if (questionDocument == null) {
                log.warn("未找到问题id对应的文档");
                throw new BusinessException("未找到问题id对应的文档");
            }
            // 更新文档的点击次数
            meiliSearchComponent.updateCount(KNOWLEDGE_BASE_QUESTION_DOCUMENTS, questionDocument.getId(), questionDocument.getCount() + 1);
        } else {
            // 无结果时，meilisearch查问题
            List<KnowledgeBaseQuestionDocument> questionDocuments = null;
            try {
                String str = IKPreprocessorUtil.stopWordSegment(userQuestion, true);
                questionDocuments = meiliSearchComponent.strictSearchForKnowledgeBaseQuestion(str, 1, 1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!questionDocuments.isEmpty()) {
                // 有数据
                KnowledgeBaseQuestionDocument questionDocument = questionDocuments.getFirst();
                meiliSearchComponent.updateCount(KNOWLEDGE_BASE_QUESTION_DOCUMENTS, questionDocument.getId(), questionDocument.getCount() + 1);
                questionId = questionDocument.getQuestionId();
            } else {
                Map<Long, String> knowledgeIdAndContent = null;
                try {
                    knowledgeIdAndContent = knowledgeQueryService.queryKnowledgeId(KnowledgeQueryRequestVO.builder().question(userQuestion).build());
                } catch (Exception e) {
                    log.error("查询知识库问题失败", e);
                    throw new RuntimeException(e);
                }
                if (knowledgeIdAndContent.isEmpty()) {
                    log.warn("未找到相关法律文档");
                    throw new BusinessException("未找到相关法律");
                }
                String prompt = buildKnowledgePrompt(userQuestion, knowledgeIdAndContent);
                String response = knowledgeAgentService.chat(UUID.randomUUID().toString(), prompt);
                log.warn("ai返回结果: {}", response);
                // 异步插入数据
                // 获得自己的代理对象
                KnowledgeBaseServiceImpl knowledgeBaseServiceProxy = (KnowledgeBaseServiceImpl) AopContext.currentProxy();
                threadPoolTaskExecutor.execute(() -> {
                    knowledgeBaseServiceProxy.insertKnowledgeBaseData(userQuestion, response);
                });
                // 根据ai的信息封装返回结果
                return parseJsonAndSearchDataToKnowledgeBaseQueryResponseVO(response, userQuestion);
            }
        }
        List<Long> regulationIds = knowledgeBaseRepository.findRegulationIdsByQuestionId(questionId);
        Long questionIdForSearch = questionId;
        return regulationIds.stream().map(regulationId ->{
            KnowledgeBaseReviewDTO knowledgeBaseReviewDTO = knowledgeBaseRepository.findKnowledgeBaseReviewDetailsById(regulationId, questionIdForSearch);
                        return knowledgeBaseBaseReviewToKnowledgeBaseQueryResponseVO(knowledgeBaseReviewDTO, regulationId.intValue());
        }).collect(Collectors.toList());
    }

    /**
     * 查找topic
     * @return 所有topic列表
     */
    @Override
    public List<String> queryTopics() {
        List<KnowledgeBaseQuestionDocument> documents = meiliSearchComponent.searchFilteredAndSortedDocuments(KNOWLEDGE_BASE_QUESTION_DOCUMENTS,
                null,
                new String[]{"count:desc"},
                10,
                KnowledgeBaseQuestionDocument.class);
        return documents.stream().map(KnowledgeBaseQuestionDocument::getTopic).collect(Collectors.toList());
    }

    /**
     * 查询常见场景
     * @return 所有常见场景列表
     */
    @Override
    public List<String> queryCaseBackgrounds() {
        List<KnowledgeBaseQuestionDocument> documents = meiliSearchComponent.searchFilteredAndSortedDocuments(KNOWLEDGE_BASE_QUESTION_DOCUMENTS,
                null,
                new String[]{"count:desc"},
                10,
                KnowledgeBaseQuestionDocument.class);
        return documents.stream().map(KnowledgeBaseQuestionDocument::getCaseBackground).collect(Collectors.toList());
    }

    @Override
    public List<String> querySearchHistory() {
        Long currentUserId = (Long) ((Map<String, Object>) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).get("id");
        // 从Redis中获取搜索历史
        List<String> searchHistoryList = redissonService.getList(KNOWLEDGE_BASE_SEARCH_HISTORY_KEY + currentUserId);
        if (searchHistoryList.size() > 5){
            searchHistoryList = searchHistoryList.subList(searchHistoryList.size() - 5, searchHistoryList.size()).reversed();
        }
        if (searchHistoryList.size() < 5) {
            // 在数据库里查5条
            searchHistoryList = knowledgeBaseRepository.findSearchHistoryByUserId(currentUserId, 5);
            for (String searchHistory : searchHistoryList) {
                redissonService.addToList(KNOWLEDGE_BASE_SEARCH_HISTORY_KEY + currentUserId, searchHistory);
            }
            redissonService.setListExpired(KNOWLEDGE_BASE_SEARCH_HISTORY_KEY + currentUserId, 1000 * 60 * 5);
        }
        return new ArrayList<>(searchHistoryList);
    }

    /**
     * 异步插入知识库数据
     * @param userQuestion
     * @param response
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertKnowledgeBaseData(String userQuestion, String response) {
        // 解析数据到Questions表
        Questions questions = parseJsonToQuestions(response, userQuestion);
        Long questionId = knowledgeBaseRepository.saveQuestions(questions);
        // 异步解析数据到Questions法律关联表
        List<QuestionRegulationRelations> questionRegulationRelations = parseJsonToQuestionRegulationRelations(response, questionId);
        if (questionRegulationRelations != null)
            questionRegulationRelations.forEach(knowledgeBaseRepository::saveQuestionRegulationRelations);
        else {
            log.info("问题{}未关联任何法律", questionId);
            throw new BusinessException("问题未关联任何法律");
        }
        // 异步解析数据到KnowledgeBaseQuestionDocument表
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                KnowledgeBaseQuestionDocument knowledgeBaseQuestionDocuments = parseJsonToKnowledgeBaseQuestionDocuments(response, questionId, userQuestion);
                if (knowledgeBaseQuestionDocuments != null) {
                    // 事务提交成功后，执行MeiliSearch操作
                    meiliSearchComponent.addDocuments(KNOWLEDGE_BASE_QUESTION_DOCUMENTS,
                            List.of(knowledgeBaseQuestionDocuments));
                }
            }
        });
    }

    @Override
    public void deleteSearchHistory(String historyQuery) {
        Long currentUserId = (Long) ((Map<String, Object>) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).get("id");
        redissonService.removeList(KNOWLEDGE_BASE_SEARCH_HISTORY_KEY + currentUserId);
        KnowledgeBaseSearchHistory knowledgeBaseSearchHistory = knowledgeBaseRepository.findSearchHistoryByUserIdAndUserQuestionContent(currentUserId, historyQuery);
        if (knowledgeBaseSearchHistory == null) {
            log.info("用户{}未搜索过问题{}", currentUserId, historyQuery);
            throw new BusinessException("用户未搜索过该问题");
        }
        knowledgeBaseRepository.removeSearchHistory(knowledgeBaseSearchHistory.getId());
    }

    /**
     * 解析json字符串到知识库查询响应VO
     * @param JSONStr
     * @param userQuestion
     * @return
     */
    private List<KnowledgeBaseQueryResponseVO> parseJsonAndSearchDataToKnowledgeBaseQueryResponseVO(String JSONStr, String userQuestion) {
        JSONObject jsonObject = JSONObject.parseObject(JSONStr);
        JSONArray regulationArray = jsonObject.getJSONArray("regulationList");
        if (regulationArray == null || regulationArray.isEmpty()) {
            log.info("问题{}未关联任何法律", userQuestion);
            return null;
        }
        return regulationArray.stream()
                .map(regulation -> JSONObject.parseObject(regulation.toString()))
                .map(regulation -> {
                    Long regulationID = regulation.getLong("regulationID");
                    // 根据法律id查询法律名称、法律内容、条款号、总条款数、发布年份
                    KnowledgeBaseReviewDTO knowledgeBaseReviewDTO = knowledgeBaseRepository.findKnowledgeBaseReviewDetailsById(regulationID, null);
                    return KnowledgeBaseQueryResponseVO.builder()
                            .lawName(knowledgeBaseReviewDTO.getLawName())
                            .regulationId(regulationID.intValue())
                            .regulationContent(knowledgeBaseReviewDTO.getOriginalText())
                            .relatedRegulationList(regulation.getJSONArray("relatedRegulationList").toList(String.class))
                            .aiTranslateContent(regulation.getString("aiTranslation"))
                            .relevantCases(regulation.getJSONArray("relevantCases").toList(KnowledgeBaseQueryResponseVO.RelevantCases.class))
                            .relevantQuestions(regulation.getJSONArray("relevantQuestions").toList(String.class))
                            .articleNumber(knowledgeBaseReviewDTO.getArticleNumber())
                            .totalArticles(knowledgeBaseReviewDTO.getTotalArticles())
                            .issueYear(knowledgeBaseReviewDTO.getIssueYear())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 将知识库审核实体转换为知识库查询响应VO
     * @param knowledgeBaseReviewDTO
     * @return
     */
    private KnowledgeBaseQueryResponseVO knowledgeBaseBaseReviewToKnowledgeBaseQueryResponseVO(KnowledgeBaseReviewDTO knowledgeBaseReviewDTO, Integer regulationId) {
        String relevantCases = knowledgeBaseReviewDTO.getRelevantCases();
        String relevantQuestions = knowledgeBaseReviewDTO.getRelevantQuestions();
        return KnowledgeBaseQueryResponseVO.builder()
                .lawName(knowledgeBaseReviewDTO.getLawName())
                .regulationId(regulationId)
                .regulationContent(knowledgeBaseReviewDTO.getOriginalText())
                .aiTranslateContent(knowledgeBaseReviewDTO.getAiTranslation())
                .relevantCases(JSONUtil.toList(knowledgeBaseReviewDTO.getRelevantCases(), KnowledgeBaseQueryResponseVO.RelevantCases.class))
                .relevantQuestions(JSONUtil.toList(knowledgeBaseReviewDTO.getRelevantQuestions(), String.class))
                .relatedRegulationList(JSONUtil.toList(knowledgeBaseReviewDTO.getRelatedRegulationList(), String.class))
                .articleNumber(knowledgeBaseReviewDTO.getArticleNumber())
                .totalArticles(knowledgeBaseReviewDTO.getTotalArticles())
                .issueYear(knowledgeBaseReviewDTO.getIssueYear())
                .build();
    }

    /** 期望ai返回的json字符串格式
     * {
     *      "topic": "",
     *      "caseBackground": "",
     *      "regulationList": [
     *          {
     *              "regulationID": 1,
     *              "aiTranslation": "",
     *              "relevantCases": [
     *                  {
     *                      "caseContent": "",
     *                      "caseLink": ""
     *                  },
     *                  {
     *                      "caseContent": "",
     *                      "caseLink": ""
     *                  }
     *              ],
     *              "relevantQuestions": [
     *                  "相关问题1",
     *                  "相关问题2"
     *              ]
     *          },
     *          {
     *              "regulationID": 2,
     *              "aiTranslation": "",
     *              "relevantCases": [
     *                    {
     *                        "caseContent": "",
     *                        "caseLink": ""
     *                    }
     *                ],
     *              "relevantQuestions": [
     *                   "相关问题1",
     *                   "相关问题2"
     *              ]
     *          }
     *      ]
     *
     * }
     */

    /**
     * 构建审核提示
     * @param userQuestion
     * @param knowledgeIdAndContent 法律id到法律内容的映射
     * @return 知识库问答提示
     */
    private String buildKnowledgePrompt(String userQuestion, Map<Long, String> knowledgeIdAndContent) {
        String regulationIdAndContent = knowledgeIdAndContent.entrySet().stream()
                .map(entry -> String.format("[regulationID: %d; regulationContent:%s]", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("、"));
        return String.format("; 其中用户问题为：%s; 与问题相关的法律法条内容为：%s", userQuestion, regulationIdAndContent);
    }

    /**
     * 解析json字符串到问题实体
     * @param JSONStr
     * @param userQuestion
     * @return
     */
    private Questions parseJsonToQuestions(String JSONStr, String userQuestion) {
        // 从json解析topic，caseBackground
        JSONObject jsonObject = JSONObject.parseObject(JSONStr);
        String topic = jsonObject.getString("topic");
        String caseBackground = jsonObject.getString("caseBackground");
        return Questions.builder()
                .questionContent(userQuestion)
                .topic(topic)
                .caseBackground(caseBackground)
                .build();
    }

    /**
     * 解析json字符串到知识库问题文档
     * @param JSONStr
     * @param questionId
     * @param userQuestion
     * @return
     */
    private KnowledgeBaseQuestionDocument parseJsonToKnowledgeBaseQuestionDocuments(String JSONStr, Long questionId, String userQuestion) {
        JSONObject jsonObject = JSONObject.parseObject(JSONStr);
        String questionSegmented = null;
        try {
            questionSegmented = IKPreprocessorUtil.stopWordSegment(userQuestion, true);
        } catch (Exception e) {
            log.error("问题{}分词失败", questionId, e);
            throw new RuntimeException(e);
        }
        return KnowledgeBaseQuestionDocument.builder()
                .id(UUID.randomUUID().toString())
                .questionId(questionId)
                .question(userQuestion)
                .questionSegmented(questionSegmented)
                .topic(jsonObject.getString("topic"))
                .caseBackground(jsonObject.getString("caseBackground"))
                .count(1)
                .build();
    }

    /**
     * 解析json字符串到问题法规关联关系列表
     * @param JSONStr
     * @param questionId
     * @return
     */
    private List<QuestionRegulationRelations> parseJsonToQuestionRegulationRelations(String JSONStr, Long questionId) {
        JSONObject jsonObject = JSONObject.parseObject(JSONStr);
        JSONArray regulationArray = jsonObject.getJSONArray("regulationList");
        if (regulationArray == null || regulationArray.isEmpty()) {
            log.info("问题{}未关联任何法律", questionId);
            return null;
        }
        return regulationArray.stream()
                .map(obj -> {
                    JSONObject regulation = (JSONObject) obj;
                    return QuestionRegulationRelations.builder()
                            .questionId(questionId)
                            .regulationId(regulation.getLong("regulationID"))
                            .aiTranslation(regulation.getString("aiTranslation"))
                            .relevantCases(regulation.getJSONArray("relevantCases").toJSONString())
                            .relevantQuestions(regulation.getJSONArray("relevantQuestions").toJSONString())
                            .relevantRegulations(regulation.getJSONArray("relatedRegulationList").toJSONString())
                            .build();
                })
                .collect(Collectors.toList());
    }
}

