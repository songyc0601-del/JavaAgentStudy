# Coding Standards

Judgement-call standards enforced during code review. Mechanical rules (Jackson 3 imports, banned Spring AI 2.x deprecated APIs, import hygiene, package cycle freedom) are enforced automatically by `config/checkstyle/checkstyle.xml` and ArchUnit tests on every `mvn -B -ntp test`—do not duplicate them here.

## Branch logging

Every HTTP handler and service method logs its execution path with SLF4J (`LoggerFactory.getLogger(<Class>.class)`) in Simplified Chinese so a developer can trace which branch ran from logs alone:

- **Entry** (`INFO`): endpoint or method name plus non-sensitive inputs (lengths, presence flags, sync vs stream).
- **Each branch** (`INFO`): validation failure, optional-field present or absent, fallback, early return, null conversion, and success exit—one line per branch naming the branch and selecting condition.
- **Failure** (`WARN` / `ERROR`): model error, mid-stream error, and mapped exceptions with the cause attached.
  - **Sanitization exception**: when an exception message may embed raw model output or prompt text (such as Jackson deserialization errors during structured output parsing), log only the exception/cause class name and non-sensitive metrics without attaching the raw exception object. Keeping model output out of logs takes precedence.
- **Content**: log lengths, counts, elapsed time, and chunk indexes; keep prompt text, model output, API keys, and tokens out of logs.
- **Library-layer logs** (e.g., `common` loaders): write warning/error messages in Simplified Chinese to match the rest of the repo.

## Comments

Explain the *why* of each non-obvious step (fallbacks, boundary conditions, workarounds, sanitization choices) in Simplified Chinese. Skip restating what the code obviously does.

## Package and domain boundaries

- Place cross-feature Web infrastructure (`ApiError`, `ApiExceptionHandler`, shared exceptions, CORS, log helpers) in `stageXX.web`; feature packages depend on `web` one-way, never vice versa.
- Validate invariants inside `common` domain `record` compact constructors (non-blank strings, non-null domain fields, non-empty immutable lists) rather than relying solely on JSON Schema or callers.
- Use `PromptTemplate` (parameterized placeholders) for non-trivial User Prompts instead of ad-hoc string concatenation.

## Testing seam and reuse

- Follow `docs/agents/testing-seam.md`: drive offline integration tests through the HTTP layer and replace only the outermost `ChatModel` bean.
- Reuse the module's shared `@TestConfiguration` + `SwitchableChatModel` for MockMvc tests so tests share one Spring context; create a separate context only when testing startup/auto-config or `RANDOM_PORT` clients.

## Documentation and scope changes

- **Single source of truth**: root `README.md` summarizes every user-facing capability (usage, configuration, failure behaviour) and links to each stage's `README.md` for walkthroughs and full command examples—avoid duplicating long commands across both files where they can drift.
- **Scope changes**: when an implementation intentionally defers a spec requirement or adds out-of-spec convenience (such as debug UI or Swagger), document the decision in the ticket summary comment and parent Spec.
- **Static debug pages**: files matched by `.gitattributes` (`static/*.html`) may be spot-checked during review rather than line-by-line audited unless HTML/JS behaviour itself is under review.
