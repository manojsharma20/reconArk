## What and why

<!-- One concern per PR. Link the issue or decision record. -->

## Checklist
- [ ] New behaviour is a plugin, service or UI module, not an `if` in existing code
- [ ] `common` and `engine-core` stay plain Java; plugins don't depend on each other
- [ ] Tests added (unit; Testcontainers for SQL); `./gradlew build` and frontend checks pass
- [ ] Docs and diagrams in `docs/` updated; decision record for new services, plugin kinds or technology
- [ ] Simple enough for a developer with 2–3 years of experience to follow

## Security
- [ ] No secrets committed; no customer data in logs or error messages; SQL uses bind parameters
