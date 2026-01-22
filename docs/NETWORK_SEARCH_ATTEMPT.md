# 联网搜索功能尝试记录

## 尝试时间
2025年1月

## 问题描述
尝试为通义千问模型启用联网搜索功能，以便模型能够获取实时信息。

## 尝试方案

### 方案一：使用 HTTP 请求头（失败）
尝试通过 `X-DashScope-Plugin` 请求头传递 `{"enable_search": true}` 参数。

**错误信息**：
```
InvalidRequestException: Plugin ["enable_search"] not exist
```

**失败原因**：
- 联网搜索参数应该在请求体（body）中，而不是 HTTP 请求头
- LangChain4j 1.0.1 的 OpenAI 兼容模式不支持自定义请求体参数

### 方案二：使用 DashScope 原生集成（未验证）
尝试切换到 LangChain4j 的 DashScope 原生集成（`langchain4j-dashscope`），使用 `QwenChatModel` 和 `QwenStreamingChatModel`。

**未完成原因**：
- 用户反馈阿里云模型不支持联网搜索
- 已回退所有修改

## 结论
通义千问模型在当前配置下不支持联网搜索功能。项目已恢复到原始状态，使用标准的 OpenAI 兼容模式调用通义千问模型。

## 当前配置
```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: sk-xxx
      model: qwen-plus
```

## 备注
如果未来需要联网搜索功能，可以考虑：
1. 使用支持联网搜索的其他模型
2. 自行实现搜索工具集成到 LangChain4j 的 Tools 功能
3. 等待阿里云 DashScope 或 LangChain4j 的功能更新

---

## ✅ 方案三：使用 Tavily API（成功）

### 实施时间
2025年1月23日

### 方案描述
通过集成 Tavily Search API 为 AI 模型提供实时网络搜索能力。Tavily 是专为 AI 应用设计的搜索 API 平台。

### 实现步骤

#### 1. 添加依赖
在 `themis-common/pom.xml` 中添加 OkHttp 依赖：
```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

#### 2. 创建搜索组件
创建 `TavilySearchComponent.java` 用于调用 Tavily API。

#### 3. 创建 LangChain4j Tool
创建 `TavilyTool.java` 将搜索功能暴露给 AI 模型。

#### 4. 配置 API Key
在 `application-dev.yml` 和 `application-prod.yml` 中添加：
```yaml
tavily:
  api-key: tvly-xxxxxxxxxxxxxx
  api-url: https://api.tavily.com/search
```

#### 5. 集成到 AgentConfig
在所有 AI 服务中添加 `.tools(tavilyTool)`：
- **consulter** 服务：`.tools(tavilyTool)`
- **Knowledge** 服务：`.tools(tavilyTool)`
- **adjudicator** 服务：`.tools(meilisearchTool, tavilyTool)` - 同时支持两种搜索

### 优势
- ✅ 专为 AI 设计的搜索 API
- ✅ 返回结构化、高相关度的结果
- ✅ 支持中英文搜索
- ✅ 免费套餐：1000次/月
- ✅ 响应速度快（通常 < 2秒）
- ✅ 易于集成，无需复杂配置

### 获取 API Key
1. 访问 https://app.tavily.com/sign-up 注册账号
2. 登录后进入 Dashboard
3. 点击 "API Keys" 菜单
4. 复制你的 API Key（格式：`tvly-xxxxxxxxxxxxxx`）

### 使用示例
用户询问："今天的天气怎么样？"
- AI 自动识别需要实时信息
- 调用 `webSearch` 工具
- 返回最新天气信息

### 详细文档
完整的集成指南请参考：`docs/TAVILY_SETUP_GUIDE.md`

### 结论
✅ **Tavily API 集成成功**，项目现已具备实时网络搜索能力！
