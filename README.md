# JavaAgentStudy

面向初级 Java 开发者的渐进式 AI Agent 实战学习仓库。项目以 **Study Agent**（「学—练—评—记」四步学习闭环）为主线场景，按由浅入深的 Maven 多模块逐步拆解 Spring AI 核心原语，并最终整合成可运行的完整应用。

> 注：本仓库中的 **Agent** 统一指基于大语言模型的 **AI Agent**，而非 JVM 字节码 `javaagent`。

## 技术基线

- **语言与构建**：JDK 21 + Maven 3.10（根坐标 / 包名：`dev.javaagentstudy`）
- **框架版本**：Spring Boot 4.2.x + Spring AI 2.1.0 GA（见 [ADR-0001](./docs/adr/0001-spring-ai-2-1-on-spring-boot-4.md)）
- **模型运行时**：默认接入 OpenAI-compatible 云端 API（通过环境变量配置，不入库密钥），同时保留本地 `ollama` Profile
- **测试策略**：默认 `mvn test` 全离线、零费用通过；真实大模型与评估测试通过 `@Tag("live-llm")` 隔离按需触发

## 渐进式模块路线

详见 [ADR-0002](./docs/adr/0002-multi-module-progressive-stages.md)，各阶段按顺序独立演进：

| 模块 | 核心主题 | 状态 |
| :--- | :--- | :--- |
| `common` | 共享领域模型、默认初级 Java `Knowledge Source` 与离线测试替身 | 规划中（Milestone 1） |
| `stage-01-chat-and-structured-output` | `ChatClient` 同步 / SSE 流式讲解与强类型 `Exercise` 结构化输出 | 规划中（Milestone 1） |
| `stage-02-tools-and-advisors` | `@Tool`、`ToolCallingAdvisor`、手动循环对比、自定义 `BaseAdvisor` 与 `requiresConfirmation` 人工确认 | 待开启 |
| `stage-03-session-and-rag` | Spring AI 2.1 `Session API`、ETL Pipeline、模块化 RAG 与 `PostgreSQL + pgvector`（Docker Compose） | 待开启 |
| `stage-04-mcp` | 自建 Java MCP Server（`@McpTool` / Streamable HTTP）与 MCP Client 集成 | 待开启 |
| `stage-05-agentic-workflows` | Spring AI Agentic Workflow 模式（Routing、Evaluator-Optimizer 等） | 待开启 |
| `stage-06-observability-and-eval` | Micrometer / OpenTelemetry 观测与 `RelevancyEvaluator` / `FactCheckingEvaluator` 评估 | 待开启 |
| `study-agent-app` | 整合全部能力的完整 Study Agent 应用 | 待开启 |

> 本地启动与调试命令（Quick Start）将在 Milestone 1 首张代码票落地后补充。

## 核心文档索引

- 领域术语表：[GLOSSARY.md](./GLOSSARY.md)
- 架构决策记录：[ADR-0001（Spring AI 2.1 / Spring Boot 4.2 基线）](./docs/adr/0001-spring-ai-2-1-on-spring-boot-4.md)、[ADR-0002（Maven 多模块分阶段隔离）](./docs/adr/0002-multi-module-progressive-stages.md)
- 一手技术调研：[Java、AI Agent 与 Spring AI 调研报告](./docs/research/java-agent-spring-ai.md)
