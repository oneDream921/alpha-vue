---
name: alpha-vue-task-delivery-orchestrator
description: Orchestrate one approved Alpha Vue delivery task through
  strong-model planning, fast-model implementation, batched validation,
  persistent strong-model review, bounded remediation, user testing, status
  synchronization, and Git handoff. Use for confirmed framework modernization,
  cross-layer delivery, infrastructure replacement, security/session changes,
  migrations, or other scoped tasks that benefit from automated auxiliary agents
  and explicit delivery gates.
---

# Alpha Vue Task Delivery Orchestrator

Run one user-approved task continuously without starting later numbered tasks. The current thread is
the coordinator and fast executor. It may delegate planning, review, and status synchronization, but
it remains responsible for scope, evidence, and user gates.

## 1. Entry and capability check

Identify one entry mode:

- `NEW`: approved task, no implementation started.
- `RESUME`: continue the current task with existing worktree changes.
- `REMEDIATION`: fix only accepted findings for the current task.
- `CLOSEOUT`: implementation is already merged or accepted; only reconcile plan, acceptance, and
  index status from verified evidence.

Read `AGENTS.md`, the named plan and acceptance sections, applicable formal docs, relevant code and
tests, and only the project Skills required by scope. Inspect branch, HEAD, worktrees, worktree
changes, and running processes. Preserve unrelated changes and never read, print, commit, or
shell-source `deploy/.env`.

Before delegating, check whether the environment can:

1. create an internal auxiliary agent;
2. send it follow-up context and wait for results;
3. reuse the same review agent;
4. route planning and review to a strong-capability model.

Invocation of this Skill authorizes internal planning, review, and status agents for the named task.
It does not authorize creating user-visible tasks, Git writes, destructive data operations, or later
tasks. If a required capability is unavailable, use `alpha-vue-generate-task-prompts` to produce the
prompt for that phase and stop. Never present self-review as independent review or a standard model
as a strong model.

If an auxiliary agent fails for transient platform reasons such as rate limiting, record the failure
as missing independent evidence. Continue only when the remaining work is low-risk documentation or
status reconciliation backed by local command evidence; for implementation, security/session changes,
data semantics, or required strong review, stop or generate a handoff prompt instead.

## 2. Lightweight state machine

Use these states in order:

```text
INIT -> PLANNING -> PLAN_READY -> IMPLEMENTING -> IMPLEMENTATION_COMPLETE
-> FOCUSED_VALIDATION -> TASK_VALIDATION -> REVIEWING
-> [REMEDIATING -> RE_REVIEWING] (maximum two rounds)
-> USER_TEST_PENDING -> STATUS_SYNC -> COMMIT_PENDING -> MERGE_HANDOFF
```

Stop on `BLOCKED` or `USER_DECISION`. Do not infer completion from an agent summary when Git, command,
or user evidence is missing.

Before any file write after a user reply, long-running command, validation batch, or context
transition, re-run a read-only branch, HEAD, and worktree check. If the branch has changed or is now
protected for the intended write, stop and ask the user to migrate again.

## 3. Strong planning agent

For `NEW`, create one read-only strong planning agent. For `RESUME`, create it only when no approved
task execution plan exists or the current implementation materially diverges from that plan.
For `CLOSEOUT`, skip the planning agent unless the evidence conflicts or the closeout would change
scope; build a brief local closeout plan from the named plan and acceptance sections.

Give the planner the named task, current Git evidence, approved design boundaries, relevant code,
tests, and documentation. Require exactly one conclusion:

- `READY`: detailed plan is directly implementable and introduces no unresolved decision.
- `BLOCKED`: list the minimum user or architecture decisions required.

A `READY` plan must contain:

- objective and observable completion behavior;
- verified base, current branch, and current implementation evidence;
- precise scope, file ownership areas, and explicit non-goals;
- ordered implementation steps without placeholders;
- API, data, migration, configuration, security, compatibility, and rollback impact where relevant;
- tests to write during implementation;
- one batched focused-validation checkpoint and the task-level validation matrix;
- runtime and user acceptance scenarios;
- selected project Skills with reasons;
- stop conditions, residual risks, and Git boundary.

The planner must inspect actual code and remain read-only. If `READY`, the coordinator writes the
plan to:

```text
docs/requirements/main/alpha-framework-modernization/execution-plans/<task-id>.md
```

An execution plan is committed with its implementation, not as a mandatory pre-implementation
commit. Proceed automatically after `READY`; ask the user only for unresolved architecture, scope,
destructive data, or security decisions.

## 4. Continuous implementation

Implement the complete approved task in coherent batches. Write tests alongside production code,
but do not run tests after every file, class, method, or small function. Maintain a pending validation
list and finish the feature before the normal validation checkpoint.

Early validation is allowed only when it determines whether implementation can continue:

