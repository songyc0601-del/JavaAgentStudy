# stage-01：ChatClient 与结构化输出学习导读

本阶段面向初级 Java `Learner`，用三个 HTTP 接口演示 Spring AI 2.1 最核心的调用方式：同步讲解、SSE 流式讲解、结构化 `Exercise` 生成。

## 1. 你会学到什么

| 概念 | 在本阶段哪里能看到 | 一句话理解 |
| :--- | :--- | :--- |
| `ChatClient` | `ConceptExplanationService`、`ExerciseService` | 调用大模型的统一入口，`prompt().user(...).call()` 同步取结果，`.stream()` 流式取结果 |
| System / User Prompt | `SYSTEM_PROMPT` 常量与 `PromptTemplate` 参数化模板 | System Prompt 规定模型的角色和风格，User Prompt 通过占位符渲染本次具体问题 |
| SSE 流式输出 | `POST /api/explanations/stream` | 模型每生成一段文本就推送一段，用 `Flux<String>` 表示 |
| Structured Output | `POST /api/exercises` | 用 `BeanOutputConverter` 把模型输出的 JSON 绑定成强类型 `Exercise`，并用 `StructuredOutputValidationAdvisor` 校验、重试 |

### 调用链路

```
HTTP 请求 → Controller（@Valid 入参校验）→ Service（组装提示词）→ ChatClient → ChatModel → 大模型
```

入参校验失败时直接返回 400，Service 和模型都不会被调用，所以不会产生费用。

## 2. 启动步骤（云端 OpenAI-compatible）

```bash
# 在仓库根目录执行
cp .env.example .env          # 填入真实的 OPENAI_API_KEY（.env 已被 Git 忽略，切勿提交）
set -a && source .env && set +a
mvn -pl stage-01-chat-and-structured-output -am spring-boot:run
```

可配置的环境变量见 `.env.example`：`OPENAI_API_KEY`、`OPENAI_BASE_URL`、`OPENAI_MODEL`。任何兼容 OpenAI 协议的云端端点，只要改 `OPENAI_BASE_URL` 与 `OPENAI_MODEL` 即可。

> 本阶段暂不提供本地 `ollama` 方式，后续按需再引入。

启动后可访问：

- `http://localhost:8080/`：概念讲解验证页面（若通过 IntelliJ 预览端口如 `localhost:63342` 打开，页面会自动回退指向 `http://localhost:8080` 并支持在「后端地址」框修改；服务端 `LocalCorsConfig` 仅放行本机回环端口对 `/api/**` 的跨源调用）
- `http://localhost:8080/swagger-ui.html`：Swagger UI
- `http://localhost:8080/v3/api-docs`：OpenAPI 3 JSON

## 3. curl 调试示例

以下命令都在服务已启动、端口为 `8080` 时执行。

### 3.1 同步概念讲解

```bash
curl -X POST http://localhost:8080/api/explanations \
  -H 'Content-Type: application/json' \
  -d '{"topic":"集合框架","focus":"ArrayList 与 LinkedList 的区别"}'
```

预期：HTTP 200，JSON 含 `topic` 与 `explanation`。`focus` 可省略。

### 3.2 SSE 流式概念讲解

```bash
curl -N -X POST http://localhost:8080/api/explanations/stream \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"topic":"集合框架","focus":"ArrayList 与 LinkedList 的区别"}'
```

预期：终端逐段输出 `data:` 行。`-N` 用于关闭 curl 缓冲，否则看不到逐段效果。

### 3.3 结构化 Exercise 生成

```bash
curl -X POST http://localhost:8080/api/exercises \
  -H 'Content-Type: application/json' \
  -d '{"topic":"集合框架","difficulty":"BEGINNER","focus":"ArrayList 与 LinkedList 的区别"}'
```

预期：HTTP 200，JSON 含 `title`、`topic.name`、`difficulty`、`description`、`hints`、`referenceAnswerPoints`。`difficulty` 可选 `BEGINNER` / `INTERMEDIATE` / `ADVANCED`，缺省为 `BEGINNER`。

### 3.4 非法入参（400）

```bash
# topic 为空白：不会调用模型，返回 400 与 JSON 错误体
curl -i -X POST http://localhost:8080/api/explanations \
  -H 'Content-Type: application/json' \
  -d '{"topic":"  "}'

# difficulty 取值非法：同样返回 400
curl -i -X POST http://localhost:8080/api/exercises \
  -H 'Content-Type: application/json' \
  -d '{"topic":"集合框架","difficulty":"EXPERT"}'
```

预期：`HTTP/1.1 400`，响应体含 `error` 字段（`VALIDATION_FAILED` 或 `MALFORMED_REQUEST`）。

## 4. 测试接缝设计思路

本阶段所有测试都在同一个接缝上：**HTTP 入口**。Controller、Service、`ChatClient` 与结构化输出转换都是真实代码，只有最外层的 `ChatModel` 不同：

| 测试类型 | `ChatModel` | 是否联网 / 花钱 | 何时运行 |
| :--- | :--- | :--- | :--- |
| 离线测试 | `common` 提供的 `FakeChatModel`（预设回复或分片） | 否 | 默认 `mvn test` |
| `live-llm` 端到端测试 | 真实的 OpenAI-compatible 端点 | 是 | 显式指定时才运行 |

好处：离线测试稳定、免费，适合 CI；`live-llm` 测试用同一组接口验证真实模型确实能跑通。

### 运行 live-llm 测试

默认 `mvn test` 会通过父 `pom.xml` 的 `surefire.excludedGroups=live-llm` 跳过它们，无需 API Key，也不产生网络请求。需要时先配置好环境变量，再激活根 `pom.xml` 的 `live-llm` Profile：

```bash
set -a && source .env && set +a
mvn -B -ntp -Plive-llm test
```

注意：

- 会真实调用模型并消耗 Token，四个用例（同步、SSE、`Exercise`、400）共耗时约 1 分钟。
- 断言只检查结构与非空，不比较模型原文，因为真实模型每次输出都不同。
- `live-llm` Profile 会自动排除 `common` 中用于守护默认构建的必败测试 `LiveLlmExclusionGuardTest`，无需手动额外拼 `-Dtest`。
