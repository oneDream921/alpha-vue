#!/usr/bin/env node

import { findBlockedGitSubcommand, findLocalGitWriteSubcommand } from "./afk-git-guard-shared.mjs";

let input;
try {
  input = JSON.parse(await new Promise((resolve, reject) => {
    let content = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => { content += chunk; });
    process.stdin.on("end", () => resolve(content));
    process.stdin.on("error", reject);
  }));
} catch (error) {
  process.stderr.write(`AFK git guard could not parse hook input: ${error.message}\n`);
  process.exit(2);
}

const command = typeof input.tool_input?.command === "string" ? input.tool_input.command : input.command;
const blocked = typeof command === "string" && findBlockedGitSubcommand(command);
const localWrite = typeof command === "string" && findLocalGitWriteSubcommand(command);
if (blocked) {
  const reason = `Git ${blocked} 属于 AFK 禁止 Agent 代执行的危险操作，请在本地终端自行执行。`;
  process.stdout.write(`${JSON.stringify({ hookSpecificOutput: { hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: reason } })}\n`);
} else if (localWrite) {
  const reason = `Git ${localWrite} 属于 AFK 人工提交交接操作，请输出完整命令供用户在本地终端执行。`;
  process.stdout.write(`${JSON.stringify({ hookSpecificOutput: { hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: reason } })}\n`);
} else process.stdout.write("{}\n");
