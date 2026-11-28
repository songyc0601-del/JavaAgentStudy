# Agent Routing Guide

## Build verification

Trust `mvn -B -ntp test` over IDE diagnostics (the IDE lags behind Spring Boot 4.2 package renames and reports stale errors). Root `mvn -B -ntp test` also enforces Checkstyle (`config/checkstyle/checkstyle.xml`) and ArchUnit guards; CI runs the same command plus `-Plive-llm -pl common test`.

## Context pointers

- **Spring AI 2.1 / Boot 4.2 APIs**: before writing or reviewing Spring AI code or dependencies, read `docs/agents/spring-ai-2-1-cheatsheet.md` (full research in `docs/research/java-agent-spring-ai.md`).
- **Testing seam & live-llm**: before adding a stage module or Spring Boot tests, read `docs/agents/testing-seam.md`.
- **Coding standards**: when reviewing code or checking branch logging, comments, package boundaries, and doc conventions, read `CODING_STANDARDS.md`.
- **Domain vocabulary & ADRs**: before naming domain concepts or changing architecture, read `docs/agents/domain.md`, `GLOSSARY.md`, and relevant files in `docs/adr/`.
- **Issue tracker**: when reading, creating, linking, or closing GitHub Issues on `songyc0601-del/JavaAgentStudy`, read `docs/agents/issue-tracker.md`.
- **Triage labels**: when applying or interpreting issue triage labels, read `docs/agents/triage-labels.md`.
- **Issue wrap-up**: before closing an issue or committing wrap-up changes, follow `docs/agents/issue-wrap-up.md`.
