# Java、AI Agent 与 Spring AI 一手资料调研报告

> **调研基准日期**：2026-11-27  
> **来源约束**：本文严格以官方文档、官方工程博客、官方 GitHub 仓库与规范为事实依据；任何跨版本差异或未在一手文档中完全确认的细节均已显式标注。

---

## 1. 概述与核心结论

### 1.1 Spring AI 在 Java AI Agent 体系中的定位

[Spring AI](https://spring.io/projects/spring-ai) 是 Spring 官方推出的 AI 工程应用框架，核心目标是将 Spring 生态的可移植性、模块化设计与依赖注入范式引入生成式 AI 与 Agent 开发领域，使 Java 开发者能够在不绑定单一模型厂商的前提下，以 POJO、Spring Boot Auto-Configuration 和统一抽象构建企业级 AI 应用与 Agent 系统（来源：[Spring AI Project Page](https://spring.io/projects/spring-ai)、[Spring AI Reference Documentation](https://docs.spring.io/spring-ai/reference/)）。

在 Java AI Agent 技术栈中，Spring AI 的定位具有两个显著特征：

1. **基础设施与原语层（Core Primitives）**：官方核心仓库 [`spring-projects/spring-ai`](https://github.com/spring-projects/spring-ai) 重点提供模型抽象（`ChatModel`、`EmbeddingModel`、`DecisionModel` 等）、统一入口 `ChatClient`、Tool Calling、Advisor 拦截与递归循环、Session/Memory、RAG、Structured Output、MCP（Model Context Protocol）集成、可观测性与评估原语。
2. **克制而可组合的 Agentic 工程哲学**：Spring AI 团队并未在核心框架内强行内置重型黑盒多智能体运行时，而是先落地 Anthropic 提出的五种可组合 Agentic Workflow 模式，再通过 **Recursive Advisors（递归拦截器）**、**Tool Calling 架构解耦**、**Session API** 与 **Tool Call Confirmation** 补齐自主 Agent 所需的基础原语，同时通过社区扩展仓库 [`spring-ai-community/spring-ai-agent-utils`](https://github.com/spring-ai-community/spring-ai-agent-utils) 探索 Agent Skills、Subagents 与 A2A 协议集成（来源：[Building Effective Agents with Spring AI (Part 1)](https://spring.io/blog/2025/01/21/building-effective-agents-with-spring-ai-part-1)、[Spring AI 2.0.0 GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-ga-available-now)、[Spring AI 2.1.0 GA and 2.0.3 GA](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-ga-and-2-0-3-ga-available-now)）。

### 1.2 版本演进线与运行环境要求（截至 2026-11-27）

根据 Spring 官方博客发布记录与 [`spring-projects/spring-ai`](https://github.com/spring-projects/spring-ai) 仓库分支映射，Spring AI 当前存在四条主要发布线：

| 版本线 | 里程碑 / 最新发布 | 对应 Spring Boot / Framework | Java 版本要求 | 核心架构定位与变化 | 一手来源 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1.0.x** | `1.0.0 GA`（2025-05-20） | Spring Boot 3.x / Spring Framework 6.x | Java 17+ | 首个 GA 版本，确立 `ChatClient`、`ToolCallback`、`CallAdvisor`、RAG、MCP 等基础 API | [Spring Blog (2025-05-20)](https://spring.io/blog/) / [GitHub Releases](https://github.com/spring-projects/spring-ai/releases) |
| **1.1.x** | `1.1.0 GA`（2025-11-12） | Spring Boot 3.5.x / Spring Framework 6.x | Java 17+ | 面向 Spring Boot 3.x 的功能增强版，引入 Recursive Advisors 基础与更完善的 MCP 支持 | [Spring Blog (2025-11-12)](https://spring.io/blog/) / [GitHub README](https://github.com/spring-projects/spring-ai) |
| **2.0.x** | `2.0.0 GA`（2026-06-12）<br>`2.0.3 GA`（2026-11-20，含 13 项 CVE 修复） | Spring Boot 4.0.x / 4.1.x<br>Spring Framework 7.x | Java 17 最低，强烈推荐 Java 21+（见下方说明） | 面向 Agent 时代的重大架构升级：迁移至 Jackson 3 与 JSpecify 空安全；移除模型内部工具循环 `internalToolExecutionEnabled`，由 `ToolCallingAdvisor` 统一托管；不可在 Spring Boot 3.x 中加载 | [Spring AI 2.0.0 GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-ga-available-now)<br>[Spring AI 2.1.0 & 2.0.3 GA](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-ga-and-2-0-3-ga-available-now) |
| **2.1.x** | `2.1.0 GA`（2026-11-20） | Spring Boot 4.2.x<br>Spring Framework 7.1.x | Java 17 最低 / Java 21+ 推荐（部分新版入门文档指向 Java 21+） | 引入 Session API（替代弃用的 `ChatMemory`）、Tool Call Confirmation（人工确认暂停）、按轮次工具调用上限、有序 `MessagePart`（`ReasoningPart` / `ToolCallPart`）、OpenAI Responses API、Decision Models、MCP 2026-07-28 无状态规范支持、User-Supplied Embeddings | [Spring AI 2.1.0 GA and 2.0.3 GA](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-ga-and-2-0-3-ga-available-now) |

> **Java 版本要求核实说明**：Spring AI 1.x 与 Spring Framework 6/7 的基线一致，最低支持 Java 17，但官方在 2.0/2.1 系列强烈推荐 Java 21+（以配合 Virtual Threads 等现代特性），且部分 2.x Getting Started 文档条目已按 Java 21+ 书写。新立项实验项目（如 `JavaAgentStudy`）建议直接使用 **JDK 21 LTS**，并在选定具体 BOM 版本后核对对应 `spring-ai` 仓库根 `pom.xml` 的编译基线（来源：[Spring AI Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)、[spring-projects/spring-ai](https://github.com/spring-projects/spring-ai)）。

### 1.3 依赖管理（Spring AI BOM）

官方推荐使用 `org.springframework.ai:spring-ai-bom` 统一管理各模型 Starter、VectorStore Starter 与 MCP Starter 版本，避免与 Spring Boot 依赖冲突（来源：[Spring AI Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)）：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <!-- 若基于 Spring Boot 4.2.x，使用 2.1.0；若基于 Spring Boot 3.5.x，使用 1.1.x -->
            <version>2.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Gradle（`build.gradle`）对应写法：

```groovy
dependencies {
    implementation platform("org.springframework.ai:spring-ai-bom:2.1.0")
    implementation "org.springframework.ai:spring-ai-starter-model-openai"
}
```

### 1.4 关键 API 演进与弃用速查表（1.0 M/GA → 2.0 → 2.1）

由于网络上大量存量资料仍停留在早期里程碑或 1.0 写法，下表汇总了经官方参考文档与升级指南确认的核心 API 变更：

| 领域 | 旧 API / 已弃用或已移除 | 现行推荐 API | 变更版本与说明 | 一手来源 |
| :--- | :--- | :--- | :--- | :--- |
| 工具回调抽象 | `FunctionCallback`、`FunctionCallback.builder().function(...)` | `ToolCallback`、`FunctionToolCallback`、`MethodToolCallback`、`@Tool` | 已在 1.0 演进中弃用并移除，术语全面统一为 Tool | [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) / [Tools API](https://docs.spring.io/spring-ai/reference/api/tools.html) |
| Advisor 核心接口 | `CallAroundAdvisor`、`StreamAroundAdvisor`、`AdvisedRequest`、`AdvisedResponse` | `CallAdvisor`、`StreamAdvisor`、`BaseAdvisor`、`ChatClientRequest`、`ChatClientResponse` | 1.0 稳定版前完成精简重命名 | [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) / [Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html) |
| 工具执行循环 | 模型侧 `internalToolExecutionEnabled`；以及 1.1/早期 `ToolCallAdvisor` | `ToolCallingAdvisor` + `ToolCallingManager` | **2.0.0** 彻底移除模型内部循环及 `internalToolExecutionEnabled`；`ToolCallAdvisor` 自 2.0.0 起弃用并标记待移除 | [Tools API](https://docs.spring.io/spring-ai/reference/api/tools.html) / [Spring AI 2.0.0 GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-ga-available-now) |
| 对话记忆 / 会话 | `ChatMemory`、`ChatMemoryRepository`、`MessageChatMemoryAdvisor` | `org.springframework.ai.session.SessionService`、`SessionRepository`、`SessionMemoryAdvisor` / `SessionAdvisors` | **2.1.0** 起弃用旧 `ChatMemory` 体系（计划 3.0.0 移除），以事件日志与按轮次压缩的 Session API 替代；`VectorStoreChatMemoryAdvisor` 仍保留支持 | [Chat Memory Reference](https://docs.spring.io/spring-ai/reference/api/chat-memory.html) / [Spring AI 2.1.0 GA](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-ga-and-2-0-3-ga-available-now) |
| RAG 文档后处理 | 分散的 `DocumentCompressor`、`DocumentRanker`、`DocumentSelector` | 统一函数式接口 `DocumentPostProcessor` | 现代模块化 RAG 统一收敛为后处理阶段抽象 | [RAG Reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html) |
| MCP HTTP 传输 | 旧版双通道 HTTP + SSE | 单端点 **Streamable HTTP**（含 `streamable` / `stateless`） | 顺应 MCP 规范（含 2026-07-28 无状态修订）成为推荐远程传输方式 | [MCP Spec](https://modelcontextprotocol.io/) / [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) |

---

## 2. Java 视角下的 AI Agent 与 Spring AI Agentic 架构

### 2.1 Workflows 与 Autonomous Agents 的界定

Spring AI 官方博客系列 [*Building Effective Agents with Spring AI (Part 1)*](https://spring.io/blog/2025/01/21/building-effective-agents-with-spring-ai-part-1) 直接承接 Anthropic 工程文章 [*Building Effective Agents*](https://www.anthropic.com/engineering/building-effective-agents) 的分类法，将广义的 **Agentic Systems** 明确划分为两类：

- **Workflows（确定性工作流）**：由预先编写好的 Java 代码路径编排 LLM 调用与工具执行。控制流由开发者显式决定，模型只在节点内部执行推理、分类或生成；具备高可预测性、低调试成本和明确 SLA，是多数企业级生产场景的首选起点。
- **Autonomous Agents（自主智能体）**：由 LLM 自身动态决定下一步流程、选择调用哪些工具、评估环境反馈并决定何时结束循环。适用于步骤难以预先穷举的开放式任务，但伴随着更高的延迟、Token 成本、错误累积与安全边界管控难度。

两篇一手文献共同强调的核心工程原则是：**优先使用简单、可组合的原语与工作流模式，仅在任务确实需要开放式动态决策时再升级到自主 Agent 循环**（来源：[Anthropic - Building Effective Agents](https://www.anthropic.com/engineering/building-effective-agents)、[Spring Blog - Building Effective Agents with Spring AI (Part 1)](https://spring.io/blog/2025/01/21/building-effective-agents-with-spring-ai-part-1)）。

### 2.2 Spring AI 官方落地的五种核心 Agentic Workflow 模式

Spring AI 官方示例仓库 [`spring-projects/spring-ai-examples/tree/main/agentic-patterns`](https://github.com/spring-projects/spring-ai-examples/tree/main/agentic-patterns) 基于 `ChatClient`、Structured Output（`.entity(...)`）与标准 Java 并发原语，完整实现了五种经典模式（来源：[Building Effective Agents with Spring AI (Part 1)](https://spring.io/blog/2025/01/21/building-effective-agents-with-spring-ai-part-1)、[spring-ai-examples/agentic-patterns](https://github.com/spring-projects/spring-ai-examples/tree/main/agentic-patterns)）：

1. **Chain Workflow（Prompt Chaining / 链式工作流）**
   - **机制**：将复杂任务拆分为有序步骤数组，前一步 `ChatClient` 的文本或结构化输出作为后一步的输入上下文，并可在步骤间插入确定性的 Java 校验门控（Gate）。
   - **适用场景**：多阶段文档加工、格式归一化、需求抽取 → 方案生成 → 合规审查。
2. **Parallelization Workflow（并行工作流）**
   - **机制**：借助 Java 并发机制（如 `ExecutorService` / `CompletableFuture` 及 Java 21 虚拟线程）将独立子任务（Sectioning 分片）或同一任务的多视角评审（Voting 投票）并行分发给多个 `ChatClient` 调用，再由 Java 代码聚合结果。
   - **适用场景**：批量文本分块摘要、多维度安全/护栏并行校验、多候选方案投票。
3. **Routing Workflow（路由工作流）**
   - **机制**：先通过一次低延迟 LLM 调用结合 Structured Output 对用户意图进行分类，输出路由键与推理依据，再按路由表分发给专用的系统提示词、专用工具集或不同规格模型处理。
   - **适用场景**：客服工单分流（账单 / 技术故障 / 退款）、简单查询走轻量模型与复杂推理走旗舰模型。
4. **Orchestrator-Workers（编排者-执行者模式）**
   - **机制**：由中央 Orchestrator LLM 动态分析任务并生成结构化的子任务清单（无法在编译期静态写死子任务数），程序将各子任务并发派发给 Worker LLM 执行，最后交由综合步骤汇总结果。
   - **适用场景**：跨多文件代码修改分析、多源主题调研报告生成。
5. **Evaluator-Optimizer（评估者-优化者循环）**
   - **机制**：Generator LLM 产出一版候选结果，Evaluator LLM 按明确标准打分并给出结构化修改建议（`PASS` / `NEEDS_IMPROVEMENT` + feedback）；若未达标则携带历史反馈进入下一轮优化，直至通过或达到最大迭代次数。
   - **适用场景**：代码生成与静态/语义审查迭代、翻译润色、高合规要求文案生成。

### 2.3 向自主 Agent 的官方与社区演进（2025–2026）

从 1.1 到 2.1，Spring AI 核心及官方社区围绕“真正可控的自主 Agent”补齐了四大关键能力：

1. **核心层：Recursive Advisors 与 `ToolCallingAdvisor` 驱动的循环**
   - 自 Spring AI 1.1 引入递归拦截器机制、2.0 将工具循环正式上移为 `ToolCallingAdvisor` 后，单次 `ChatClient` 调用即可在 Advisor 链内部围绕下游模型调用执行多轮自主循环（如多步工具调用、结构化输出失败自修复），且不会重复触发上游的内存/鉴权等前置 Advisor（来源：[Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html)、[Tools API](https://docs.spring.io/spring-ai/reference/api/tools.html)）。
2. **核心层（2.1.0 GA）：Human-in-the-Loop 工具确认、轮次护栏与有序 `MessagePart`**
   - **Tool Call Confirmation**：在工具上声明 `requiresConfirmation = true` 后，工具执行会在高风险操作前暂停以等待人工审批，弥补了自主 Agent 在企业生产环境中的关键安全缺口。
   - **Turn-Based Tool Call Limits**：提供按轮次限制工具调用次数的护栏，防止模型陷入无限工具调用死循环。
   - **Richer Message Model & Decision Models**：消息体采用有序 `MessagePart`（如 `ReasoningPart`、`ToolCallPart`）完整保留推理链与工具调用上下文；新增 `org.springframework.ai.decision` API 支持带概率输出的是/否、多选与评分决策（来源：[Spring AI 2.1.0 GA and 2.0.3 GA](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-ga-and-2-0-3-ga-available-now)）。
3. **社区扩展层（`spring-ai-community/spring-ai-agent-utils`）：Agent Skills、Subagents 与 A2A**
   - 注意：该库归属 `spring-ai-community` 组织（[`spring-ai-community/spring-ai-agent-utils`](https://github.com/spring-ai-community/spring-ai-agent-utils)），并非 `spring-projects/spring-ai` 核心模块，但由 Spring AI 团队成员在 Spring 官方博客系列中重点推介：
     - **Agent Skills（`SkillsTool`）**：借鉴 Claude Code 范式，将领域流程封装为含 `SKILL.md`（YAML frontmatter + Markdown 指令）、脚本与参考文档的目录；启动时仅暴露轻量元数据，命中任务时按需渐进式披露（Progressive Disclosure）全文，避免系统提示词膨胀，且跨模型可移植（来源：[Spring Blog - Agent Skills (2026-01-13)](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills)）。
     - **Subagent Orchestration（`TaskTool` / `TaskTools`）**：通过 Markdown 配置定义专用子代理，由主代理经 `Task` 工具派发子任务，在隔离上下文窗口内运行并可配置不同模型，避免长链路任务污染主上下文（来源：[Spring Blog - Subagent Orchestration (2026-01-27)](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents)）。
     - **A2A（Agent-to-Agent）协议集成（`spring-ai-agent-utils-a2a`）**：通过统一的子代理 SPI 集成开放 A2A Java SDK，使主代理能像调用本地 Subagent 一样发现（基于 `.well-known/agent-card.json`）并委托跨进程/跨系统的远程异构 Agent，经 HTTP/SSE/JSON-RPC 协作（来源：[spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils)、[Spring Blog (2026-01-27)](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents)）。

---

## 3. Spring AI 构建 Agent 的核心原语与一手 API 拆解

### 3.1 `ChatClient` Fluent API 与模型抽象（同步与流式）

`ChatClient` 是面向应用开发者的统一流式/流畅门面（Fluent API），底层封装可移植的 `ChatModel` / `StreamingChatModel` 接口（来源：[ChatClient API Reference](https://docs.spring.io/spring-ai/reference/api/chatclient.html)）。

- **构建方式**：Spring Boot 自动装配 `ChatClient.Builder`，允许配置默认系统提示词（`defaultSystem`）、默认 Advisor（`defaultAdvisors`）与默认工具（`defaultTools` / `defaultToolCallbacks`）。多模型场景下可通过 `ChatClient.builder(specificChatModel)` 构造多个独立客户端。
- **同步与流式调用**：
  - 同步（`call()`）：可取纯文本 `.content()`、完整响应元数据 `.chatResponse()`（含 Token 用量、Finish Reason）或强类型对象 `.entity(MyRecord.class)`。
  - 流式（`stream()`）：返回基于 Project Reactor 的 `Flux<String>`（`.content()`）或 `Flux<ChatResponse>`（`.chatResponse()`），天然适配 Spring WebFlux / SSE 输出。

```java
// 构建与调用示意（基于 Spring AI Reference API）
ChatClient chatClient = chatClientBuilder
    .defaultSystem("You are a concise Java architecture assistant.")
    .build();

// 同步调用
String text = chatClient.prompt()
    .user("Summarize Spring AI ToolCallingAdvisor.")
    .tools(myToolsBean)
    .call()
    .content();

// 流式调用
Flux<String> tokenStream = chatClient.prompt()
    .user("Explain Advisor execution order.")
    .stream()
    .content();
```

### 3.2 Tool Calling 机制（`@Tool`、`ToolCallback`、`ToolCallingManager` 与循环控制）

Tool Calling 是 LLM 与外部系统交互的核心通道，Spring AI 始终坚持**工具代码在应用侧安全执行，模型只负责发出结构化调用意图与参数**（来源：[Tools API Reference](https://docs.spring.io/spring-ai/reference/api/tools.html)）。

1. **工具定义双路径**：
   - **声明式 `@Tool`**：在普通 Java Bean 方法上标注 `@Tool(description = "...")`（配合 `@ToolParam` 描述参数），Spring AI 自动生成 JSON Schema 并封装为 `MethodToolCallback`；在 2.1.0 中还支持 `requiresConfirmation = true` 触发人工确认。
   - **编程式 `ToolCallback`**：底层统一抽象接口，封装 `ToolDefinition`（名称、描述、输入 Schema）与 `call(String toolInput)` 执行体，包含 `MethodToolCallback`、`FunctionToolCallback` 以及 MCP 动态桥接的回调实现，通过 `.toolCallbacks(...)` 注册。
2. **核心调度器 `ToolCallingManager`**：
   - 负责根据模型返回的 `ToolCall` 请求查找对应的 `ToolCallback`，完成参数反序列化、本地执行、异常转换并将结果封装为 `ToolResponseMessage`。
3. **框架托管循环 vs 用户自主控制循环（1.x 与 2.x 的关键架构分水岭）**：
   - **Spring AI 2.0+（现行架构）**：彻底移除了 1.x 中散落在各厂商 `ChatOptions` 里的 `internalToolExecutionEnabled` 标志和模型内部工具循环。现在：
     - **使用 `ChatClient`（框架托管）**：`DefaultChatClient` 自动在 Advisor 链中注册递归拦截器 **`ToolCallingAdvisor`**（默认顺序 `Ordered.HIGHEST_PRECEDENCE + 300`），由其调用 `ToolCallingManager` 循环往复，直到模型输出无待执行工具的最终答复；并可无缝叠加 Memory/Session、Retry 与 Observability。
     - **直接使用 `ChatModel`（用户自主控制）**：底层 `ChatModel` 不再自动执行工具，仅原样返回模型发出的工具调用请求，开发者可显式编写 `while` 循环并调用 `ToolCallingManager.executeToolCalls(...)` 以完全掌控状态机、审批流或自定义重试。
   - **Spring AI 1.x 兼容说明**：1.0/1.1 曾默认在 `ChatModel` 内部执行工具（通过 `internalToolExecutionEnabled(false)` 关闭以切回手动循环），1.1 曾引入 `ToolCallAdvisor`（2.0.0 起弃用并由 `ToolCallingAdvisor` 替代）（来源：[Tools API Reference](https://docs.spring.io/spring-ai/reference/api/tools.html)、[Spring AI 2.0.0 GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-ga-available-now)）。

### 3.3 Advisors 拦截链（核心接口、执行顺序与递归机制）

Advisors 是 Spring AI 对标 Servlet Filter / Spring AOP 的横切关注点拦截管道，用于在请求发往模型前后以及流式响应返回过程中改写 Prompt、注入上下文、记录日志、执行护栏或驱动循环（来源：[Advisors API Reference](https://docs.spring.io/spring-ai/reference/api/advisors.html)）。

- **核心接口**：
  - `CallAdvisor`：同步拦截接口，核心方法 `adviseCall(ChatClientRequest, CallAdvisorChain)`。
  - `StreamAdvisor`：流式拦截接口，核心方法 `adviseStream(ChatClientRequest, StreamAdvisorChain)`。
  - `BaseAdvisor`：便捷接口，同时实现 `CallAdvisor` 与 `StreamAdvisor`，允许开发者以统一的 `before()` / `after()` 钩子编写自定义逻辑。
- **栈式执行顺序（`Ordered`）**：
  - 按 `getOrder()` 数值升序排列：**数值越小（越接近 `Ordered.HIGHEST_PRECEDENCE`），在请求下行阶段越早执行，在响应上行阶段越晚执行**（典型洋葱/栈模型）。
  - 工程实践上，通常将日志/鉴权/Session 记忆与 `ToolCallingAdvisor` 按明确优先级排布，确保上下文读取、工具循环与最终输出校验各司其职。
- **递归拦截（Recursive Advisors）**：
  - 借助 `CallAdvisorChain.copy(this)`，递归 Advisor 可以仅重放**当前节点之后**的下游拦截链与模型调用，而不重复触发上游的鉴权或初始输入记录。`ToolCallingAdvisor` 与 `StructuredOutputValidationAdvisor` 即是典型内置代表。
- **常用内置 Advisor**：
  - 工具循环：`ToolCallingAdvisor`（2.0+；替代弃用的 `ToolCallAdvisor`）
  - 会话/记忆：`SessionMemoryAdvisor` / `SessionAdvisors`（2.1+）；`MessageChatMemoryAdvisor`、`PromptChatMemoryAdvisor`（1.x–2.0，2.1 起弃用）；`VectorStoreChatMemoryAdvisor`（向量长期记忆，仍受支持）
  - RAG：`QuestionAnswerAdvisor`、`RetrievalAugmentationAdvisor`
  - 结构化校验与防护：`StructuredOutputValidationAdvisor`、`SafeGuardAdvisor`、`SimpleLoggerAdvisor`

### 3.4 Memory 记忆与 Session 上下文管理（重要 2.1 演进）

LLM 本身无状态，多轮对话与长程 Agent 必须显式管理上下文。Spring AI 在此领域经历了从 **扁平消息窗口（1.0–2.0）** 到 **事件日志与按轮次压缩的 Session API（2.1.0）** 的关键演进（来源：[Chat Memory Reference](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)、[Spring AI 2.1.0 GA](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-ga-and-2-0-3-ga-available-now)）。

#### A. Spring AI 1.0–2.0 经典架构（2.1.0 起已弃用，计划 3.0.0 移除）
- **策略与存储分离**：
  - `ChatMemory`：定义上下文裁剪策略，默认实现为 `MessageWindowChatMemory`（滑动保留最近 `maxMessages` 条消息）。
  - `ChatMemoryRepository`：负责按 `conversationId` 持久化消息，内置内存实现与 `JdbcChatMemoryRepository`（`spring-ai-starter-model-chat-memory-repository-jdbc`，按 `SPRING_AI_CHAT_MEMORY` 表与多数据库方言持久化）以及 Cassandra/Neo4j 等扩展仓储。
- **接入方式**：通过 `MessageChatMemoryAdvisor` 或 `PromptChatMemoryAdvisor` 挂载到 `ChatClient`，请求时以 `ChatMemory.CONVERSATION_ID` 参数区分会话。
- **长期语义记忆**：`VectorStoreChatMemoryAdvisor` 将历史交互向量化存入 `VectorStore` 并按语义检索召回，**在 2.1.0 中继续保留支持**。

#### B. Spring AI 2.1.0 现行架构：Session API（`spring-ai-session`）
针对 Agent 多步工具调用场景下扁平消息截断容易产生“孤立 `ToolResponseMessage` 破坏对话合法性”的痛点，2.1.0 引入了全新的 Session 架构：
- **核心 SPI**：`org.springframework.ai.session.SessionService` 与 `SessionRepository`（提供 `InMemorySessionRepository`、`JdbcSessionRepository` 支持 PostgreSQL / MySQL / MariaDB / H2）。
- **追加式事件日志（`SessionEvent`）**：将对话记录建模为结构化事件流而非简单消息列表。
- **Turn-Aware Compaction（轮次感知压缩）**：通过可插拔触发器与策略（如 `TurnCountTrigger`、`WindowCompactionStrategy`）严格在**完整轮次（Turn）边界**上执行裁剪或压缩，保证工具调用请求与响应成对保留，不破坏模型协议约束。
- **接入方式**：通过 `SessionMemoryAdvisor` / `SessionAdvisors` 挂载到 `ChatClient` 链条中。

### 3.5 RAG 与向量存储（ETL Pipeline、`VectorStore` 与模块化 RAG）

Spring AI 将 RAG 拆分为“离线数据接入（ETL）”与“在线检索增强（Advisor）”两大体系（来源：[RAG Reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)、[ETL Pipeline Reference](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html)）：

1. **ETL 数据管道**：基于函数式组合三阶段接口：
   - **Extract（`DocumentReader`）**：如 `TextReader`、`TikaDocumentReader`（支持 PDF/Word/Markdown 等）、PDF/JSON Reader，将原始文件解析为 `List<Document>`。
   - **Transform（`DocumentTransformer`）**：如 `TokenTextSplitter` 按 Token 分块，或元数据增强器抽取关键词/摘要。
   - **Load（`DocumentWriter` / `VectorStore`）**：调用 `EmbeddingModel` 计算向量并写入 `VectorStore`（支持 PGVector、Milvus、Qdrant、Redis、Elasticsearch、Chroma、Neo4j 等；2.1.0 起 11 种向量库还支持直接写入与检索用户预计算好的 **User-Supplied Embeddings**）。
2. **基础 RAG：`QuestionAnswerAdvisor`**
   - 开箱即用的 Naive RAG 实现：直接以用户输入检索 `VectorStore`（支持 `topK`、`similarityThreshold` 和类 SQL 的元数据 `filterExpression`），将命中文档拼接到 Prompt 中。适合快速原型验证。
3. **模块化 RAG：`RetrievalAugmentationAdvisor`**
   - 面向生产级复杂场景的可组合管道，将 RAG 清晰划分为四个可替换阶段：
     - **Pre-Retrieval（查询变换与扩展）**：`RewriteQueryTransformer`（重写模糊查询）、`CompressionQueryTransformer`（将多轮历史与追问压缩为独立查询）、`MultiQueryExpander`（生成多角度子查询提升召回）。
     - **Retrieval（文档检索）**：`VectorStoreDocumentRetriever`（配置 `vectorStore`、`similarityThreshold`、`topK`、`filterExpression`）或自定义检索器。
     - **Post-Retrieval（后处理）**：统一接口 `DocumentPostProcessor`（用于重排 Rerank、去重、过滤或压缩，替代早期的 `DocumentRanker`/`DocumentCompressor`/`DocumentSelector`）。
     - **Generation（上下文增强）**：`ContextualQueryAugmenter`（将检索文档与用户问题注入模板，可通过 `allowEmptyContext(boolean)` 控制无命中时是拒答还是回退常规回答）。

### 3.6 Structured Output（结构化输出转换机制）

Agent 工作流（尤其是 Routing、Orchestrator-Workers、Evaluator-Optimizer）高度依赖强类型数据交换。Spring AI 通过 `StructuredOutputConverter<T>`（同时扩展 Spring `Converter<String, T>` 与 `FormatProvider`）实现“前置格式指令注入 + 后置文本反序列化”（来源：[Structured Output Converter Reference](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)）：

- **三种内置转换器**：
  - `BeanOutputConverter<T>`：基于 JSON Schema 将输出映射为 Java `record` / POJO 或泛型 `ParameterizedTypeReference<T>`。
  - `ListOutputConverter`：将输出解析为 `List<String>`。
  - `MapOutputConverter`：将输出解析为动态键值 `Map<String, Object>`。
- **高层用法与可靠性兜底**：
  - 95% 场景直接通过 `chatClient.prompt()...call().entity(MyRecord.class)` 自动完成 Schema 生成、提示词注入与反序列化。
  - 针对 LLM 输出偶尔偏离 JSON Schema 的非确定性问题，可结合递归校验器 `StructuredOutputValidationAdvisor`（或 `.validateSchema()`）在解析失败时自动携带报错信息触发模型自我修正。

### 3.7 MCP（Model Context Protocol）官方集成

[Model Context Protocol (MCP)](https://modelcontextprotocol.io/) 是连接 AI 应用与外部数据源/工具的开放标准协议。官方 [MCP Java SDK (`modelcontextprotocol/java-sdk`)](https://github.com/modelcontextprotocol/java-sdk) 正是在 Spring AI 团队深度协作下构建，并与 Spring AI 无缝衔接（来源：[Spring AI MCP Overview](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)、[MCP Specification](https://modelcontextprotocol.io/)）。

1. **官方 Spring Boot Starters**：
   - **Client 端**：
     - `spring-ai-starter-mcp-client`（支持 STDIO、基于 Servlet 的 Streamable HTTP、Stateless Streamable HTTP 与 SSE）
     - `spring-ai-starter-mcp-client-webflux`（基于 WebFlux 的响应式 Streamable HTTP / SSE）
   - **Server 端**：
     - `spring-ai-starter-mcp-server`（基于 STDIO 的本地进程服务端）
     - `spring-ai-starter-mcp-server-webmvc`（基于 Spring WebMVC，支持 `STREAMABLE`、`STATELESS` 与 SSE）
     - 注：WebFlux 栈对应亦有响应式服务端 Starter，具体名称以对应版本 BOM 文档为准。
2. **传输协议演进**：
   - **STDIO**：子进程标准输入输出交换 JSON-RPC，适合本地 CLI / 脚本工具集成，不暴露网络端口。
   - **Streamable HTTP / Stateless（推荐远程方式）**：取代旧版双通道 HTTP+SSE，使用单一端点（如 `/mcp`）经 HTTP `POST` 发起请求并按需返回单个 JSON 或 SSE 流；Spring AI 2.1.0 已跟进支持 **MCP 2026-07-28 无状态规范修订**；生产部署需按规范校验 `Origin` 头并结合 OAuth2 Bearer Token 鉴权。
3. **声明式服务端注解与客户端工具发现**：
   - **Server 端**：开启 `spring.ai.mcp.server.annotation-scanner.enabled=true` 后，可在 `@Component` 方法上使用 `@McpTool`、`@McpToolParam` 直接将 Java 方法发布为标准 MCP Tool。
   - **Client 端**：自动装配 `SyncMcpToolCallbackProvider`（同步）或 `AsyncMcpToolCallbackProvider`（异步），将远端 MCP Server 暴露的工具自动转换为 Spring AI 原生 `ToolCallback`，直接传入 `ChatClient.defaultToolCallbacks(...)` 或 `.toolCallbacks(...)` 即可参与 `ToolCallingAdvisor` 的完整工具循环。

### 3.8 Observability & Evaluation（可观测性与评估）

1. **Observability（Micrometer & OpenTelemetry）**（来源：[Spring AI Observability Reference](https://docs.spring.io/spring-ai/reference/observability/index.html)）：
   - 原生构建于 Spring Boot Actuator + Micrometer Observation 之上，覆盖 `ChatClient`（含 Advisor 链路）、`ChatModel`、`EmbeddingModel`、`ImageModel` 与 `VectorStore`。
   - 对齐 OpenTelemetry GenAI 语义约定：低基数标签（如 `gen_ai.operation.name`、`gen_ai.system`、`spring.ai.kind`）同时进入指标与链路追踪；高基数信息（如会话 ID、Advisor 列表、工具名）仅记入分布式 Trace。
   - 出于隐私与体量考虑，原始 Prompt/Completion 文本默认不导出，需通过显式配置开关按需开启；接入 `micrometer-tracing-bridge-otel` 与 OTLP Exporter 即可对接 Jaeger / Tempo / Prometheus 等标准 APM 平台。
2. **Evaluation（LLM-as-a-Judge 评估测试）**（来源：[Spring AI Testing / Evaluation Reference](https://docs.spring.io/spring-ai/reference/api/testing.html)）：
   - 定义函数式接口 `Evaluator`（输入 `EvaluationRequest`，包含用户输入、检索上下文与模型回答；输出 `EvaluationResponse`，含 `isPass()` 判定）。
   - **`RelevancyEvaluator`**：评估生成答案与用户问题及检索文档的相关性（常用于 RAG 集成测试）。
   - **`FactCheckingEvaluator`**：检测幻觉，验证模型陈述是否受给定源文档逻辑支撑，支持搭配轻量专用模型（如通过 Ollama 调用 `FactCheckingEvaluator.forBespokeMinicheck(...)`）降低评测成本。

---

## 4. Java AI Agent 生态横向定位（基于一手文档对比）

在 Java/JVM 生态中，Spring AI 并非唯一选择，且与其他主流框架既存在竞争也存在显著互补关系（尤其多个框架直接构建于 Spring AI 之上）：

| 框架 / 项目 | 官方定位 | 核心编排与 Agent 模型 | Spring 生态集成度 | 典型适用场景 | 一手来源 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Spring AI** | Spring 官方企业级 AI 基础框架与 Agent 原语层 | `ChatClient` + 递归 `ToolCallingAdvisor` + 五种代码级 Workflow 模式；配合 `spring-ai-agent-utils` 扩展 Skills / Subagents / A2A | **原生（First-Party）**，与 Spring Boot 3.5/4.x、Actuator、Micrometer、Spring Security 深度同构 | 希望以标准 Spring 工程范式掌控底层细节、构建可控 Workflow 或自定义 Agent 循环的企业 Java 项目 | [spring.io/projects/spring-ai](https://spring.io/projects/spring-ai)<br>[spring-projects/spring-ai](https://github.com/spring-projects/spring-ai) |
| **LangChain4j** | 独立于特定 Web 框架的 Java LLM 开发套件 | 声明式接口代理 **`@AiService`** + `@Tool`；扩展模块 `langchain4j-agentic` 提供 `@Agent` 与工作流/多代理编排 | 提供官方 Spring Boot 3 / Boot 4 Starters，但不强绑定 Spring（同样适配 Quarkus / Micronaut / Plain Java） | 非 Spring 技术栈（如 Quarkus）或偏好声明式 Java 接口代理开发风格的团队 | [docs.langchain4j.dev](https://docs.langchain4j.dev/)<br>[langchain4j/langchain4j](https://github.com/langchain4j/langchain4j) |
| **LangGraph4j** | Java 版有状态图编排引擎（对标 Python LangGraph） | 有向循环状态图（Node / Edge / Conditional Routing / Checkpoint / Human-in-the-Loop） | 作为编排库可同时集成 **Spring AI** 或 **LangChain4j** | 需要显式状态机、可中断恢复（Checkpointing）与可视化图拓扑的复杂多角色工作流 | [langgraph4j/langgraph4j](https://github.com/langgraph4j/langgraph4j) |
| **Spring AI Alibaba** | 构建于 Spring AI 之上的企业级 Agentic 与图编排框架 | `spring-ai-alibaba-graph-core`（`StateGraph` / `OverAllState` / `CompiledGraph` / `MemorySaver`）+ 开箱即用的 **`ReactAgent`**（支持 `.asNode()` 嵌入图、Supervisor/Handoffs、人工中断恢复、导出 Mermaid/PlantUML） | **基于 Spring AI 扩展**，天然适配 Spring Boot 与阿里云/DashScope 等生态 | 需要现成 `StateGraph` + `ReactAgent` + 持久化断点续跑的复杂多智能体生产系统 | [alibaba/spring-ai-alibaba](https://github.com/alibaba/spring-ai-alibaba)<br>[java2ai.com](https://java2ai.com/) |
| **Google ADK for Java** | Google 开源代码优先（Code-First）Agent 开发与评估套件 | 显式区分推理型 **`LlmAgent`** 与确定性编排型 **`SequentialAgent`**、**`ParallelAgent`**、**`LoopAgent`**，内置开发调试 Web UI（`google-adk-dev`） | 独立 Java 库（`com.google.adk:google-adk`），可嵌入任意 Java/Spring 应用，但非 Spring 原生体系 | 强依赖 Gemini 生态、需要开箱即用的多 Agent 层次化原语与可视化调试台的项目 | [google.github.io/adk-docs](https://google.github.io/adk-docs/)<br>[google/adk-java](https://github.com/google/adk-java) |
| **Embabel** | 由 Spring 创始人 Rod Johnson 发起的 JVM 强类型规划型 Agent 框架 | 基于游戏 AI 的 **GOAP（Goal-Oriented Action Planning）** 确定性规划算法 + **OODA 重规划循环**，强类型领域对象驱动而非脆弱字符串拼接 | **直接构建在 Spring Core 与 Spring AI 之上**（Kotlin 编写，提供一等 Java 支持） | 复杂业务领域模型下，希望用确定性规划替代不可控 LLM 自由编排的高严谨度企业应用 | [embabel/embabel-agent](https://github.com/embabel/embabel-agent) |

**选型洞察**：在 Java 生态中，**Spring AI 正在成为事实上的底层模型与协议（Tool/MCP/Observability）底座**——不仅 Spring AI 自身具备完整的 Agent 原语，Spring AI Alibaba、LangGraph4j 与 Embabel 都直接支持或构建在 Spring AI 之上。因此，先扎实掌握 Spring AI 核心原语，再视复杂度按需叠加图编排（Spring AI Alibaba Graph / LangGraph4j）或规划器（Embabel）是最具复利价值的技术路线。

---

## 5. `JavaAgentStudy` 学习与实践路线参考

### 5.1 渐进式实验模块规划

结合 Spring AI 2.1.0 GA（Spring Boot 4.2.x + JDK 21）的最新体系——若需兼顾企业现有 Spring Boot 3.5.x 环境，也可先以 Spring AI 1.1.x 切入并标注差异——建议在 `JavaAgentStudy` 中按以下 6 个渐进阶段组织实验模块：

| 阶段 | 建议模块名 | 核心知识点与 API | 实践目标与验收标准 |
| :--- | :--- | :--- | :--- |
| **Phase 1** | `01-chatclient-basics` | `spring-ai-bom`、`ChatClient.Builder`、`call()` / `stream()`、Prompt 模板、`BeanOutputConverter` / `.entity()`、`StructuredOutputValidationAdvisor` | 跑通同步与 WebFlux/SSE 流式对话；实现带 Schema 校验的强类型 `record` 结构化输出 |
| **Phase 2** | `02-tool-calling-and-advisors` | `@Tool`、`ToolCallback`、`ToolCallingManager`、`ToolCallingAdvisor`（及直接 `ChatModel` 手动循环对比）、自定义 `BaseAdvisor`、`Ordered` 栈顺序、2.1 `requiresConfirmation` | 实现查询 + 写入双工具；通过日志验证 Advisor 上下行顺序；分别演示“框架托管循环”与“人工确认暂停/恢复” |
| **Phase 3** | `03-session-memory-and-rag` | 2.1 `SessionService` / `JdbcSessionRepository` / `TurnCountTrigger`（与 1.x `ChatMemory` 对比）；ETL Pipeline（`DocumentReader` → `TokenTextSplitter` → `VectorStore`）；`QuestionAnswerAdvisor` → `RetrievalAugmentationAdvisor`（含 `RewriteQueryTransformer`、`DocumentPostProcessor`、`ContextualQueryAugmenter`） | 持久化多轮会话并验证工具调用轮次压缩不破坏上下文；用 Docker Named Volume 启动 PGVector（符合本地开发环境规范）跑通模块化 RAG 与元数据过滤 |
| **Phase 4** | `04-mcp-integration` | `spring-ai-starter-mcp-server-webmvc`（`@McpTool` + Streamable HTTP/Stateless）、`spring-ai-starter-mcp-client`、`SyncMcpToolCallbackProvider`、STDIO 本地服务器接入 | 独立编写一个 Java MCP Server 暴露业务工具，再由 Agent 应用作为 MCP Client 动态发现并经 `ChatClient` 完成端到端调用 |
| **Phase 5** | `05-agentic-workflows` | 落地 Anthropic & Spring AI 五种 Workflow 模式（Chain、Parallelization、Routing、Orchestrator-Workers、Evaluator-Optimizer） | 针对真实任务（如技术文档审校或代码分析）编写可运行对比实验，结合 Java 21 Virtual Threads 实现并行与编排-执行者模式 |
| **Phase 6** | `06-autonomous-agent-eval-ops` | Micrometer + OpenTelemetry Trace/Metrics、`RelevancyEvaluator` & `FactCheckingEvaluator` 自动化评测；进阶选做 `spring-ai-agent-utils`（`SkillsTool` / `TaskTool` / A2A）或对比 Spring AI Alibaba `StateGraph` | 为 Agent 接入 OTel 链路追踪观察每轮工具耗时与 Token 消耗；编写 JUnit 评估测试防回归；完成单 Agent 向多 Agent / Skills 架构的延伸实验 |

### 5.2 一手官方文档与官方示例仓库索引

#### Spring AI 官方核心资料
- **项目主页**：[https://spring.io/projects/spring-ai](https://spring.io/projects/spring-ai)
- **官方参考文档总览**：[https://docs.spring.io/spring-ai/reference/](https://docs.spring.io/spring-ai/reference/)
  - 快速开始与 BOM：[Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)
  - 升级与破坏性变更指南：[Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
  - ChatClient API：[ChatClient API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
  - 工具调用：[Tools API](https://docs.spring.io/spring-ai/reference/api/tools.html)
  - 拦截器链：[Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html)
  - 对话记忆与弃用说明：[Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)
  - 检索增强生成：[Retrieval Augmented Generation (RAG)](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html) 与 [ETL Pipeline](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html)
  - 结构化输出：[Structured Output Converter](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
  - MCP 集成：[Model Context Protocol (MCP)](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)
  - 可观测性与测试评估：[Observability](https://docs.spring.io/spring-ai/reference/observability/index.html) 与 [Testing / Evaluators](https://docs.spring.io/spring-ai/reference/api/testing.html)
- **官方源码与发行说明**：
  - 核心仓库：[https://github.com/spring-projects/spring-ai](https://github.com/spring-projects/spring-ai)
  - Release Notes：[https://github.com/spring-projects/spring-ai/releases](https://github.com/spring-projects/spring-ai/releases)
  - 官方示例仓库（含 `agentic-patterns/` 与 MCP 示例）：[https://github.com/spring-projects/spring-ai-examples](https://github.com/spring-projects/spring-ai-examples)
  - 社区 Agent 扩展工具包（Skills / TaskTool / A2A）：[https://github.com/spring-ai-community/spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils)

#### 官方工程博客与原始论文/规范
- **Spring AI 版本发布公告**：
  - [Spring AI 2.1.0 GA and 2.0.3 GA Available Now (2026-11-20)](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-ga-and-2-0-3-ga-available-now)
  - [Spring AI 2.0.0 GA Available Now (2026-06-12)](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-ga-available-now)
- **Spring AI Agentic 系列官方博客**：
  - [Building Effective Agents with Spring AI (Part 1) (2025-01-21)](https://spring.io/blog/2025/01/21/building-effective-agents-with-spring-ai-part-1)
  - [Spring AI Agentic Patterns (Part 1): Agent Skills (2026-01-13)](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills)
  - [Spring AI Agentic Patterns (Part 4): Subagent Orchestration (2026-01-27)](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents)
- **Anthropic 原始工程文章**：[Building Effective Agents](https://www.anthropic.com/engineering/building-effective-agents)
- **MCP 官方规范与 Java SDK**：[https://modelcontextprotocol.io/](https://modelcontextprotocol.io/)、[https://github.com/modelcontextprotocol/java-sdk](https://github.com/modelcontextprotocol/java-sdk)

#### Java Agent 生态对照官方入口
- **LangChain4j**：[https://docs.langchain4j.dev/](https://docs.langchain4j.dev/)、[https://github.com/langchain4j/langchain4j](https://github.com/langchain4j/langchain4j)
- **LangGraph4j**：[https://github.com/langgraph4j/langgraph4j](https://github.com/langgraph4j/langgraph4j)
- **Spring AI Alibaba**：[https://java2ai.com/](https://java2ai.com/)、[https://github.com/alibaba/spring-ai-alibaba](https://github.com/alibaba/spring-ai-alibaba)
- **Google ADK for Java**：[https://google.github.io/adk-docs/](https://google.github.io/adk-docs/)、[https://github.com/google/adk-java](https://github.com/google/adk-java)
- **Embabel**：[https://github.com/embabel/embabel-agent](https://github.com/embabel/embabel-agent)
