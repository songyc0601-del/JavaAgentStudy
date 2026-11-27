# JavaAgentStudy

面向初级 Java 开发者的渐进式 AI Agent 实战学习仓库。项目以 **Study Agent**（「学—练—评—记」四步学习闭环）为主线场景，按由浅入深的 Maven 多模块逐步拆解 Spring AI 核心原语，并最终整合成可运行的完整应用。

> 注：本仓库中的 **Agent** 统一指基于大语言模型的 **AI Agent**，而非 JVM 字节码 `javaagent`。

## 技术基线

- **语言与构建**：JDK 21 + Maven 3.10（根坐标 / 包名：`dev.javaagentstudy`）
- **框架版本**：Spring Boot 4.2.x + Spring AI 2.1.0 GA（见 [ADR-0001](./docs/adr/0001-spring-ai-2-1-on-spring-boot-4.md)）
- **模型运行时**：默认接入 OpenAI-compatible 云端 API（通过环境变量配置，不入库密钥）（`stage-01` 暂不包含本地 `ollama` Profile，后续按需再引入）
- **测试策略**：默认 `mvn test` 全离线、零费用通过；真实大模型与评估测试通过 `@Tag("live-llm")` 隔离按需触发

## 渐进式模块路线

详见 [ADR-0002](./docs/adr/0002-multi-module-progressive-stages.md)，各阶段按顺序独立演进：

| 模块 | 核心主题 | 状态 |
| :--- | :--- | :--- |
| `common` | 共享领域模型、默认初级 Java `Knowledge Source` 与离线测试替身 | 已完成（Milestone 1 / Ticket 2，`Knowledge Source` 加载见下文） |
| `stage-01-chat-and-structured-output` | `ChatClient` 同步 / SSE 流式讲解与强类型 `Exercise` 结构化输出 | 进行中（Milestone 1：同步与 SSE 流式讲解、`Exercise` 结构化生成接口、`live-llm` 真实模型端到端测试与[学习导读](./stage-01-chat-and-structured-output/README.md)已完成） |
| `stage-02-tools-and-advisors` | `@Tool`、`ToolCallingAdvisor`、手动循环对比、自定义 `BaseAdvisor` 与 `requiresConfirmation` 人工确认 | 待开启 |
| `stage-03-session-and-rag` | Spring AI 2.1 `Session API`、ETL Pipeline、模块化 RAG 与 `PostgreSQL + pgvector`（Docker Compose） | 待开启 |
| `stage-04-mcp` | 自建 Java MCP Server（`@McpTool` / Streamable HTTP）与 MCP Client 集成 | 待开启 |
| `stage-05-agentic-workflows` | Spring AI Agentic Workflow 模式（Routing、Evaluator-Optimizer 等） | 待开启 |
| `stage-06-observability-and-eval` | Micrometer / OpenTelemetry 观测与 `RelevancyEvaluator` / `FactCheckingEvaluator` 评估 | 待开启 |
| `study-agent-app` | 整合全部能力的完整 Study Agent 应用 | 待开启 |

## Quick Start

```bash
# 1. 离线运行全部测试（无需网络、API Key，零 Token 费用；默认排除 @Tag("live-llm")）
mvn test

# 2. 准备云端模型配置：复制模板并填入真实值（.env 已被 Git 忽略，切勿提交）
cp .env.example .env
set -a && source .env && set +a

# 3. 启动 stage-01（默认使用 OpenAI-compatible 云端端点）
mvn -pl stage-01-chat-and-structured-output -am spring-boot:run

# 启动后可在浏览器访问：
# - http://localhost:8080/                 打开概念讲解验证页面
# - http://localhost:8080/swagger-ui.html  打开 Swagger UI 在线查看与调试接口
# - http://localhost:8080/v3/api-docs      查看 OpenAPI 3 JSON 文档
# 或直接通过 curl 调用同步概念讲解接口：
curl -X POST http://localhost:8080/api/explanations \
  -H 'Content-Type: application/json' \
  -d '{"topic":"集合框架","focus":"ArrayList 与 LinkedList 的区别"}'

# 5. 调用 SSE 流式概念讲解接口（-N 关闭 curl 缓冲，才能看到逐段输出）
curl -N -X POST http://localhost:8080/api/explanations/stream \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"topic":"集合框架","focus":"ArrayList 与 LinkedList 的区别"}'
```

## SSE 流式概念讲解（`stage-01`）

