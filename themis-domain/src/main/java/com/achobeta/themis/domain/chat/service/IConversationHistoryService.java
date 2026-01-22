package com.achobeta.themis.domain.chat.service;

import com.achobeta.themis.domain.chat.model.entity.ConversationMeta;

import java.util.List;

public interface IConversationHistoryService {

    /**
     * 开启一个新对话；如果传入了当前对话，则将当前对话归档到用户历史。
     * 返回新的 conversationId。
     */
    String startNewConversation(String userId, String currentConversationId, String title);

    /**
     * 将对话归档到用户历史（如果尚未归档），可选设置标题。
     */
    void archiveIfAbsent(String userId, String conversationId, String title);

    /**
     * 触碰对话：确保其存在于历史列表，并续期 TTL。
     */
    void touch(String userId, String conversationId);

    /**
     * 查询用户的全部历史对话（仅返回元信息）。
     */
    List<ConversationMeta> listHistories(String userId);

    /**
     * 获取指定对话的元信息。
     */
    ConversationMeta getMeta(String conversationId);



}
