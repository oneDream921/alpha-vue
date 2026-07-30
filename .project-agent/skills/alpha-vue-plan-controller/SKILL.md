---
name: alpha-vue-plan-controller
description: Orchestrate Alpha Vue's approved implementation plan from a dedicated strong-model planning thread. Use when checking plan progress, selecting the next task, preparing a copyable prompt for a fast execution model, preparing a strong-model acceptance or remediation prompt, or recording verified execution status in the plan and acceptance documents. This Skill never creates threads or implements the task itself.
---

# Alpha Vue Plan Controller

Use this Skill in one long-lived, strong-model planning thread. Preserve that thread as the
coordination context; the user manually copies generated prompts into separate execution or review
threads.

This Skill is upstream of `alpha-vue-task-delivery-gate`:

- this Skill selects and specifies one task;
- the generated execution prompt requires the delivery-gate Skill;
- the generated review prompt verifies the resulting branch and evidence;
- this Skill records confirmed status after the user returns the result.

## 1. Hard role boundary

The plan-controller thread may:

- read Git state, plan documents, acceptance documents, relevant source code, tests, rules, and
  formal project documentation;
- analyze the next task and produce a complete task packet;
- output copyable execution, review, or remediation prompts;
- update only the approved requirement, implementation-plan, acceptance, and execution-record
  documents when the user asks to record verified progress.

The plan-controller thread must not:

- create, fork, message, wait for, or manage threads or subagents;
- implement or repair production code, tests, dependencies, runtime configuration, or Flyway;
- start or stop services, run test suites, perform browser acceptance, or execute the generated
  prompt;
- create branches, commit, merge, rebase, push, reset, stash, or delete Git state;
- infer completion from a worker summary when Git or acceptance evidence is unavailable;
- advance more than one numbered task in one cycle.

When the user asks this thread to execute code, explain the role boundary and provide the execution
prompt instead.

## 2. Sources of truth

Resolve the repository root and inspect current state before every planning cycle:

```bash
git status --short --branch
git worktree list
git log --oneline --decorate -12
```

Read:

1. `AGENTS.md`.
2. `docs/requirements/main/alpha-framework-modernization/implementation-plan.md`.
3. `docs/requirements/main/alpha-framework-modernization/acceptance.md`.
4. The requirement, architecture, backend, frontend, or infrastructure document relevant to the
   selected task.
5. Applicable formal documents such as `docs/security.md`, `docs/api.md`,
   `docs/development.md`, `docs/operations.md`, and `docs/frontend-conventions.md`.
6. Relevant source and tests needed to make the prompt concrete.

Do not load the entire requirement directory when one task has a narrow document set. Current Git
and code override stale status text; approved plan boundaries override worker improvisation.

Never read, print, or shell-source `deploy/.env`.

## 3. Task status model

Use only these states:

| State | Meaning |
| --- | --- |
| `PLANNED` | Task exists in the approved plan but has no approved task packet |
| `APPROVED` | User approved the concrete task packet |
| `IN_PROGRESS` | A task branch exists and execution is underway |
| `IMPLEMENTED` | Worker reports implementation complete; acceptance is not complete |
| `ACCEPTED` | Automated, strong-model, and explicit user acceptance are complete |
| `MERGED` | The accepted task is verified in `main` |
| `BLOCKED` | A recorded stop condition prevents progress |
| `N/A` | A conditional task was explicitly determined not applicable |

Status evidence:

- `IN_PROGRESS`: branch/worktree evidence or explicit user report.
- `IMPLEMENTED`: actual diff plus worker evidence; never from a prompt being issued.
- `ACCEPTED`: required automated evidence, strong review, and explicit user acceptance.
- `MERGED`: read-only ancestry check on current `main`.
- `BLOCKED` or `N/A`: reason, date, and decision owner must be recorded.

Do not rewrite historical Spike conclusions when updating current execution status.

## 4. Planning cycle

### Step 1: Reconcile state

Compare Git, the implementation plan, acceptance results, and the user's latest report. Summarize:

- current `main` and remote divergence;
- active task branches or worktrees;
- last accepted and merged task;
- current task and status;
- unresolved blockers;
- the single next authorized action.

