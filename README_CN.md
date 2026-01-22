# AchoBeta Themis 裁衡 · 2025 年 J 组复试项目

English | <a href="./README.md">英文说明</a>

## 简介
- AchoBeta Themis 是一个基于 Java 21 与 Spring Boot 3 的多模块后端项目，采用领域分层实现可维护的业务架构，并提供 Docker 与 GitHub Actions 等工程化支持。
- 本文档作为中文完整版说明，覆盖技术栈、模块划分、环境准备、快速开始、配置与部署、开发规范与安全合规等内容。

## 项目目标与功能
- 面向中国劳动法与劳动合规的 AI 助手，为个人与企业用户提供咨询与文档审查能力。
- 主要能力：
  - AI 咨询聊天与对话历史管理。
  - 合同/内容审查：使用 Apache Tika 提取文本，AI 输出结构化合规结果。
  - 知识库查询：按问题、话题与场景检索。
  - MeiliSearch 检索与调试工具：法律文本的快速索引与查询。
  - 基于 JWT 与 `@LoginRequired` 的访问控制。

## 目录
- 概览与技术栈
- 模块说明
- 环境准备
- 快速开始
- 配置说明
- 数据库与外部依赖
- Docker 部署
- 常用命令
- 开发规范（分支与提交）
- CI/CD
- 许可证

## 概览与技术栈
- 语言与运行时：`Java 21`
- 框架：`Spring Boot 3.5.7`
- 构建工具：`Maven`（内置 Wrapper：`mvnw` / `mvnw.cmd`）
- 数据访问：`MyBatis`、`MyBatis-Plus`
- 缓存与队列：`Redisson`（Redis 客户端）、`Caffeine`
- 搜索：`MeiliSearch`
- 安全与工具：`JWT`、`Hutool`、`Fastjson`
- 文档解析：`Apache Tika`

## 模块说明（Maven 多模块）
- `themis-app`：应用入口与运行时，Spring Boot 启动与 Web 层集成，主类 `com.achobeta.themis.Application`。
- `themis-trigger`：触发层（Web/Adapter），包含验证与文档解析，依赖 `domain`、`api`、`common`、`infrastructure`。
- `themis-domain`：领域层，定义领域模型与核心业务逻辑，仅依赖 `common`。
- `themis-infrastructure`：基础设施层，数据库访问与外部系统集成，依赖 `domain` 与 `common`。
- `themis-api`：接口契约层，定义请求/响应模型，依赖 `common`。
- `themis-common`：公共模块，统一异常、通用工具、Redis 脚本、统一响应模型等。

项目结构参考：
```
achobeta-themis/
  .githooks/
  .github/workflows/
  docs/dev-ops/
  themis-app/
  themis-trigger/
  themis-domain/
  themis-infrastructure/
  themis-api/
  themis-common/
  pom.xml
  README.md
  LICENSE
```

## 环境准备
- 安装 `JDK 21` 与 `Git`。
- 安装 `Docker Desktop`（可选，用于一键启动依赖环境）。
- 推荐 IDE：IntelliJ IDEA（启用自动优化 import）。

初始化 Git 钩子（用于校验提交信息）：
```bash
git config core.hooksPath .githooks
chmod -R -x .githooks
```

## 快速开始
- 克隆仓库：
```bash
git clone <repo-url>
cd achobeta-themis
```

- 构建（跳过测试）：
```bash
# Windows
mvnw.cmd -T 1C -DskipTests clean package

# macOS/Linux
./mvnw -T 1C -DskipTests clean package
```

- 本地运行（开发模式）：
```bash
# 仅启动应用模块
mvnw.cmd -pl themis-app -am spring-boot:run
```

- 运行打包产物：
```bash
java -jar themis-app/target/themis-app.jar
```

## 配置说明
- 统一配置位置：`themis-app/src/main/resources/`
  - `application.yml`：通用配置
  - `application-dev.yml`：开发环境配置
  - `application-prod.yml`：生产环境配置
- 日志：`logback-spring.xml`
- 重要提示：请勿将包含密钥或敏感信息的真实配置提交到版本库（详见下文“安全与合规”）。

运行档案与端口：
- 默认激活 `dev` 运行档（参见 `application.yml`）。
- 开发端口：`611`（参见 `application-dev.yml`）。

建议以环境变量覆盖配置（在 Docker/CI 中）：
- `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT`
- `JWT_SECRET`
- 阿里云短信：`aliyun-access-key-id`、`aliyun-access-key-secret`、`aliyun-template-code`、`aliyun-sign-name`

## 数据库与外部依赖
- 数据库：`MySQL`（初始化 SQL 位于 `docs/dev-ops/mysql/init.sql`）
- 缓存：`Redis`
- 检索：`MeiliSearch`
- 对象存储与短信：`AliOSS` 与 `AliSms`（在 `themis-common` 提供工具类）

本地开发可通过 Docker 快速启动依赖：
```bash
docker compose -f docs/dev-ops/docker-compose-environment.yml up -d
```

端口（compose 默认）：
- MySQL：`13306 -> 3306`
- Redis：`16379 -> 6379`

## Docker 部署
- 启动应用容器（示例 Compose 文件）：
```bash
docker compose -f docs/dev-ops/docker-compose-app.yml up -d
```
- 如需自定义镜像与层级打包，可参考 `themis-app/Dockerfile` 与 Spring Boot Maven Plugin 配置。

容器环境变量示例：
- `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/themis?...`
- `SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT=611`
- `JWT_SECRET` 与阿里云短信密钥（建议使用密钥管理服务注入）

