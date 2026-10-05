---
description: Scaffold a React feature module (UI brick) and register it in the shell and UI manifest
argument-hint: <module-id> "<Title>"
---

Create a frontend module: $ARGUMENTS. Follow LLD §12 and §15 "Add a UI module".

1. Create `frontend/modules/<id>/package.json`, named `@reconark/module-<id>`, with `main: src/index.tsx` and
   dependencies on `@reconark/module-sdk`.
2. Create `src/index.tsx`, which exports `defineModule({ id, title, routes })`, plus one page component per route.
   - Use `api`/`postJson`, `Async` and `SchemaForm` from the SDK.
   - Never import another module.
   - Never store tokens.
3. Add `"@reconark/module-<id>": "0.2.0"` to `frontend/apps/shell/package.json` and one line to
   `apps/shell/src/registry.ts`.
4. Add a manifest entry (id, title, path, roles, order) to `reconark.bff.modules` in
   `services/web-bff/src/main/resources/application.yaml`, and to `apps/shell/public/ui-manifest.dev.json`.
5. Add Vitest tests for any non-trivial logic.
6. Run `cd frontend && npm install && npm run typecheck && npm test && npm run build`.