`POST /api/explanations/stream` 与同步接口 `POST /api/explanations` 使用同一套入参和面向初级 Java `Learner` 的讲解提示词，区别是模型每生成一段文本就立即推送一段。

- **请求**：JSON 请求体，必填 `topic`，可选 `focus`。
- **响应**：`Content-Type: text/event-stream`，每个文本分片对应一个 SSE 事件（`data:` 行），按模型生成顺序推送；客户端把所有 `data` 按序拼接即得完整讲解。SSE 规范规定 `data:` 后的一个前导空格是字段分隔符，解析时需去掉。
- **失败时的行为**：
  - `topic` 为空白或缺失、请求体不是合法 JSON：返回 HTTP 400（JSON 错误体，`error` 为 `VALIDATION_FAILED` 或 `MALFORMED_REQUEST`），不会调用模型。即使客户端带了 `Accept: text/event-stream`，错误体也固定为 `application/json`。
  - 流式调用中模型出错：`Flux` 在订阅后才产生错误，此时响应通常已开始写出，HTTP 状态码无法保证为 502；已推送的分片会保留，流随后异常终止。服务端会把错误转换为 `ModelCallException` 并记录 `ERROR` 级别日志（含已发送分片数、总字符数、耗时与异常堆栈），不向客户端返回上游异常原文。
- **调试日志**：`stage-01` 在 `application.yml` 中默认将 `dev.javaagentstudy.stage01` 设为 `DEBUG`（可通过环境变量 `STAGE01_LOG_LEVEL` 覆盖），以简体中文记录接口入口、提示词分支、400/502 异常分支、SSE 分片序号与字符数，以及流式完成/取消/失败状态；提示词正文与模型输出正文不会写入日志。
- **当前限制**：流中途出错时尚未向客户端推送专门的错误事件，客户端只能感知到连接异常结束。
- **离线测试**：`common` 的 `FakeChatModel` 提供 `streamingChunks(...)`（按序回放预设分片）和 `streamingChunksThenFailing(...)`（回放分片后以预设异常终止），流式接口测试无需网络与 API Key。

## 结构化 Exercise 生成（`stage-01`）

`POST /api/exercises` 让 `Study Agent` 围绕指定 `Topic` 生成一道强类型 `Exercise`，而不是松散文本。

- **请求**：JSON 请求体，必填 `topic`，可选 `difficulty`（`BEGINNER` / `INTERMEDIATE` / `ADVANCED`，缺省为 `BEGINNER`）与 `focus`。
- **响应**：HTTP 200，JSON 包含 `title`、`topic.name`、`difficulty`、`description`、`hints`、`referenceAnswerPoints`。
- **实现**：通过 `StructuredOutputValidationAdvisor` 按 `Exercise` 的 JSON Schema 校验模型输出，不合法时携带错误信息让模型重试（最多重试 2 次），再用 `BeanOutputConverter` 绑定为 `Exercise`。
- **失败时的行为**：
  - `topic` 空白或缺失、`difficulty` 取值非法、请求体不是合法 JSON：返回 HTTP 400（`VALIDATION_FAILED` 或 `MALFORMED_REQUEST`），不会调用模型。
  - 重试用尽后输出仍无法解析：返回 HTTP 502，`error` 为 `EXERCISE_GENERATION_FAILED`。
  - 模型调用抛异常：返回 HTTP 502，`error` 为 `MODEL_CALL_FAILED`；响应中不含上游异常原文。
- **`focus`**：非空白时会追加到提示词，让题目围绕该关注重点；为空或缺失时只按 `topic` 与难度出题。

## OpenAPI / Swagger UI 接口文档（`stage-01`）

`stage-01` 集成了 `springdoc-openapi-starter-webmvc-ui`（3.2.1，适配 Spring Boot 4.2.x；版本由父工程 `pom.xml` 统一管理，供后续阶段模块按需引入），自动扫描 REST 接口与数据结构并暴露交互式接口文档。

- **访问地址**（启动 `stage-01` 后）：
  - **Swagger UI**：`http://localhost:8080/swagger-ui.html`（自动重定向到 `/swagger-ui/index.html`）
  - **OpenAPI 3 JSON**：`http://localhost:8080/v3/api-docs`
- **已覆盖接口**：
  - `POST /api/explanations`（同步生成概念讲解，含 200 / 400 / 502 响应与 Schema 示例）
  - `POST /api/explanations/stream`（SSE 流式生成概念讲解）
  - `POST /api/exercises`（生成结构化 `Exercise`，含 200 / 400 / 502 响应与 Schema 示例）
