---
name: afk-project-alpha-vue-frontend
description: Apply Alpha Vue's repository-specific frontend architecture,
  coding, UI, permission, responsive, testing, and delivery conventions. Use
  when planning, implementing, reviewing, debugging, or refactoring files under
  alpha-web, especially Vue components and pages, TypeScript services, Pinia
  stores, Vue Router routes and guards, permission controls, Ant Design Vue UI,
  UnoCSS/CSS styles, Vitest tests, or frontend directory structure.
---

# Alpha Vue Frontend

Follow the project's frontend conventions while preserving existing behavior, contracts, and local patterns.

## Load Project Rules

1. Resolve the repository root with `git rev-parse --show-toplevel` when needed.
2. Read `docs/frontend-conventions.md` completely before making frontend decisions or edits. Treat it as the canonical detailed specification.
3. Read `docs/conventions.md` when the task touches cross-layer contracts, security, permissions, API behavior, or delivery checks.
4. Read `docs/ai/services/alpha-web/overview.md` when executing or revising frontend structure-governance work; it records the approved durable boundaries and evolution rules.
5. Inspect `alpha-web/package.json`, nearby implementation files, tests, and `git status --short` before proposing or applying changes.

If a referenced rule conflicts with the user's explicit current request, surface the conflict and follow the user's decision. Do not silently weaken security, permissions, type safety, or accessibility.

## Classify The Task

- For review, explanation, diagnosis, specification, or planning requests, inspect and report with evidence; do not edit source unless explicitly asked.
- For implementation or refactoring requests, make the smallest coherent change, preserve unrelated work, and verify in proportion to risk.
- For visual or interaction changes, preserve complete mobile functionality and perform browser checks at desktop, tablet, and mobile widths.
- For structure changes, apply the approved plan in independent stages and stop at its review points.

## Preserve Core Architecture

- Keep Vue 3 Composition API, strict TypeScript, Ant Design Vue, Pinia, Vue Router, Axios, Vite, and UnoCSS.
- Do not introduce Element Plus or a second overlapping UI library.
- Keep direct Axios usage inside `src/service`; keep UI messages and loading state outside service modules.
- Keep authentication state atomic and route components constrained by the frontend whitelist plus backend menu and permission data.
- Treat backend authorization as the security boundary; use routes and `v-permission` only for frontend experience.
- Keep page-local state local. Move state to Pinia only when it is genuinely shared across pages or sessions.
- Extract components and composables only for clear responsibilities or demonstrated reuse. Do not create empty symmetry directories or generic CRUD abstractions preemptively.

## Execute Changes

1. Identify the affected domain, current imports, API contracts, permission codes, tests, and responsive behavior.
2. Define the ownership boundary before extracting code. Keep route pages responsible for workflow orchestration.
3. Preserve public import paths during migrations through typed barrel exports when required.
4. Use explicit types for props, emits, template refs, API models, forms, and unknown errors. Do not use `any` as an escape hatch.
5. Use Ant Design Vue for business controls and `@ant-design/icons-vue` for icons. Use project tokens and UnoCSS/CSS for layout and responsive composition. Keep the UnoCSS reset disabled and only add shortcuts for stable cross-page layout patterns.
6. Keep loading, success, empty, failure, confirmation, and cleanup behavior complete. Avoid duplicate request-layer and page-layer error messages.
7. Add or update focused tests for changed contracts and risky state transitions. Assert observable behavior and error paths; mock external boundaries rather than internal behavior, and do not add production APIs only for tests.
8. Leave unrelated refactors, dependency upgrades, metadata churn, and backend changes outside the task.

## Route By Change Type

- **Services**: organize contracts by domain, test method/path/parameters/body, and preserve stable barrel imports during migrations.
- **Views and components**: keep route pages responsible for orchestration; place page-only code locally and promote it only after real cross-page reuse.
- **State, permissions, and routes**: preserve atomic session changes, permission-code consistency, the component whitelist, and dynamic-route cleanup.
- **UI and styles**: keep admin screens compact, all actions reachable below 768px, tables scrollable, icon controls accessible, and styles based on project tokens.

## Verify

Run focused checks first. Before delivering an approved stage, run:

```bash
pnpm --dir alpha-web typecheck
pnpm --dir alpha-web test
pnpm --dir alpha-web lint
pnpm --dir alpha-web format:check
pnpm --dir alpha-web build
git diff --check
```

For rendered UI changes, also verify 1440px, 768px, and 375px widths. Check loading, empty, error, permission, table scrolling, dialog or drawer layout, action reachability, and absence of overlap.

Report changed files, behavior preserved or changed, checks run, failures or skipped checks, and any remaining risk. Never claim a check passed unless its command completed successfully.
