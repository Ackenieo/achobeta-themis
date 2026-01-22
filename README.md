# AchoBeta Themis · 2025 J Group Interview Project

English | <a href="./README_CN.md">Chinese (Simplified)</a>

## Overview
- AchoBeta Themis is a Java 21 and Spring Boot 3 multi-module backend project. It applies layered domain architecture and provides engineering support including Docker and GitHub Actions.
- This document is the standard, comprehensive README covering tech stack, module layout, environment setup, quick start, configuration & deployment, development guidelines and security compliance.

## Project Goals & Features
- Labor law compliance assistant powered by AI, focusing on consultation and document review for individual and enterprise users.
- Core capabilities:
  - AI consultation chat with conversation history management.
  - Contract/content review: extract text via Apache Tika and generate structured compliance output.
  - Knowledge base query for topics and scenarios.
  - MeiliSearch integration for fast legal text retrieval and debugging utilities.
  - Secure endpoints with JWT and `@LoginRequired` aspect-based enforcement.

## Contents
- Overview & Tech Stack
- Modules
- Prerequisites
- Quick Start
- Configuration
- Databases & External Services
- Docker Deployment
- Common Commands
- Development Guidelines
- CI/CD
- License

## Tech Stack
- Language & Runtime: `Java 21`
- Framework: `Spring Boot 3.5.7`
- Build: `Maven` with wrapper (`mvnw` / `mvnw.cmd`)
- Data Access: `MyBatis`, `MyBatis-Plus`
- Cache & Queue: `Redisson` (Redis client), `Caffeine`
- Search: `MeiliSearch`
- Security & Utilities: `JWT`, `Hutool`, `Fastjson`
- Document Parsing: `Apache Tika`

## Modules (Maven multi-module)
- `themis-app`: Application entry and runtime, Spring Boot startup and Web integration, main class `com.achobeta.themis.Application`.
- `themis-trigger`: Trigger/adapter layer (Web), includes validation and document parsing, depends on `domain`, `api`, `common`, `infrastructure`.
- `themis-domain`: Domain layer defining core business logic, depends only on `common`.
- `themis-infrastructure`: Infrastructure layer for DB access and external integrations, depends on `domain` and `common`.
- `themis-api`: API contracts (request/response models), depends on `common`.
- `themis-common`: Shared module for exceptions, utilities, Redis scripts, common response models.

Reference structure:
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

## Prerequisites
- Install `JDK 21` and `Git`.
- Install `Docker Desktop` (optional, for one-click dependencies).
- Recommended IDE: IntelliJ IDEA (enable auto-optimize imports).

Initialize Git hooks (to validate commit messages):
```bash
git config core.hooksPath .githooks
chmod -R -x .githooks
```

## Quick Start
- Clone:
```bash
git clone <repo-url>
cd achobeta-themis
```

- Build (skip tests):
```bash
# Windows
mvnw.cmd -T 1C -DskipTests clean package

# macOS/Linux
./mvnw -T 1C -DskipTests clean package
```

- Run locally (dev):
```bash
# Start application module only
mvnw.cmd -pl themis-app -am spring-boot:run
```

- Run the packaged jar:
```bash
java -jar themis-app/target/themis-app.jar
```

## Configuration
- Centralized configs: `themis-app/src/main/resources/`
  - `application.yml`: common config
  - `application-dev.yml`: development
  - `application-prod.yml`: production
- Logging: `logback-spring.xml`
- Important: do not commit real secrets or sensitive configs (see Security & Compliance).

Profiles and port:
- Active profile defaults to `dev` (see `application.yml`).
- Dev server port: `611` (see `application-dev.yml`).

Environment overrides (recommended in Docker/CI):
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT`
- `JWT_SECRET`
- Aliyun SMS: `aliyun-access-key-id`, `aliyun-access-key-secret`, `aliyun-template-code`, `aliyun-sign-name`

## Databases & External Services
- Database: `MySQL` (init SQL in `docs/dev-ops/mysql/init.sql`)
- Cache: `Redis`
- Search: `MeiliSearch`
- Object storage & SMS: `AliOSS` and `AliSms` (utilities in `themis-common`)

Start dependencies with Docker:
```bash
docker compose -f docs/dev-ops/docker-compose-environment.yml up -d
```

Ports (compose defaults):
- MySQL: `13306 -> 3306`
- Redis: `16379 -> 6379`

## Docker Deployment
- Start application container (example):
```bash
docker compose -f docs/dev-ops/docker-compose-app.yml up -d
```
- For custom images and layered jars, refer to `themis-app/Dockerfile` and the Spring Boot Maven Plugin setup.

Container environment variables example:
- `SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/themis?...`
- `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SERVER_PORT=611`
- `JWT_SECRET` and Aliyun SMS keys (provide via secrets manager)

## Common Commands
- Clean & build: `mvnw.cmd clean package -DskipTests`
- Run single module: `mvnw.cmd -pl themis-app -am spring-boot:run`
- Run tests: `mvnw.cmd test`

## Architecture & Components
- Interceptors: `LogInterceptor` applied globally (`WebConfig`).
- Authentication: `LoginCheckAspect` validates `Authorization: Bearer <accessToken>` using `JwtUtil`; saves user info into SecurityContext.
- Spring Security: `SecurityConfig` allows `/api/user/login` anonymous; adjust for stricter access as needed.
- Threading: `ThreadPoolTaskExecutor` for async tasks (e.g., adjudication and history touch in chat).
- Responses & errors: unified `ApiResponse` plus `GlobalExceptionHandler` for business and validation errors.

## Development Guidelines
- Branch naming must include owner and clearly describe work:
```bash
<type>-<name>-<description>
```
Examples:
- New feature:
```bash
feature-<name>-<feature description>
e.g.: feature-jett-dev_log_system
```
- Bug fix:
```bash
bugfix-<name>-<bug name>
e.g.: bugfix-jett-login_error
```
Other types: `hotfix`, `release`.

- Commit messages must be clear and granular (one thing per commit):
```bash
<type>(<scope>): <subject>
e.g.: feat: add new api
     feat(common): add new api
