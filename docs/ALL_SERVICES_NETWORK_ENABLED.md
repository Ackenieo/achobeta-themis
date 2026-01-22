# 所有 AI 服务联网搜索功能说明

## 📋 概述

项目中的所有 AI 服务现已全部启用 Tavily 网络搜索功能，可以实时获取互联网信息。

---

## ✅ 已启用的服务

### 1. **Consulter 服务** - 咨询服务
- **Bean 名称**: `consulter`
- **服务接口**: `IAiChatService`
- **启用工具**: `tavilyTool`
- **功能**: 提供咨询服务，支持实时网络搜索
- **使用场景**: 
  - 用户咨询实时信息
  - 查询最新资讯
  - 获取当前事件信息

### 2. **Knowledge 服务** - 知识库服务
- **Bean 名称**: `Knowledge`
- **服务接口**: `IAiKnowledgeService`
- **启用工具**: `tavilyTool`
- **功能**: 知识库查询服务，支持实时网络搜索
- **使用场景**:
  - 知识库查询
  - 补充最新知识
  - 验证信息准确性

### 3. **Adjudicator 服务** - 裁决服务
- **Bean 名称**: `adjudicator`
- **服务接口**: `IAiAdjudicatorService`
- **启用工具**: `meilisearchTool`, `tavilyTool`
- **功能**: 裁决服务，同时支持知识库搜索和网络搜索
- **使用场景**:
  - 问题分类和裁决
  - 结合知识库和实时信息
  - 综合判断和决策

---

## 🔧 技术实现

### AgentConfig 配置

```java
@Configuration
public class AgentConfig {
    @Autowired
    private TavilyTool tavilyTool;
    
    @Autowired
    private MeilisearchTool meilisearchTool;
    
    // 1. Consulter 服务
    @Bean("consulter")
    public IAiChatService consulterService() {
        return AiServices.builder(IAiChatService.class)
                .streamingChatModel(model)
                .chatMemoryProvider(...)
                .tools(tavilyTool)  // ✅ 网络搜索
                .build();
    }
    
    // 2. Knowledge 服务
    @Bean("Knowledge")
    public IAiKnowledgeService KnowledgeService() {
        return AiServices.builder(IAiKnowledgeService.class)
                .chatModel(model)
                .chatMemoryProvider(...)
                .tools(tavilyTool)  // ✅ 网络搜索
                .build();
    }
    
    // 3. Adjudicator 服务
    @Bean("adjudicator")
    public IAiAdjudicatorService adjudicatorService() {
        return AiServices.builder(IAiAdjudicatorService.class)
                .chatModel(model)
                .chatMemoryProvider(...)
                .tools(meilisearchTool, tavilyTool)  // ✅ 知识库 + 网络搜索
                .build();
    }
}
```

---

## 🎯 使用示例

### 示例 1：Consulter 服务 - 实时天气查询

**用户提问**：
```
今天北京的天气怎么样？
```

**AI 处理流程**：
1. Consulter 服务接收请求
2. 识别需要实时信息
3. 自动调用 `tavilyTool.webSearch("北京天气")`
4. 获取最新天气数据
5. 整合信息回答用户

**AI 回复**：
```
根据最新信息，今天北京的天气是晴天，温度 15-25°C...
```

---

### 示例 2：Knowledge 服务 - 技术知识查询

**用户提问**：
```
Spring Boot 3.2 有哪些新特性？
```

**AI 处理流程**：
1. Knowledge 服务接收请求
2. 调用 `tavilyTool.webSearch("Spring Boot 3.2 新特性")`
3. 获取官方文档和技术文章
4. 提取关键特性
5. 结构化呈现

**AI 回复**：
```
Spring Boot 3.2 的主要新特性包括：
1. 虚拟线程支持（Virtual Threads）
2. 改进的 Docker Compose 支持
3. ...
```

---

### 示例 3：Adjudicator 服务 - 综合判断

**用户提问**：
```
这个问题应该归类到哪个分类？最近有类似的问题吗？
```

**AI 处理流程**：
1. Adjudicator 服务接收请求
2. 使用 `meilisearchTool` 查询知识库中的历史问题
3. 使用 `tavilyTool` 搜索网络上的相关讨论
4. 综合两种信息源进行判断
5. 给出分类建议

**AI 回复**：
```
根据知识库和网络信息，这个问题应该归类为"技术问题"。
最近有 3 个类似问题...
```

---

## 📊 工具对比

| 工具 | 功能 | 数据源 | 响应速度 | 使用场景 |
|------|------|--------|----------|----------|
| **TavilyTool** | 网络搜索 | 实时互联网 | ~2秒 | 最新资讯、实时信息 |
| **MeilisearchTool** | 知识库搜索 | 本地知识库 | <100ms | 历史数据、已知问题 |

---

## 🔍 AI 自动决策逻辑

AI 会根据问题类型自动选择合适的工具：

