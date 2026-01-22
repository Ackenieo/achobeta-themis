# Tavily API 集成完整指南

## 📋 概述

本指南将帮助您完成 Tavily Search API 的集成，为 AI 模型添加实时网络搜索能力。

---

## ✅ 已完成的工作

### 1. 创建的文件

- ✅ `TavilySearchComponent.java` - Tavily API 调用组件
- ✅ `TavilyTool.java` - LangChain4j 工具封装

### 2. 待批准的修改

以下修改已提议，等待您的批准：

#### 📦 pom.xml 修改
- 在 `themis-common/pom.xml` 中添加 OkHttp 依赖

#### 配置文件修改
- `application-dev.yml` - 添加 Tavily 配置
- `application-prod.yml` - 添加 Tavily 配置（使用环境变量）

#### 代码修改
- `AgentConfig.java` - 导入 TavilyTool 并在所有 AI 服务中启用联网搜索

#### 文档修改
- `README_CN.md` - 添加功能说明
- `NETWORK_SEARCH_ATTEMPT.md` - 记录成功方案

---

## 🚀 接下来的步骤

### 步骤 1：批准所有待处理的修改

在 IDE 中查看并批准所有提议的代码修改。

### 步骤 2：获取 Tavily API Key

1. 访问 https://app.tavily.com/sign-up
2. 使用邮箱或 Google 账号注册
3. 登录后进入 Dashboard
4. 点击 "API Keys" 菜单
5. 复制你的 API Key（格式：`tvly-xxxxxxxxxxxxxx`）

### 步骤 3：配置 API Key

在 `themis-app/src/main/resources/application-dev.yml` 中，将：

```yaml
tavily:
  api-key: tvly-xxxxxxxxxxxxxx  # 请替换为你的 Tavily API Key
```

替换为你的真实 API Key。

### 步骤 4：编译项目

```bash
mvn clean install
```

### 步骤 5：运行项目

```bash
cd themis-app
mvn spring-boot:run
```

或在 IDE 中直接运行主类。

### 步骤 6：测试功能

向 AI 提问以下问题测试联网搜索：

- "今天的天气怎么样？"
- "最近有什么科技新闻？"
- "Python 3.12 有哪些新特性？"

---

## 📊 功能说明

### AI 如何使用搜索工具

AI 会在以下情况自动调用网络搜索：

1. **实时信息查询** - 天气、新闻、股票等
2. **最新资讯** - 当前事件、最新发布
3. **知识库外信息** - 训练数据中没有的内容
4. **事实核查** - 验证信息准确性

### 搜索结果格式

```
搜索结果（共 5 条）：

【结果 1】
标题：xxx
来源：https://xxx.com
内容：xxx
相关度：0.95

【结果 2】
...
```

---

## 🔧 配置说明

### 开发环境配置

```yaml
# application-dev.yml
tavily:
  api-key: tvly-xxxxxxxxxxxxxx  # 你的 API Key
  api-url: https://api.tavily.com/search
```

### 生产环境配置

```yaml
# application-prod.yml
tavily:
  api-key: ${TAVILY_API_KEY}  # 使用环境变量
  api-url: https://api.tavily.com/search
```

设置环境变量：
```bash
export TAVILY_API_KEY=tvly-xxxxxxxxxxxxxx
```

---

## 📈 API 配额

### 免费套餐
- **1000 次搜索/月**
- 适合开发和小规模应用

### 付费套餐
访问 https://tavily.com/pricing 查看详情

### 监控使用量
在 Tavily Dashboard 中可以查看：
- 当前使用量
- 剩余配额
- 使用历史

---

## 🛠️ 高级配置

### 启用深度搜索

在 `TavilySearchComponent.java` 中修改：

```java
requestBody.put("search_depth", "advanced"); // 更深入的搜索
```

### 获取 AI 生成的答案

```java
requestBody.put("include_answer", true); // Tavily AI 生成的答案
```

### 获取完整网页内容

```java
requestBody.put("include_raw_content", true); // 包含原始HTML
```

### 调整超时时间

```java
this.httpClient = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)  // 连接超时
        .readTimeout(60, TimeUnit.SECONDS)     // 读取超时
        .writeTimeout(60, TimeUnit.SECONDS)    // 写入超时
        .build();
```

---

## 🐛 故障排查

### 问题 1：API Key 未配置

