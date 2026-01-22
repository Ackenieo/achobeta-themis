package com.achobeta.themis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model", ignoreInvalidFields = true)
public class AgentConfigProperties {
    private String baseUrl;
    private String apiKey;
    private String model;
}
