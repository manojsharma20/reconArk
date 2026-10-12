---
description: Create a new frontend module (screen) and switch it on
argument-hint: <module-id, e.g. audit> "<Menu title>"
---

Create a UI module. Arguments: $ARGUMENTS.

1. `code/frontend/src/modules/<id>/`: a page component (`<Name>Page.tsx`) using `api` from `shared/api.ts`,
   `Card`/`ErrorBox`/`StatusBadge` from `shared/components.tsx` and types from `shared/types.ts`; and `index.ts`
   exporting `export const <id>Module: UiModule = { id, title, home: '/<id>', routes: [...] }`.
2. Register it in `allModules` in `code/frontend/src/shell/modules.ts`.
3. Switch it on: add the id to `modules` in `code/frontend/public/config.json`.
4. If it calls a new `/api` path, add the proxy in `vite.config.ts` and `code/deploy/docker/nginx.conf`.
5. Tests for any non-trivial logic (pure functions next to the page, like `definitionForm.ts`).
6. Run `cd code/frontend && npm run format && npm run typecheck && npm test && npm run build`.
7. Add the screen to `docs/01-business-overview.md` §1.5 and `docs/03-detailed-design.md` §3.11.
