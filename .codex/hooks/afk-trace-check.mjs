#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const hookRoot = dirname(fileURLToPath(import.meta.url));

function playCompletionNotification() {
  const isWindows = process.platform === "win32";
  const command = isWindows ? "powershell.exe" : "sh";
  const args = isWindows
    ? ["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", join(hookRoot, "afk-notify.ps1")]
    : [join(hookRoot, "afk-notify.sh")];

  spawnSync(command, args, { stdio: "ignore", timeout: 4_000 });
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
  process.stderr.write(`AFK trace check could not parse hook input: ${error.message}\n`);
  process.exit(2);
}

const message = typeof input.last_assistant_message === "string" ? input.last_assistant_message.trimEnd() : "";
const lastLine = message.split(/\r?\n/u).at(-1) ?? "";
const hasTrace = /^\[TRACE\] 规则：[^；\r\n]+；记忆：[^；\r\n]+；技能：[^；\r\n]+；钩子：[^；\r\n]+$/u.test(lastLine);

if (!hasTrace && input.stop_hook_active !== true) {
  process.stdout.write(`${JSON.stringify({
    decision: "block",
    reason: "AFK final-response contract: repeat the complete user-facing final response and end it with exactly one [TRACE] line.",
  })}\n`);
} else {
  playCompletionNotification();
  process.stdout.write("{}\n");
}
