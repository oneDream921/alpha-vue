---
name: alpha-vue-task-delivery-gate
description: Govern delivery of a confirmed Alpha Vue task from approved design and clean main through scoped implementation, automated and runtime verification, strong-model review, user-authorized commit, and merge handoff. Use for Alpha Vue framework modernization, infrastructure replacement, security-sensitive backend changes, or any task that must stop at explicit Git authorization boundaries.
---

# Alpha Vue Task Delivery Gate

Use this skill for a clearly scoped, user-approved Alpha Vue task. It is a delivery gate, not a requirements-discovery workflow: if the task boundary or authorization is unclear, stop and use requirements exploration or ask the user.

## 1. Establish the gate

Before editing:

1. Read `AGENTS.md`, the task's approved requirement/design/acceptance documents, and the applicable `docs/security.md`, `docs/operations.md`, `docs/development.md`, and frontend conventions.
2. Read the applicable Alpha Vue backend/frontend/local-start and risk-review Skills.
3. Inspect `git status --short --branch`, `git worktree list`, `git log`, and relevant running processes. Preserve unrelated user changes.
4. Never read, print, commit, or shell-source `deploy/.env`.
5. Record the task boundary, explicit stop conditions, required validation, and Git permissions before implementation.

For a modernization task whose approved documents are not yet on `main`, commit those documents separately first. Then create the task branch from the latest clean `main`, using `codex/<task>-<topic>` unless the user specifies another name.

## 2. Keep the scope closed

Only change files and behavior required by the current task. Do not begin later numbered tasks, dependency spikes, unrelated upgrades, or opportunistic refactors. Pay special attention to requirements that explicitly forbid changes to adjacent systems such as Redis, Redisson, client/session identifiers, SpringDoc, storage providers, or frontend component systems.

Before implementation, make a change inventory covering dependencies, configuration, Java/backend code, APIs, filters, permissions, menus, environment variables, frontend services/views, tests, and formal documentation. Use it to prove both removal and preservation.

If a change affects a public contract, use the API-design Skill and validate both backend and frontend consumers. If a change affects security, sessions, transactions, migrations, or runtime dependencies, require focused tests and a strong-model review.

## 3. Implement and verify in layers

Use the smallest coherent implementation. Add regression tests for changed behavior and failure paths. Keep production configuration typed and externalized; preserve existing transaction, Flyway, MyBatis-Plus, mapper, and authorization boundaries unless the task explicitly changes them.

Run focused checks first, then the complete project checks required by the repository:

```bash
# Use the repository-specified Maven binary when one is provided.
<maven> -f alpha-server/pom.xml test
<maven> -f alpha-server/pom.xml package
pnpm --dir alpha-web typecheck
pnpm --dir alpha-web test
pnpm --dir alpha-web lint
pnpm --dir alpha-web format:check
pnpm --dir alpha-web build
git diff --check
```

For dependency replacement tasks, inspect the complete dependency tree and packaged runtime libraries. Confirm the intended implementation is BOM-managed where required, there is no duplicate implementation, and removed dependencies/configuration/classes/endpoints/menu entries/env names have no effective residuals. Historical approved requirement records may retain baseline terminology; distinguish those from runtime residuals.

For frontend changes, validate desktop, tablet, and mobile layouts. If the user has already manually checked the browser and explicitly says not to automate browser verification, record that result and do not invoke browser automation; still perform available HTTP/runtime checks.

## 4. Runtime acceptance

Use the project scripts rather than reconstructing process or environment handling:

```bash
scripts/stop-all.sh
scripts/start-all.sh
```

When dependencies were stopped and the task needs them, use the matching dependency script. Do not manually source environment files.

For security or infrastructure changes, validate real HTTP behavior, not only MockMvc:

- health and liveness/readiness behavior;
- anonymous and authenticated access to protected management endpoints;
- login, permissions, and at least one real database CRUD path;
- Flyway startup and representative query/pagination behavior;
- transaction commit/rollback evidence where the task affects data access;
- changed operational UI/API behavior and removed legacy entry points;
- metrics or diagnostics using the intended controlled access path.

Do not print tokens, credentials, response bodies containing sensitive data, or `.env` contents. Use temporary test records only with explicit filters and clean them up through the application API.

## 5. Strong-model review and remediation loop

Run an independent strong-model review after the implementation and initial validation. Give it the current diff, approved acceptance criteria, relevant code/docs, and ask for P0/P1/P2 findings without leaking the expected conclusion.

Review at minimum:

- dependency/configuration/endpoint/menu/document residuals;
- authentication and filter ordering, bypasses, token lifecycle, and health-probe boundaries;
- parameter units/defaults and production semantics;
- Flyway, MyBatis-Plus, SQL, transaction, pagination, and data behavior;
- public error leakage and frontend contract regressions;
- AFK source-rule/projection consistency;
- scope leakage into later tasks.

If a P0 or P1 is found, fix only the current task, keep the fix uncommitted until authorized, rerun relevant and complete validation, repeat runtime checks, and obtain a final independent review. P2 findings must be reported with impact and whether they block delivery.

## 6. Git authorization gates

Treat these as separate states:

1. Implementation complete: code may remain uncommitted.
2. User says `确认提交`: stage only the reviewed task files, run `git diff --cached --check`, and create a focused commit with a Chinese subject/body following repository conventions.
3. User says `确认合并`: do not infer push or branch deletion. In environments where the agent is prohibited from merge/push, provide exact local commands instead of executing them.
4. After the user merges locally, perform read-only verification of current branch, clean worktree, ancestry, merge commit, and remote divergence.

Never use `git add -A` when unrelated changes may exist. Never reset, checkout away, stash, rebase, merge, push, or delete branches unless the active environment explicitly permits that operation and the user has authorized it.

## 7. Final report

Report only evidence from commands or explicit user confirmation. Include:

- branch, HEAD, worktree state, and document/implementation commits;
- changed files grouped by concern;
- removed and preserved capabilities;
- exact validation commands and results, including test counts;
- runtime HTTP, database, metrics, and browser results;
- P0/P1/P2 findings and fixes;
- unverified items and residual risks;
- whether the task meets merge conditions;
- whether main was merged, pushed, or later tasks started.

End with the next authorized action and a concise TRACE line. If code remains uncommitted, say so explicitly and wait for the required authorization.
