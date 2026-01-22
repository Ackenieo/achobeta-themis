package com.achobeta.themis.domain.laws.service;

import com.achobeta.themis.domain.laws.model.vo.KnowledgeBaseQueryResponseVO;

import java.util.List;

public interface IKnowledgeBaseService {
     /**
      * 查询知识库问题
      * @param userQuestion 用户问题
      * @return 知识库问题响应VO
      */
    List<KnowledgeBaseQueryResponseVO> query(String userQuestion);

     /**
      * 查找topic
      * @return 所有topic列表
      */
    List<String> queryTopics();

    /**
      * 查询常见场景
      * @return 所有常见场景列表
      */
    List<String> queryCaseBackgrounds();

    /**
     * 查询搜索历史
     * @return 所有搜索历史列表
     */
    List<String> querySearchHistory();

    /**
     * 插入知识库数据
     * @param userQuestion 用户问题
     * @param response 知识库响应
     */
    void insertKnowledgeBaseData(String userQuestion, String response);

    /**
     * 删除搜索历史
     * @param historyQuery 搜索历史查询
     */
    void deleteSearchHistory(String historyQuery);

}
