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