## 常用命令
- 清理与构建：`mvnw.cmd clean package -DskipTests`
- 运行单模块：`mvnw.cmd -pl themis-app -am spring-boot:run`
- 运行测试：`mvnw.cmd test`

## 架构与组件
- 拦截器：全局启用 `LogInterceptor`（配置于 `WebConfig`）。
- 认证：`LoginCheckAspect` 校验 `Authorization: Bearer <accessToken>`，并通过 `JwtUtil` 将用户信息保存到 SecurityContext。
- Spring Security：`SecurityConfig` 允许匿名访问 `/api/user/login`；可按需收紧权限策略。
- 线程：`ThreadPoolTaskExecutor` 用于异步任务（如裁判分类与历史触碰）。
- 响应与错误：统一 `ApiResponse` 与 `GlobalExceptionHandler` 处理业务与校验异常。

## 开发规范（分支与提交）
分支命名需包含责任人并明确工作内容：
```bash
<type>-<name>-<description>
```
示例：
- 新功能：
```bash
feature-<name>-<feature description>
e.g.: feature-jett-dev_log_system
```
- Bug 修复：
```bash
bugfix-<name>-<bug name>
e.g.: bugfix-jett-login_error
```
其他类型：`hotfix`、`release` 等。

提交信息需清晰且一次只做一件事：
```bash
<type>(<scope>): <subject>
e.g.: feat: add new api
     feat(common): add new api
```

类型约定：
```text
feat      增加新功能
fix       修复 bug
docs      文档改动
style     代码风格（不影响逻辑）
build     构建/依赖调整
refactor  代码重构
revert    回滚提交
# 暂不使用：test、perf、ci、chore
```

Subject 不以标点结尾，例如：
```bash
feat: add new feature
fix: fix a bug
```

- 内容规范：删除无用 import，IDEA 可用 `Ctrl + Alt + O` 优化。

## API 参考
- 认证与用户（`/api/user`）：
  - `POST /api/user/login` 登录，返回令牌。
  - `POST /api/user/logout` 使用 `refreshToken` 登出（需登录）。
  - `POST /api/user/logout-all` 批量注销所有设备（需登录）。
  - `POST /api/user/refresh-token` 刷新令牌。
  - `POST /api/user/send-verify-code` 发送短信验证码。
  - `POST /api/user/forget` 忘记密码。
  - `POST /api/user/change-password` 修改密码（需登录）。
  - `POST /api/user/change-username` 修改用户名（需登录）。
  - `GET /api/user/info?userId=...` 获取用户信息（需登录）。

- 聊天（`/api/chat`，除特殊说明外需登录）：
  - `POST /api/chat/consult` AI 问答。
  - `POST /api/chat/newOr` 生成新的对话 ID。
  - `POST /api/chat/new` 归档当前并新建对话。
  - `GET /api/chat/history?conversationId=...` 获取对话历史。
  - `GET /api/chat/histories` 列出用户对话元信息。
  - `DELETE /api/chat/history?conversationId=...` 重置对话历史。
  - `GET /api/chat/secondary_question_titles/{userType}` 常见问题列表。

- 文件审查（`/api/file`，需登录）：
  - `POST /api/file/upAndwrite` 上传文件并抽取文本（Apache Tika）。
  - `POST /api/file/review` 审查文本并返回结构化结果。
  - `POST /api/file/review/getId` 获取审查对话 ID。
  - `POST /api/file/review/record?flag=...` 保存审查记录。
  - `GET /api/file/review/records` 审查记录列表。
  - `GET /api/file/review/record?recordId=...` 审查记录详情。
  - `DELETE /api/file/review/record?recordId=...` 删除审查记录。
  - `POST /api/file/download` 下载生成的文件。

- 知识库（`/api/knowledgebs`，需登录）：
  - `GET /api/knowledgebs/query?question=...` 按问题查询。
  - `GET /api/knowledgebs/topic` 获取所有话题。
  - `GET /api/knowledgebs/case` 获取常见场景。

- MeiliSearch 调试（`/api/law/debug`）：
  - `GET /api/law/debug/check-index` 检查 `law_documents` 索引状态。
  - `GET /api/law/debug/list-indexes` 索引列表。
  - `POST /api/law/debug/test-add-one` 添加测试文档并等待任务。
  - `GET /api/law/debug/check-db-data` 数据库样本统计。

认证头部：
- 对标注 `@LoginRequired` 的接口，使用 `Authorization: Bearer <accessToken>`。

## 示例请求
- 登录：
```bash
curl -X POST http://localhost:611/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"******"}'
```
- 聊天咨询：
```bash
curl -X POST http://localhost:611/api/chat/consult \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"<id>","message":"...","userType":1}'
```
- 文件文本抽取：
```bash
curl -X POST http://localhost:611/api/file/upAndwrite \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -F file=@/path/to/contract.pdf
```

## 安全与合规
- 禁止在代码库提交任何敏感信息（如 `api_key`、地址、密码等）。
- `application-dev.yml` 与 `application-prod.yml` 的真实敏感配置请仅在团队内安全保存，禁止提交至 Git；一旦提交将永久留痕。
- 不要使用 `git push --force`，除非你非常确定其影响并已与团队达成共识。

## CI/CD
- 本仓库使用 GitHub Actions（见 `.github/workflows/`）进行构建与发布。
- 可根据需要扩展检查、测试与制品发布流程。

## 许可证
- 开源协议：Apache License 2.0（见 `LICENSE`）。
