#!/usr/bin/env node

import { findBlockedGitSubcommand, findLocalGitWriteSubcommand } from "./afk-git-guard-shared.mjs";

function response(permission, extra = {}) {
  process.stdout.write(`${JSON.stringify({ permission, ...extra })}\n`);
}

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
  process.exit(1);
}

const command = typeof input.command === "string" ? input.command : "";
const blocked = findBlockedGitSubcommand(command);
const localWrite = findLocalGitWriteSubcommand(command);
if (blocked) {
  response("deny", {
    user_message: `Git ${blocked} 属于 AFK 禁止 Agent 代执行的危险操作，请在本地终端自行执行。`,
    agent_message: `AFK git guard blocked git ${blocked}. Print the exact command for the user; do not retry through Shell.`,
  });
} else if (localWrite) {
  response("deny", {
    user_message: `Git ${localWrite} 属于 AFK 人工提交交接操作，请由 Agent 使用 afk-core-git-handoff Skill 输出完整命令后在本地终端自行执行。`,
    agent_message: `AFK git guard blocked git ${localWrite}. Use the afk-core-git-handoff Skill to print the exact handoff command for the user; do not retry through Shell.`,
  });
} else response("allow");
