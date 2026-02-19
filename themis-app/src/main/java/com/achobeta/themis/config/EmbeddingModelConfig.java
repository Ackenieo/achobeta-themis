package com.achobeta.themis.config;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmbeddingModelConfigProperties.class)
public class EmbeddingModelConfig {
    @Autowired
    private EmbeddingModelConfigProperties embeddingModelConfigProperties;

    @Bean("openAiEmbeddingModel")
    public OpenAiEmbeddingModel openAiEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(embeddingModelConfigProperties.getBaseUrl())
                .apiKey(embeddingModelConfigProperties.getApiKey())
                .modelName(embeddingModelConfigProperties.getModel())
                .build();
    }
}