- dependency or Spring Boot compatibility;
- a blocking compile-time contract;
- Flyway or persistent-data semantics;
- an irreversible operation;
- an explicit planner stop condition.

Keep Controllers, Services, Mappers/XML, entities, DTOs/VOs, frontend services/views, configuration,
and tests within existing ownership boundaries. Do not begin later tasks or opportunistic cleanup.

## 5. Batched validation

After implementation is complete:

1. run the named task's focused tests together;
2. collect failures and fix them as one batch;
3. rerun failed and directly affected tests;
4. run task-level checks once before strong review.

Select task-level checks by scope:

- backend only: backend `test` and `package`;
- frontend only: relevant tests, typecheck, lint, format check, and build;
- cross-layer: applicable backend and frontend checks;
- deployment/configuration: applicable Compose validation and runtime smoke;
- milestone/stage closeout: complete backend, frontend, Compose, HTTP, and required user acceptance.

Dependency replacement also requires dependency-tree, packaged-runtime, configuration, class, and
effective-residual checks. Historical requirement text is not a runtime residual.

When recording validation counts, prefer raw reports such as Surefire XML, Vitest JSON, or build
summaries saved by the tool over truncated terminal excerpts. If counts differ between sources, cite
the source used instead of silently choosing one.

## 6. Startup and HTTP acceptance

Do not reconstruct startup, environment loading, port cleanup, Maven, pnpm, or Compose commands.
Read `alpha-vue-local-start` and use project scripts:

```bash
# Project is stopped
scripts/start-all.sh

# Restart is required
scripts/stop-all.sh
scripts/start-all.sh
```

Use partial scripts only when the user explicitly requests one service. Wait for script health and
availability checks. Diagnose ports or processes only after a script fails, using its output and
`${TMPDIR:-/tmp}/alpha-vue-local-start/` logs.

Default runtime acceptance uses unit/integration tests and batched `curl` scenarios. Verify only
task-relevant status codes, response contracts, authorization, state changes, error safety, and
traceId. Keep credentials and complete Token values out of output, and clean temporary data through
the application API.

Do not run browser automation, Playwright, screenshots, visual comparison, or automated viewport
checks unless the user explicitly asks. Put UI and responsive behavior in the user test checklist and
record browser automation as not run, not as passed.

## 7. Persistent strong review

After validation, create one independent read-only strong review agent. Retain its identifier for the
entire task. Give it the execution plan, complete diff, command evidence, relevant docs, and task
acceptance criteria. Do not leak an expected conclusion.

The reviewer reports file/line evidence with P0/P1/P2 severity and exactly one disposition:

- `PASS`: no blocking issue.
- `FIX_REQUIRED`: blocking findings that can be fixed inside the approved task.
- `USER_DECISION`: a proposed fix changes architecture, scope, data risk, or an approved contract.

Non-blocking advice is reported and not implemented opportunistically. On `FIX_REQUIRED`, fix all
accepted blocking findings as one batch, run affected tests together, and run task-level checks again
only when the fix changes shared, security, runtime, or cross-layer behavior.

Send the original review agent the original findings, remediation diff, adjacent impact, and new test
evidence. It reviews those findings and the necessary regression surface, not the entire repository
from scratch. Allow at most two remediation/re-review rounds. If blocking findings remain, stop and
report rather than loop indefinitely.

## 8. User testing and status synchronization

After review `PASS`, report automated evidence and provide a task-specific user checklist. Every item
contains prerequisites, exact actions, expected result, safe failure evidence such as HTTP status or
traceId, temporary-data impact, and cleanup. Never ask the user to reveal secrets or complete Token
values.

Wait for the exact phrase `测试通过`. Do not mark manual acceptance before it. After that phrase, create
one standard-capability status agent. It may update only the current task execution block in
`implementation-plan.md` and evidence-supported checks in `acceptance.md`. It must not modify
production code, start the next task, infer unsupported checks, or commit documentation.

For `CLOSEOUT`, update only evidence-supported plan, acceptance, and index status. Keep remaining
stage gates explicit, especially formal documentation, user acceptance, and stage-release
confirmation. Do not convert a task closeout into a stage pass or the start of the next numbered task.

## 9. Git gates and final handoff

Follow repository Git rules exactly:

- only the exact phrase `确认提交` authorizes selective staging and commit;
- never use `git add -A` when unrelated changes exist;
- never execute push, pull, merge, rebase, reset, checkout, stash, branch deletion, or other forbidden
  operations; provide user commands when needed;
- after the user merges, perform read-only branch, worktree, ancestry, and divergence verification;
- do not start the next task automatically.

At each stop, report current state, agent results, changed files by concern, tests and runtime evidence,
unverified items, review disposition and rounds, user acceptance, documentation status, residual risk,
and the single next authorized action.
