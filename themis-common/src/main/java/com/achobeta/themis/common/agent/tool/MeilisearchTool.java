package com.achobeta.themis.common.agent.tool;

import com.achobeta.themis.common.component.MeiliSearchComponent;
import com.achobeta.themis.common.component.entity.QuestionTitleDocument;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.common.util.IKPreprocessorUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MeilisearchTool {

    private final MeiliSearchComponent meiliSearchUtils;
    private final String QUESTION_TITLE_DOCUMENTS = "question_title_documents";

    @Tool(name = "addDocument", value = "添加文档到Meilisearch索引，用于存储问题标题文档。" +
            "- arg0：字符串类型，提炼出的当前用户问题的问题标签；" +
            "- arg1：整数类型，问题分类标签（父标签），范围1-15；" +
            "缺失任何参数或格式错误会导致添加失败，请务必检查。")
    public String addDocument(@P(value = "arg0 问题标签（提炼出的当前用户问题的问题标签）") String arg0,
                            @P(value = "arg1 问题分类标签（父标签）") int arg1) {
        try {
            System.out.println("ai 开始添加文档到Meilisearch索引");
            QuestionTitleDocument document = null;

            document = QuestionTitleDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .title(arg0)
                    .titleSegmented(IKPreprocessorUtil.segment(arg0, true))
                    .primaryTag(arg1)
                    .count(1)
                    .createTime(LocalDateTime.now())
                    .build();

            meiliSearchUtils.addDocuments(QUESTION_TITLE_DOCUMENTS, Collections.singletonList(document));
        } catch (Exception e) {
            return "添加文档到Meilisearch索引失败：" + e.getMessage();
        }
        return "添加文档到Meilisearch索引成功";
    }
}
