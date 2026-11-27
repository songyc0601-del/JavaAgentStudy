## Agent skills

### Issue tracker

GitHub Issues on `songyc0601-del/JavaAgentStudy` (via the `gh` CLI). See `docs/agents/issue-tracker.md`.

### Triage labels

Default five canonical roles (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context (`GLOSSARY.md` and `docs/adr/` at the repo root). See `docs/agents/domain.md`.

### Build verification

Trust the output of `mvn -B -ntp test` over IDE diagnostics: the IDE lags behind Spring Boot 4.2 package names and reports stale errors. Run the root build before declaring work done; CI runs the same command.

### Spring AI 2.1 reference

Primary-source Spring AI 2.1.0 GA / Spring Boot 4.2.x APIs, BOM coordinates, deprecations, and agentic primitives—read `docs/research/java-agent-spring-ai.md` before writing or reviewing Spring AI code or dependencies.

### Branch logging

Every request handler and service method logs the branch it takes, so a developer debugging an endpoint can read from the log alone which path ran. Use SLF4J (`LoggerFactory.getLogger(<Class>.class)`) and log, in Simplified Chinese:

- **Entry**: endpoint or method name plus non-sensitive inputs (`topic` length, whether `focus` is present, sync vs stream) at `INFO`.
- **Each branch**: validation failure, optional-field present or absent, fallback, early return, and the success exit, one line per branch at `INFO`, naming the branch and the condition that selected it.
- **Failure**: model error, mid-stream error, and mapped exceptions at `WARN` or `ERROR` with the cause attached.
- **Content**: log lengths, counts, and chunk indexes; keep prompt text, model output, API keys, and tokens out of logs.

### Issue wrap-up

An issue is done only when every item below is accounted for. Report each one in the final summary as done, not needed (with the reason), or deferred.

1. `README.md` describes every new or changed user-facing capability (usage, configuration, behaviour on failure).
2. `GLOSSARY.md` and `docs/adr/` reflect any new term or decision; a term the glossary already defines stays unchanged.
3. A summary comment for the issue is drafted: what was built, verification (`mvn -B -ntp test` result), acceptance criteria that were not met, and follow-ups. Show the draft to the user and post it with `gh issue comment` only after approval; tick the acceptance-criteria checkboxes in the same step.
4. Code comments explain the why of each non-obvious step (fallbacks, boundary conditions, workarounds) in Simplified Chinese.
5. Once the summary comment is approved, do these three steps together: post the comment (and tick the checkboxes), close the issue with `gh issue close <number>`, and commit and push the code to the current branch. Closing matters because open issues block the downstream issues that depend on them. Before committing, check `git status` and the full diff, and run `mvn -B -ntp test`; never commit secrets.

Outside the approved wrap-up in item 5, leave `git commit` and `git push` to the user unless asked.
