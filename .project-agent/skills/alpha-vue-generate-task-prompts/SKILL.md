---
name: alpha-vue-generate-task-prompts
description: Generate one copyable Alpha Vue execution, review, remediation, or status-sync prompt from a user-specified task and current project evidence. Use when another manually created thread needs a grounded task prompt, applicable project Skills must be selected, or user testing and status-document handoff must be specified. This Skill only writes the prompt; it never selects the next task, updates plan status, creates threads, or executes the task.
---

# Alpha Vue Generate Task Prompts

Generate a precise prompt that the user can copy into a separate task. Do nothing beyond producing
that prompt.

## Boundaries

The user must identify the task or the exact plan item. Do not choose the next task, maintain a task
queue, reconcile project progress, or decide that a later task should start.

You may use read-only inspection to ground the prompt:

- read `AGENTS.md`, the user-named plan or acceptance section, applicable formal documentation,
  relevant code and tests;
- inspect the current branch, commit and working-tree state;
- resolve file paths, commands, constraints and acceptance criteria that belong in the prompt.

You must not:

- create, fork, message, wait for, or manage tasks, threads or subagents;
- modify project files, plan status, acceptance records or source code;
- implement, review, test or start the requested task;
- create branches or perform commit, merge, rebase, push, reset, stash or deletion;
- infer or select a task when the user's target is ambiguous;
- read, print or shell-source `deploy/.env`.

If essential task information is missing, list the minimum missing information and stop. Do not fill
an architectural or scope decision with an assumption.

## Prompt types

Generate exactly one type requested by the user:

- `EXECUTION`: implement an approved task with a fast execution model.
- `REVIEW`: independently inspect and accept an implemented task with a strong model.
- `REMEDIATION`: fix only user-accepted findings from the current task.
- `STATUS_SYNC`: after verified automated evidence and the user's exact `测试通过` confirmation,
  update only the named task's plan and acceptance status.

If the user does not name a type, infer it only when their wording is unambiguous. Otherwise ask
which type they need.

## Build the prompt

Before writing, verify only the context needed for the named task. Prefer current Git and code
evidence over stale prose, while keeping the approved plan as the scope boundary.

## Select applicable Skills

Decide which project Skills the receiving thread actually needs. Do not include every Skill by
default. State the decision and its reason before the copyable prompt.

Use `alpha-vue-task-delivery-gate` when the receiving task is a confirmed implementation or
remediation with a formal delivery lifecycle, especially framework modernization, dependency or
infrastructure replacement, authentication/session/security changes, migrations, runtime
configuration, cross-layer behavior, manual acceptance, or Git handoff.

Do not use the delivery gate for prompt generation itself, requirements discussion, read-only
explanation, ordinary standalone review, or a `STATUS_SYNC` task that only records already verified
evidence. For a formal acceptance review of an approved delivery task, include it only when the
review must evaluate the delivery gates rather than merely review code.

Select other Skills by actual scope:

- `alpha-vue-backend`: Java or Spring Boot implementation/review.
- `alpha-vue-frontend`: Vue implementation/review or responsive browser acceptance.
- `alpha-vue-api-design`: a public API contract is added or changed.
- `alpha-vue-architecture`: a system boundary or approved architecture decision is being evaluated;
  do not use it for routine implementation of an already decided design.
- `alpha-vue-local-start`: local startup, runtime smoke, dependency containers, or browser acceptance.
- `java-code-review`: risk-focused Java review, not routine implementation.

If no specialized Skill is needed, say so instead of inventing one.

Every prompt must be directly copyable and include:

1. repository path, task identifier and intended model role;
2. verified branch/base context, or an instruction for the worker to verify it when unavailable;
3. objective and observable completion behavior;
4. in-scope areas and explicit non-goals;
5. only the applicable project Skills and why each is required;
6. ordered work or review steps;
7. focused and complete validation or review requirements;
8. security, sensitive-data and unrelated-change boundaries;
9. exact Git authorization boundary;
10. required final evidence and stop condition.

Do not include unresolved placeholders. Do not silently expand the approved scope.

### EXECUTION

Require the worker to inspect actual state before editing, preserve unrelated changes, implement only
the named task, run the required validations, and return evidence without committing unless the user
has explicitly authorized a commit.

For behavior that a user can observe or operate, require the worker to finish automated checks first
and then provide a user test checklist. Each item must contain prerequisites, exact actions, expected
result, safe failure evidence such as HTTP status or traceId, temporary-data impact, and cleanup.
Cover only behavior changed by the task. Never ask the user to expose credentials, Token values,
verification codes, private data, or `deploy/.env`.

The worker must distinguish automated checks from user checks and wait. Tell the user to reply with
the exact phrase `测试通过` only after all required manual checks pass. On that reply, the worker must
stop implementation, preserve evidence, and hand off to a separate `STATUS_SYNC` task; it must not
silently mark its own work accepted.

### REVIEW

Target an exact branch, commit or working-tree state when available. Require inspection of the actual
diff and report findings first as P0/P1/P2 with file and line evidence. Distinguish verified command
evidence, explicit user acceptance and unverified claims. A review prompt does not authorize fixes.

### REMEDIATION

Include only findings explicitly accepted by the user. Forbid unrelated cleanup and later tasks,
require focused regression checks plus the task's required complete checks, and stop before commit or
merge unless separately authorized.

If remediation changes user-observable behavior, regenerate only the affected manual checks and use
the same `测试通过` handoff.

### STATUS_SYNC

Generate this prompt only when the user has explicitly said `测试通过` and implementation evidence is
available. Require the receiving agent to verify Git commits and supplied evidence, then update only
the named task's execution block in `implementation-plan.md` and supported checks in `acceptance.md`.
It must not infer unverified results, change production code, start the next task, or use
`alpha-vue-task-delivery-gate` merely for a documentation-only status update. Documentation changes
remain uncommitted unless the user separately gives the repository's exact commit authorization.

## Output

Return:

1. `提示词类型` and intended model class;
2. `Skill 判断`, listing selected and deliberately omitted Skills with brief reasons;
3. one fenced code block containing only the copyable prompt;
4. one sentence stating that no thread was created and the task was not executed.

Do not update documentation or generate prompts for later tasks.
