# Testing Seam & Live-LLM Contract

Read before adding a new stage module or writing Spring Boot integration tests.

## Offline Integration Seam

- **Single seam**: drive tests from the **HTTP boundary** (`MockMvc` or `WebClient`) against the real Spring Boot context (Controller, validation, Service, `PromptTemplate`, `ChatClient`, Advisors, `ApiExceptionHandler`), replacing **only** the outermost `ChatModel` bean. Never mock business Services or `ChatClient`.
- **Shared fakes** (`common` module, `dev.javaagentstudy.common.testing`):
  - `FakeChatModel`: `replyingWith(...)`, `replyingInOrder(...)`, `failingWith(...)` (applies to both `call` and `stream`), `streamingChunks(...)`, `streamingChunksThenFailing(...)`, plus `recordedPrompts()`.
  - `SwitchableChatModel`: thread-safe delegating `ChatModel` wrapper. Register **one** shared `@TestConfiguration` bean (`@Bean @Primary SwitchableChatModel`) per stage module (see `stage-01` `Stage01TestConfig`) and switch delegates in `@BeforeEach` so MockMvc test classes reuse a single Spring context.
- **Quiet test logs**: in each stage's `src/test/resources/application.properties`, set `spring.main.banner-mode=off` and `logging.level.org.springframework=WARN` / `org.springdoc=WARN` while keeping `dev.javaagentstudy.<stage>=DEBUG` so branch-logging tests (`OutputCaptureExtension`) continue working without console spam.

## Hidden Cross-File Coupling: Placeholder API Key

Because `@Primary` adds the fake `ChatModel` alongside auto-configured OpenAI beans, OpenAI auto-configuration still requires a non-empty `spring.ai.openai.api-key` at context startup:

1. `src/main/resources/application.yml` reads `${OPENAI_API_KEY:}`.
2. `src/test/resources/application.properties` sets `spring.ai.openai.api-key=test-placeholder-key` so offline tests start without env vars.
3. Any `@Tag("live-llm")` test **must** override this back to the real env var via `@SpringBootTest(properties = "spring.ai.openai.api-key=${OPENAI_API_KEY:}")`—otherwise it sends the placeholder key to the real endpoint.

## Live-LLM Tag & Guard Contract

- Default `mvn -B -ntp test` excludes `@Tag("live-llm")` via `surefire.excludedGroups=live-llm` in root `pom.xml`.
- `common/.../LiveLlmExclusionGuardTest` is tagged `@Tag("live-llm")` and unconditionally fails to catch broken exclusion config.
- Explicit real-model runs use `mvn -B -ntp -Plive-llm test`: the root `live-llm` Maven Profile clears `excludedGroups`, selects `groups=live-llm`, and excludes `**/LiveLlmExclusionGuardTest.java`.
- CI verifies both default offline tests (`mvn -B -ntp test`) and guard exclusion (`mvn -B -ntp -Plive-llm -pl common test`).
