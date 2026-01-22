package com.achobeta.themis.domain.review.service;

public interface IAdjudicatorService {
    /**
     *  adjudication
     * @param userType
     * @param conversationId
     * @param question
     */
    void adjudicate(Integer userType, String conversationId, String question);
}