**错误信息**：
```
Tavily API Key 未配置，请在配置文件中设置 tavily.api-key
```

**解决方案**：
在 `application-dev.yml` 中正确配置 API Key。

### 问题 2：API 请求失败

**错误信息**：
```
Tavily API 请求失败: code=401
```

**解决方案**：
- 检查 API Key 是否正确
- 确认 API Key 是否有效
- 检查是否超出配额限制

### 问题 3：搜索无结果

**日志信息**：
```
Tavily 搜索无结果: query=xxx
```

**可能原因**：
- 搜索关键词过于具体
- 网络连接问题
- API 服务暂时不可用

### 问题 4：编译错误

**错误信息**：
```
cannot find symbol: class OkHttpClient
```

**解决方案**：
确保已批准 pom.xml 的修改，并执行 `mvn clean install`。

---

## 📝 使用示例

### 示例 1：查询天气

**用户输入**：
```
今天北京的天气怎么样？
```

**AI 行为**：
1. 识别需要实时信息
2. 调用 `webSearch("北京天气")`
3. 获取搜索结果
4. 整合信息回答用户

**AI 回复**：
```
根据最新信息，今天北京的天气是...
```

### 示例 2：查询新闻

**用户输入**：
```
最近有什么 AI 领域的重大新闻？
```

**AI 行为**：
1. 调用 `webSearch("AI 最新新闻", 5)`
2. 获取 5 条最新结果
3. 总结关键信息

**AI 回复**：
```
以下是最近 AI 领域的重要新闻：
1. ...
2. ...
```

### 示例 3：技术查询

**用户输入**：
```
Spring Boot 3.2 有哪些新特性？
```

**AI 行为**：
1. 调用 `webSearch("Spring Boot 3.2 新特性", 3)`
2. 提取关键特性
3. 结构化呈现

**AI 回复**：
```
Spring Boot 3.2 的主要新特性包括：
- 虚拟线程支持
- ...
```

---

## 🔒 安全建议

### 1. API Key 保护

❌ **不要**：
```yaml
# 不要将真实 API Key 提交到 Git
tavily:
  api-key: tvly-abc123def456  # 危险！
```

✅ **应该**：
```yaml
# 开发环境：使用占位符
tavily:
  api-key: tvly-xxxxxxxxxxxxxx

# 生产环境：使用环境变量
tavily:
  api-key: ${TAVILY_API_KEY}
```

### 2. 添加到 .gitignore

确保配置文件不被提交：
```
application-dev.yml
application-prod.yml
```

### 3. 限流保护

建议在 `TavilyTool` 中添加限流：

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TavilyTool {
    
    private final AtomicInteger dailyCount = new AtomicInteger(0);
    private static final int DAILY_LIMIT = 100;
    
    @Tool(name = "webSearch", value = "...")
    public String webSearch(...) {
        if (dailyCount.incrementAndGet() > DAILY_LIMIT) {
            return "今日搜索次数已达上限，请明天再试。";
        }
        // ... 原有代码
    }
}
```

---

## 📚 相关资源

### 官方文档
- **Tavily 官网**: https://tavily.com
- **API 文档**: https://docs.tavily.com
- **定价页面**: https://tavily.com/pricing

### LangChain4j 文档
- **官方文档**: https://docs.langchain4j.dev
- **Tools 指南**: https://docs.langchain4j.dev/tutorials/tools

### 项目文档
- `docs/NETWORK_SEARCH_ATTEMPT.md` - 联网搜索尝试记录
- `README_CN.md` - 项目说明

---

## ✨ 总结

完成以上步骤后，您的 AI 系统将具备：

✅ **实时信息获取** - 回答最新资讯  
✅ **知识扩展** - 突破训练数据限制  
✅ **事实核查** - 验证信息准确性  
✅ **动态响应** - 根据实时数据调整回答  

---

## 🎯 下一步优化建议

1. **添加缓存机制** - 避免重复搜索
2. **实现使用统计** - 监控 API 调用
3. **优化搜索关键词** - 提高结果相关度
4. **添加结果过滤** - 提升内容质量
5. **实现多源搜索** - 结合其他搜索引擎

---

**文档版本**: 1.0  
**创建时间**: 2025-01-23  
**维护者**: Achobeta Themis Team

如有问题，请查看故障排查部分或联系技术支持。
