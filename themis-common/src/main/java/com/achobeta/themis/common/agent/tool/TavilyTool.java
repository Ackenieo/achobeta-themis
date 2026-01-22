package com.achobeta.themis.common.agent.tool;

import com.achobeta.themis.common.component.TavilySearchComponent;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tavily 网络搜索工具
 * 为 AI 模型提供实时网络搜索能力
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TavilyTool {

    private final TavilySearchComponent tavilySearchComponent;

    @Tool(name = "webSearch", value = "在互联网上搜索实时信息。" +
            "当用户询问最新资讯、实时数据、当前事件或你的知识库中没有的信息时使用此工具。" +
            "- query: 搜索关键词，应该是简洁明确的查询语句" +
            "- maxResults: 返回的最大结果数，默认5条，范围1-10")
    public String webSearch(
            @P(value = "query 搜索关键词") String query,
            @P(value = "maxResults 最大结果数（可选，默认5）") Integer maxResults) {
        
        log.info("AI 调用网络搜索工具: query={}, maxResults={}", query, maxResults);
        
        try {
            List<TavilySearchComponent.SearchResult> results = 
                tavilySearchComponent.search(query, maxResults);
            
            if (results.isEmpty()) {
                return "未找到相关搜索结果。";
            }
            
            // 格式化搜索结果为易读的文本
            StringBuilder formattedResults = new StringBuilder();
            formattedResults.append("搜索结果（共 ").append(results.size()).append(" 条）：\n\n");
            
            for (int i = 0; i < results.size(); i++) {
                TavilySearchComponent.SearchResult result = results.get(i);
                formattedResults.append("【结果 ").append(i + 1).append("】\n");
                formattedResults.append("标题：").append(result.getTitle()).append("\n");
                formattedResults.append("来源：").append(result.getUrl()).append("\n");
                formattedResults.append("内容：").append(result.getContent()).append("\n");
                formattedResults.append("相关度：").append(String.format("%.2f", result.getScore())).append("\n\n");
            }
            
            log.info("网络搜索完成，返回 {} 条结果", results.size());
            return formattedResults.toString();
            
        } catch (Exception e) {
            log.error("网络搜索失败: query={}", query, e);
            return "搜索过程中发生错误：" + e.getMessage();
        }
    }
}
