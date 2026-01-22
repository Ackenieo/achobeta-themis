package com.achobeta.themis.common.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tavily Search API 组件
 * 用于调用 Tavily API 进行网络搜索
 */
@Component
@Slf4j
public class TavilySearchComponent {

    @Value("${tavily.api-key:}")
    private String apiKey;

    @Value("${tavily.api-url:https://api.tavily.com/search}")
    private String apiUrl;

    private final OkHttpClient httpClient;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public TavilySearchComponent() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 执行搜索查询
     * @param query 搜索关键词
     * @param maxResults 最大返回结果数（默认5）
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query, Integer maxResults) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Tavily API Key 未配置");
            throw new IllegalStateException("Tavily API Key 未配置，请在配置文件中设置 tavily.api-key");
        }

        if (query == null || query.trim().isEmpty()) {
            log.warn("搜索关键词为空");
            return new ArrayList<>();
        }

        int limit = (maxResults != null && maxResults > 0) ? maxResults : 5;

        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("api_key", apiKey);
            requestBody.put("query", query);
            requestBody.put("max_results", limit);
            requestBody.put("search_depth", "basic"); // basic 或 advanced
            requestBody.put("include_answer", false);
            requestBody.put("include_raw_content", false);

            RequestBody body = RequestBody.create(requestBody.toJSONString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            log.info("发送 Tavily 搜索请求: query={}, maxResults={}", query, limit);

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "无响应体";
                    log.error("Tavily API 请求失败: code={}, message={}", response.code(), errorBody);
                    throw new IOException("Tavily API 请求失败: " + response.code());
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                JSONArray results = jsonResponse.getJSONArray("results");

                if (results == null || results.isEmpty()) {
                    log.info("Tavily 搜索无结果: query={}", query);
                    return new ArrayList<>();
                }

                List<SearchResult> searchResults = new ArrayList<>();
                for (int i = 0; i < results.size(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    SearchResult result = new SearchResult(
                            item.getString("title"),
                            item.getString("url"),
                            item.getString("content"),
                            item.getDouble("score")
                    );
                    searchResults.add(result);
                }

                log.info("Tavily 搜索成功: query={}, 返回 {} 条结果", query, searchResults.size());
                return searchResults;
            }
        } catch (IOException e) {
            log.error("Tavily API 调用异常: query={}", query, e);
            throw new RuntimeException("Tavily 搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 搜索结果数据类
     */
    public static class SearchResult {
        private String title;
        private String url;
        private String content;
        private Double score;

        public SearchResult(String title, String url, String content, Double score) {
            this.title = title;
            this.url = url;
            this.content = content;
            this.score = score;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getContent() {
            return content;
        }

        public Double getScore() {
            return score;
        }

        @Override
        public String toString() {
            return "标题: " + title + "\n" +
                   "链接: " + url + "\n" +
                   "内容: " + content + "\n" +
                   "相关度: " + score;
        }
    }
}