If evidence conflicts, stop at reconciliation and do not generate an execution prompt.

### Step 2: Select one task

Choose the earliest task whose prerequisites are satisfied. Do not skip a task because a later task
looks easier. Conditional second- or third-phase capabilities require their documented trigger and
user confirmation.

Inspect actual code before specifying files or commands. Use the approved plan as scope, not as proof
that the current implementation still matches its original baseline.

### Step 3: Form the task packet

The task packet must contain:

- task number and title;
- verified base branch and commit;
- proposed short-lived branch;
- objective and observable completion behavior;
- prerequisites and current implementation evidence;
- in-scope files or ownership areas;
- explicit non-goals and later tasks that remain forbidden;
- ordered implementation steps;
- focused and complete validation commands;
- runtime and manual acceptance;
- strong-review focus;
- stop conditions and rollback boundary;
- Git authorization boundary;
- required final report.

Resolve all placeholders before output. If an important decision is missing, present that decision
to the user instead of handing ambiguity to the fast model.

### Step 4: Produce one prompt

Generate only the prompt requested for the current transition:

- `EXECUTION`: approved task packet -> fast-model implementation thread.
- `REVIEW`: implemented branch -> independent strong-model acceptance thread.
- `REMEDIATION`: accepted review findings -> fast-model repair thread.
- `STATUS`: verified user/Git outcome -> plan-document status update.

Do not emit all prompts preemptively.

## 5. Execution prompt contract

An `EXECUTION` prompt must:

1. Name the repository, task, base commit, and branch.
2. Require `alpha-vue-task-delivery-gate` plus applicable backend, frontend, API, architecture, or
   local-start Skills.
3. Tell the worker to recheck Git state instead of trusting the prompt blindly.
4. Include the complete scope, non-goals, steps, tests, runtime checks, stop conditions, and report.
5. State the intended model role: fast execution model following an approved strong-model plan.
6. Forbid architecture redesign and later tasks.
7. Preserve unrelated changes and sensitive environment boundaries.
8. State exact Git permissions. Never imply commit, merge, push, or branch deletion authorization.
9. End by requiring the worker to stop and return evidence to the user.

The prompt must be directly copyable. Do not add commentary inside the code block that the worker
could mistake for optional guidance.

## 6. Review and remediation prompt contracts

A `REVIEW` prompt must:

- target an exact branch, base, and implementation commit or working-tree state;
- use the approved task packet and acceptance checklist;
- inspect the actual diff and code rather than trusting the worker summary;
- report P0/P1/P2 findings with file and line evidence;
- verify scope, security, data, dependency, runtime, documentation, and residual checks relevant to
  the task;
- distinguish automated evidence, user-performed browser acceptance, and unverified claims;
- avoid modifying files unless the user explicitly authorizes the review thread to fix findings;
- conclude whether the task meets commit or merge conditions.

A `REMEDIATION` prompt must include only accepted findings for the current task. It must forbid
unrelated cleanup, require regression validation, and stop before commit or merge unless separately
authorized.

## 7. Updating plan execution status

Update planning documents only after the user asks to record progress or supplies an outcome that
must be persisted.

Prefer a compact `**执行状态**` block under the corresponding task in
`implementation-plan.md`, containing only:

```text
状态：
基础提交：
任务分支：
实现提交：
验收日期：
合并提交：
验证摘要：
剩余风险：
下一动作：
```

Rules:

- omit fields that do not yet have evidence;
- record exact commit IDs and dates, not “latest” or “already done”;
- update `acceptance.md` only for checks supported by command evidence or explicit user acceptance;
- never mark later tasks started;
- do not duplicate full test logs or worker reports in the plan;
- keep formal current-state docs synchronized only after behavior is implemented and accepted;
- leave all documentation edits uncommitted and report them unless the user separately authorizes
  Git writes.

## 8. Response format

Use this compact order:

1. `状态核对`
2. `计划判断`
3. `可复制提示词` or `计划状态更新`
4. `用户下一动作`

State explicitly:

- which model class should receive the prompt: fast execution or strong review;
- that no thread was created;
- that this thread did not execute the task;
- whether any plan documents were edited and whether they remain uncommitted.

End after the single next action. Do not automatically prepare the following task.