### 使用 TavilyTool（网络搜索）的场景：
- ✅ 包含时间词：今天、最近、现在、当前
- ✅ 查询实时数据：天气、新闻、股票
- ✅ 最新版本信息：软件版本、技术更新
- ✅ 当前事件：热点新闻、突发事件

### 使用 MeilisearchTool（知识库搜索）的场景：
- ✅ 历史问题查询
- ✅ 已知分类标签
- ✅ 问题归档和统计
- ✅ 相似问题匹配

### 同时使用两种工具的场景：
- ✅ 需要综合判断
- ✅ 对比历史和现状
- ✅ 验证信息准确性
- ✅ 全面的信息收集

---

## 📈 性能优化建议

### 1. 缓存机制
建议为网络搜索结果添加缓存：

```java
@Component
public class TavilyTool {
    private final Cache<String, String> searchCache = 
        Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();
    
    public String webSearch(String query, Integer maxResults) {
        // 先查缓存
        String cached = searchCache.getIfPresent(query);
        if (cached != null) {
            return cached;
        }
        
        // 执行搜索
        String result = performSearch(query, maxResults);
        
        // 存入缓存
        searchCache.put(query, result);
        return result;
    }
}
```

### 2. 限流保护
防止 API 配额耗尽：

```java
@Component
public class TavilyTool {
    private final RateLimiter rateLimiter = 
        RateLimiter.create(10.0); // 每秒最多 10 次
    
    public String webSearch(String query, Integer maxResults) {
        if (!rateLimiter.tryAcquire()) {
            return "搜索请求过于频繁，请稍后再试。";
        }
        // ... 执行搜索
    }
}
```

### 3. 异步处理
对于非紧急查询，使用异步处理：

```java
@Async
public CompletableFuture<String> webSearchAsync(String query) {
    return CompletableFuture.supplyAsync(() -> 
        webSearch(query, 5)
    );
}
```

---

## 🔒 安全建议

### 1. API Key 管理
- ❌ 不要将 API Key 硬编码
- ✅ 使用环境变量或配置中心
- ✅ 定期轮换 API Key
- ✅ 监控 API 使用情况

### 2. 输入验证
```java
public String webSearch(String query, Integer maxResults) {
    // 验证输入
    if (query == null || query.trim().isEmpty()) {
        throw new IllegalArgumentException("搜索关键词不能为空");
    }
    
    if (query.length() > 200) {
        throw new IllegalArgumentException("搜索关键词过长");
    }
    
    // 过滤敏感词
    if (containsSensitiveWords(query)) {
        return "搜索内容包含敏感词，已被拒绝。";
    }
    
    // ... 执行搜索
}
```

### 3. 结果过滤
对搜索结果进行安全检查：

```java
private String filterResults(List<SearchResult> results) {
    return results.stream()
        .filter(r -> !containsSensitiveContent(r.getContent()))
        .filter(r -> isReliableSource(r.getUrl()))
        .map(SearchResult::toString)
        .collect(Collectors.joining("\n\n"));
}
```

---

## 📊 监控指标

建议监控以下指标：

### 1. 使用统计
- 每日搜索次数
- 各服务搜索频率
- 热门搜索关键词

### 2. 性能指标
- 平均响应时间
- 成功率
- 错误率

### 3. 成本控制
- API 配额使用情况
- 预计月度成本
- 配额预警

---

## 🎯 最佳实践

### 1. 搜索关键词优化
```java
// ❌ 不好的做法
webSearch("用户问的问题原文很长很长...", 5);

// ✅ 好的做法
String optimizedQuery = extractKeywords(userQuestion);
webSearch(optimizedQuery, 5);
```

### 2. 结果数量控制
```java
// ❌ 不必要的大量结果
webSearch("天气", 20);  // 浪费配额

// ✅ 合理的结果数量
webSearch("天气", 3);   // 足够获取信息
```

### 3. 错误处理
```java
try {
    return webSearch(query, maxResults);
} catch (Exception e) {
    log.error("搜索失败", e);
    return "抱歉，网络搜索暂时不可用，请稍后再试。";
}
```

---

## 📚 相关文档

- **完整集成指南**: `docs/TAVILY_SETUP_GUIDE.md`
- **实施记录**: `docs/NETWORK_SEARCH_ATTEMPT.md`
- **项目说明**: `README_CN.md`

---

## ✨ 总结

现在项目中的所有 AI 服务都具备了实时网络搜索能力：

✅ **Consulter** - 咨询服务支持联网  
✅ **Knowledge** - 知识库服务支持联网  
✅ **Adjudicator** - 裁决服务支持双重搜索（知识库 + 网络）  

这使得 AI 系统能够：
- 🌐 获取实时信息
- 📰 回答最新资讯
- 🔍 验证信息准确性
- 💡 提供更全面的服务

---

**文档版本**: 1.0  
**更新时间**: 2025-01-23  
**维护者**: Achobeta Themis Team