- **配置与开关**：本地默认启用；如部署到不希望暴露接口文档的环境，可通过配置关闭：
  - `springdoc.api-docs.enabled=false`
  - `springdoc.swagger-ui.enabled=false`
- **使用说明与限制**：Swagger UI 适合浏览字段定义与直接调试普通 JSON 接口（如 `/api/explanations`、`/api/exercises`）；调试 `POST /api/explanations/stream` 的逐段 SSE 推送效果时，仍建议使用内置页面 `http://localhost:8080/` 或 `curl -N`。

## 真实模型端到端测试与学习导读（`stage-01`）

- **学习导读**：[stage-01 学习导读](./stage-01-chat-and-structured-output/README.md) 解释 `ChatClient`、System/User Prompt、SSE 流式输出与 Structured Output 的核心概念，并提供同步讲解、SSE 流式讲解、`Exercise` 生成与非法入参 400 四类 curl 示例。
- **`live-llm` 测试**：`LiveLlmEndToEndTest` 不注入 `FakeChatModel`，通过 HTTP 入口直连 `OPENAI_*` 环境变量（或根目录 `.env`）配置的真实端点，覆盖同步讲解、SSE 流式讲解、`Exercise` 生成与 400 入参四个用例。默认 `mvn test` 自动排除该 Tag，无需 API Key，也不产生网络请求与费用。
- **显式运行**（会消耗 Token，约 1 分钟）：

```bash
set -a && source .env && set +a
mvn -B -ntp -pl stage-01-chat-and-structured-output -am test \
  -Dsurefire.excludedGroups= -Dgroups=live-llm \
  -Dtest=LiveLlmEndToEndTest -Dsurefire.failIfNoSpecifiedTests=false
```

- **失败时的行为**：未设置 `OPENAI_API_KEY` 时，OpenAI 自动装配要求必须有 API Key，Spring 上下文会在启动阶段失败，四个用例都不会执行；API Key 无效、端点不可达或模型报错时，接口返回 HTTP 502（`MODEL_CALL_FAILED`），对应用例失败。这属于预期，说明环境未就绪。
- **当前限制**：`stage-01` 暂不提供本地 `ollama` 方式，导读只覆盖云端 OpenAI-compatible 端点。

## Knowledge Source 加载（`common`）

`KnowledgeSourceLoader`（包 `dev.javaagentstudy.common.knowledge`）负责加载供 Study Agent 检索与引用的 `Knowledge Source`。

- **内置资料**：`common/src/main/resources/knowledge/` 下共 4 篇初级 Java `Topic` 的 Markdown（集合框架与 `HashMap`、JVM 内存基础、并发入门、Spring IoC/AOP 常见误区）。每篇用 frontmatter 声明 `id`、`title`、`topic`，缺失任一字段或正文为空会在加载时抛出 `IllegalStateException`。
- **用法**：
  - `loader.load()`：只返回内置资料。
  - `loader.load(Path externalDirectory)`：内置资料在前，外部笔记按文件名排序追加在后。
- **外部笔记规则**：
  - 只读取目录第一层的 `.md` / `.txt` 文件（扩展名不区分大小写）。
  - 自动忽略隐藏文件、子目录、其他扩展名和纯空白笔记。
  - 可选 frontmatter 提供 `id`、`title`、`topic`；缺失时 `title` 依次回退为首个一级标题、文件名，`topic` 回退为 `title`。
  - `id` 与已有资料冲突时自动改为 `external:<文件名>`（必要时追加数字后缀）。
- **失败时的行为**：外部目录为 `null`、不存在、不是目录或为空时，返回内置默认集合，不抛异常。目录无法列出或单个文件不可读时，记录 WARNING 日志后跳过，其余资料照常返回。
- **当前限制**：外部目录只能通过 `load(Path)` 参数传入，尚无配置属性（如 `application.yml`）；符号链接未做专门处理。

## 核心文档索引

- 领域术语表：[GLOSSARY.md](./GLOSSARY.md)
- 架构决策记录：[ADR-0001（Spring AI 2.1 / Spring Boot 4.2 基线）](./docs/adr/0001-spring-ai-2-1-on-spring-boot-4.md)、[ADR-0002（Maven 多模块分阶段隔离）](./docs/adr/0002-multi-module-progressive-stages.md)
- 一手技术调研：[Java、AI Agent 与 Spring AI 调研报告](./docs/research/java-agent-spring-ai.md)
