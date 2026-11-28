# Spring AI 2.1.0 GA / Spring Boot 4.2.x Cheatsheet

Quick reference before writing or reviewing Spring AI code or dependencies. For full background, ecosystem comparisons, and primary-source links, read `docs/research/java-agent-spring-ai.md`.

## Version & Dependency Baseline

- **Baseline**: JDK 21, Maven 3.10, Spring Boot `4.2.0` (Spring Framework `7.1.x`), Spring AI `2.1.0` GA, Jackson 3 (`tools.jackson.databind.*`; note `jackson-annotations` remains under `com.fasterxml.jackson.annotation.*`).
- **BOM**: `org.springframework.ai:spring-ai-bom:2.1.0` (imported in root `pom.xml`).
- **Common Starters**:
  - OpenAI-compatible model: `spring-ai-starter-model-openai`
  - WebMVC MCP Server: `spring-ai-starter-mcp-server-webmvc`
  - MCP Client: `spring-ai-starter-mcp-client`
  - VectorStore / PGVector: `spring-ai-starter-vector-store-pgvector`

## Removed / Deprecated APIs (Do Not Use)

Mechanical bans are also enforced by `config/checkstyle/checkstyle.xml`.

| Area | Removed / Deprecated | Spring AI 2.1 Replacement |
| :--- | :--- | :--- |
| JSON (`ObjectMapper`, `JsonNode`) | `com.fasterxml.jackson.databind.*` | `tools.jackson.databind.*` (`JsonNode.asString()`) |
| Tool definition | `FunctionCallback` | `@Tool` / `@ToolParam`, `ToolCallback`, `MethodToolCallback`, `FunctionToolCallback` |
| Tool loop | `internalToolExecutionEnabled`, `ToolCallAdvisor` | `ToolCallingAdvisor` + `ToolCallingManager` (or manual `while` loop on `ChatModel`) |
| Advisor interfaces | `CallAroundAdvisor`, `AdvisedRequest` | `CallAdvisor`, `StreamAdvisor`, `BaseAdvisor`, `ChatClientRequest`, `ChatClientResponse` |
| Memory / Session | `ChatMemory`, `MessageChatMemoryAdvisor`, `PromptChatMemoryAdvisor` | `SessionService`, `SessionRepository`, `SessionMemoryAdvisor` / `SessionAdvisors` (`VectorStoreChatMemoryAdvisor` remains supported) |
| RAG post-processing | `DocumentCompressor` / `Ranker` / `Selector` | `DocumentPostProcessor` |
| MCP remote transport | Dual-channel HTTP + SSE | Single-endpoint **Streamable HTTP** (`streamable` / `stateless`) |

## Core Primitives Quick Map

- **Chat**: inject `ChatClient.Builder` → `defaultSystem(...)` → `.prompt().user(...).call().content()` (sync) or `.stream().content()` (SSE `Flux<String>`). Render parameterized prompts via `PromptTemplate`.
- **Structured Output**: `BeanOutputConverter<T>` + `StructuredOutputValidationAdvisor.builder().outputType(T.class).maxRepeatAttempts(n).build()`; separate `.call().content()` from `converter.convert(...)` when HTTP error codes must distinguish model call failures from format failures.
- **Tools & Confirmation (2.1)**: `@Tool(description = "...", requiresConfirmation = true)` + `ToolCallingAdvisor` + turn-based tool call limits.
- **Advisors**: `Ordered` stack order—smaller `getOrder()` runs earlier on request (`before`) and later on response (`after`). Recursive advisors use `CallAdvisorChain.copy(this)`.
- **Observability & Eval**: Micrometer / OpenTelemetry GenAI conventions; `RelevancyEvaluator`, `FactCheckingEvaluator`.