```

- Types:
```text
feat      new feature
fix       bug fix
docs      docs only
style     code style (no logic change)
build     build/deps changes
refactor  refactoring
revert    revert commit
# Not used currently: test, perf, ci, chore
```

- Subjects should not end with punctuation, e.g.:
```bash
feat: add new feature
fix: fix a bug
```

- Content: remove unused imports, IDEA shortcut `Ctrl + Alt + O`.

## API Reference
- Authentication & User (`/api/user`):
  - `POST /api/user/login` login, returns tokens.
  - `POST /api/user/logout` logout with `refreshToken` (requires login).
  - `POST /api/user/logout-all` logout all sessions (requires login).
  - `POST /api/user/refresh-token` refresh tokens.
  - `POST /api/user/send-verify-code` send SMS code.
  - `POST /api/user/forget` reset password.
  - `POST /api/user/change-password` change password (requires login).
  - `POST /api/user/change-username` change username (requires login).
  - `GET /api/user/info?userId=...` get user info (requires login).

- Chat (`/api/chat`, requires login unless specified):
  - `POST /api/chat/consult` AI Q&A.
  - `POST /api/chat/newOr` create a new conversation ID.
  - `POST /api/chat/new` archive current and start new conversation.
  - `GET /api/chat/history?conversationId=...` get chat history.
  - `GET /api/chat/histories` list user conversation metas.
  - `DELETE /api/chat/history?conversationId=...` reset conversation history.
  - `GET /api/chat/secondary_question_titles/{userType}` list common questions.

- File Review (`/api/file`, requires login):
  - `POST /api/file/upAndwrite` upload file and extract text (Apache Tika).
  - `POST /api/file/review` review content and return structured result.
  - `POST /api/file/review/getId` create a review conversation ID.
  - `POST /api/file/review/record?flag=...` save review record.
  - `GET /api/file/review/records` list review records.
  - `GET /api/file/review/record?recordId=...` get record detail.
  - `DELETE /api/file/review/record?recordId=...` delete record.
  - `POST /api/file/download` download generated file.

- Knowledge Base (`/api/knowledgebs`, requires login):
  - `GET /api/knowledgebs/query?question=...` query by question string.
  - `GET /api/knowledgebs/topic` list topics.
  - `GET /api/knowledgebs/case` list common scenarios.

- MeiliSearch Debug (`/api/law/debug`):
  - `GET /api/law/debug/check-index` check `law_documents` index stats.
  - `GET /api/law/debug/list-indexes` list indexes.
  - `POST /api/law/debug/test-add-one` add a test document and wait for task.
  - `GET /api/law/debug/check-db-data` sample DB data counts.

Authentication header:
- Use `Authorization: Bearer <accessToken>` on endpoints annotated with `@LoginRequired`.

## Sample Requests
- Login:
```bash
curl -X POST http://localhost:611/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"******"}'
```
- Chat consult:
```bash
curl -X POST http://localhost:611/api/chat/consult \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"<id>","message":"...","userType":1}'
```
- File extract text:
```bash
curl -X POST http://localhost:611/api/file/upAndwrite \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -F file=@/path/to/contract.pdf
```

## Security & Compliance
- Never commit secrets (e.g., `api_key`, addresses, passwords) to the repo.
- Keep real sensitive configs of `application-dev.yml` and `application-prod.yml` within the team only; do not push to Git.
- Avoid `git push --force` unless you fully understand the impact and have team agreement.

## CI/CD
- GitHub Actions is configured under `.github/workflows/` for build and deploy.
- Extend checks, tests and artifact publishing as needed.

## License
- Apache License 2.0 (see `LICENSE`).
