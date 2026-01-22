package com.achobeta.themis.domain.chat.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 描述：
 * </p>
 *
 * @Author: ZGjie20
 * @version: 1.0.0
 */
@Data
public class ConversationMeta {

        public String conversationId;
        public String userId;
        public String title;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

