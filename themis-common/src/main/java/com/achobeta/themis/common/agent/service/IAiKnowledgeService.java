package com.achobeta.themis.common.agent.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * <p>
 * 描述：
 * </p>
 *
 * @Author: ZGjie20
 * @version: 1.0.0
 */

/**
 *
 * */
public interface IAiKnowledgeService {
     /**
      * 知识库问答
      * @param memoryId 内存ID
      * @param message 用户消息
      * @return 知识库问答响应
      */
     String chat(@MemoryId String memoryId, @UserMessage String message);
}
