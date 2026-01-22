package com.achobeta.themis.common.agent.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

public interface IAiChatService {

    /**
     * 聊天
     * @param memoryId 内存ID
     * @param message 消息
     * @return 响应流
     */

    Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);


}
