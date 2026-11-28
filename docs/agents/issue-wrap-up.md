# Issue Wrap-Up

An issue is done only when every item below is accounted for. Report each item in the final summary as **done**, **not needed** (with reason), or **deferred**.

1. **User-facing docs**: `README.md` (and stage `README.md` if applicable) reflects every new or changed capability (usage, configuration, failure behaviour).
2. **Domain docs**: `GLOSSARY.md` and `docs/adr/` reflect any new term or architectural decision; existing terms stay unchanged.
3. **Summary comment draft**: draft a comment covering what was built, `mvn -B -ntp test` verification results, unmet or deferred acceptance criteria, and follow-ups. Show the draft to the user first—post with `gh issue comment` only after approval, and tick acceptance-criteria checkboxes in the same step.
4. **Approved wrap-up execution**: once approved, run all three together:
   - Post the comment (and tick checkboxes).
   - Close the issue with `gh issue close <number>` (open issues block downstream tickets).
   - Verify `git status`, full diff, and `mvn -B -ntp test` (no secrets), then commit and push to the current branch.
5. **Post-push verification**:
   - Check remote CI status with `gh run list --limit 3` when practical so GitHub Actions (Linux / Temurin 21) is observed green.
   - If any `@Tag("live-llm")` test changed, remind the user to run `mvn -B -ntp -Plive-llm test` (or trigger `.github/workflows/live-llm.yml`) when ready.

Outside this approved wrap-up, leave `git commit` and `git push` to the user unless explicitly requested.
