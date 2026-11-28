# Domain Docs

This repo uses a single-context domain layout at the repo root:

- `GLOSSARY.md`: canonical domain vocabulary and `_Avoid_` synonyms.
- `docs/adr/`: architecture decision records.

## Rules

- **Use the glossary's vocabulary**: name domain concepts (in code, tests, issue titles, docs) with the terms defined in `GLOSSARY.md`; avoid synonyms listed under `_Avoid_`.
- **Flag gaps**: if a concept is missing from `GLOSSARY.md`, either reconsider the name or note it for `/domain-modeling`.
- **Flag ADR conflicts**: if a proposal contradicts an existing ADR, surface it explicitly (`Contradicts ADR-XXXX (...), worth reopening because...`) rather than overriding silently.
