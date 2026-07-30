---
name: alpha-vue-generate-task-prompts
description: Generate one copyable Alpha Vue task prompt from a user-specified task and the project's approved plans, rules, code, and current Git evidence. Use when the user wants an execution, review, or remediation prompt for another manually created thread. This Skill only writes the prompt; it never selects the next task, updates plan status, creates threads, or executes the task.
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

If the user does not name a type, infer it only when their wording is unambiguous. Otherwise ask
which type they need.

## Build the prompt

Before writing, verify only the context needed for the named task. Prefer current Git and code
evidence over stale prose, while keeping the approved plan as the scope boundary.

Every prompt must be directly copyable and include:

1. repository path, task identifier and intended model role;
2. verified branch/base context, or an instruction for the worker to verify it when unavailable;
3. objective and observable completion behavior;
4. in-scope areas and explicit non-goals;
5. applicable project Skills, including `alpha-vue-task-delivery-gate` for execution and remediation;
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

### REVIEW

Target an exact branch, commit or working-tree state when available. Require inspection of the actual
diff and report findings first as P0/P1/P2 with file and line evidence. Distinguish verified command
evidence, explicit user acceptance and unverified claims. A review prompt does not authorize fixes.

### REMEDIATION

Include only findings explicitly accepted by the user. Forbid unrelated cleanup and later tasks,
require focused regression checks plus the task's required complete checks, and stop before commit or
merge unless separately authorized.

## Output

Return:

1. `提示词类型` and intended model class;
2. one fenced code block containing only the copyable prompt;
3. one sentence stating that no thread was created and the task was not executed.

Do not update documentation or generate prompts for later tasks.
