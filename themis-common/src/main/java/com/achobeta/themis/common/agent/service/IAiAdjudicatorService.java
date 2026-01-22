package com.achobeta.themis.common.agent.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

public interface IAiAdjudicatorService {
    /**
     * 审核
     * @param memoryId 内存ID
     * @param message 消息
     * @return 审核结果
     */
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
