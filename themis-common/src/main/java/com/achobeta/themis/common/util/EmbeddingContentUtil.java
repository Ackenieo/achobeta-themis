package com.achobeta.themis.common.util;

import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class EmbeddingContentUtil {
    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    /**
     * 生成文本的嵌入向量
     */
    public float[] generateEmbedding(String text) {
        return openAiEmbeddingModel.embed(text).content().vector();
    }
}
