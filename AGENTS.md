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

### Issue wrap-up

An issue is done only when every item below is accounted for. Report each one in the final summary as done, not needed (with the reason), or deferred.

1. `README.md` describes every new or changed user-facing capability (usage, configuration, behaviour on failure).
2. `GLOSSARY.md` and `docs/adr/` reflect any new term or decision; a term the glossary already defines stays unchanged.
3. A summary comment for the issue is drafted: what was built, verification (`mvn -B -ntp test` result), acceptance criteria that were not met, and follow-ups. Show the draft to the user and post it with `gh issue comment` only after approval; tick the acceptance-criteria checkboxes in the same step.
4. Code comments explain the why of each non-obvious step (fallbacks, boundary conditions, workarounds) in Simplified Chinese.

Leave `git commit` and `git push` to the user unless asked.
