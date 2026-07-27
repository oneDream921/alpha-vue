#!/usr/bin/env node

import { readFile } from "node:fs/promises";
import { spawn } from "node:child_process";
import { join, normalize, relative, resolve } from "node:path";

function readInput() {
  return new Promise((resolveInput, reject) => {
    let content = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => { content += chunk; });
    process.stdin.on("end", () => { try { resolveInput(JSON.parse(content)); } catch (error) { reject(error); } });
    process.stdin.on("error", reject);
  });
}

function collectPaths(value, paths = new Set()) {
  if (Array.isArray(value)) value.forEach((item) => collectPaths(item, paths));
  else if (value && typeof value === "object") {
    for (const [key, item] of Object.entries(value)) {
      if (["file_path", "path", "file"].includes(key) && typeof item === "string") paths.add(item);
      else collectPaths(item, paths);
    }
  }
  return paths;
}

function patchPaths(patch) {
  const paths = new Set();
  if (typeof patch !== "string") return paths;
  for (const match of patch.matchAll(/^\*\*\* (?:Add|Update) File: (.+)$/gmu)) paths.add(match[1]);
  return paths;
}

function safeRelativePaths(root, input) {
  const paths = new Set([
    ...collectPaths(input.tool_input),
    ...patchPaths(input.tool_input?.patch),
    ...patchPaths(input.tool_input?.command),
  ]);
  return [...paths].map((path) => normalize(path).replaceAll("\\", "/")).filter((path) => {
    const absolute = resolve(root, path);
    const relativePath = relative(root, absolute);
    return relativePath !== "" && relativePath !== ".." && !relativePath.startsWith(`..${process.platform === "win32" ? "\\" : "/"}`);
  });
}

function matches(path, patterns) {
  return patterns.some((pattern) => path.endsWith(pattern.slice(4)));
}

function run(command, cwd, timeout) {
  return new Promise((resolveRun) => {
    const child = spawn(command[0], command.slice(1), { cwd, stdio: ["ignore", "ignore", "pipe"] });
    let stderr = "";
    const timer = setTimeout(() => child.kill(), timeout * 1000);
    child.stderr.on("data", (chunk) => { stderr += chunk; });
    child.on("error", (error) => { clearTimeout(timer); resolveRun({ code: 1, stderr: error.message }); });
    child.on("close", (code) => { clearTimeout(timer); resolveRun({ code: code ?? 1, stderr }); });
  });
}

try {
  const input = await readInput();
  const root = typeof input.cwd === "string" ? resolve(input.cwd) : process.cwd();
  const config = JSON.parse(await readFile(join(root, ".codex", "hooks", "afk-checks.json"), "utf8"));
  const paths = safeRelativePaths(root, input);
  for (const check of config.checks ?? []) {
    const files = paths.filter((path) => matches(path, check.patterns));
    if (files.length === 0) continue;
    const command = check.command.flatMap((part) => part === "{files}" ? files : [part]);
    const result = await run(command, root, check.timeout);
    if (result.stderr) process.stderr.write(result.stderr);
    if (result.code !== 0) {
      const reason = "AFK changed-file code check failed.";
      process.stdout.write(`${JSON.stringify({ decision: "block", reason, hookSpecificOutput: { hookEventName: "PostToolUse", additionalContext: reason } })}\n`);
      process.exit(2);
    }
  }
  process.stdout.write("{}\n");
} catch (error) {
  process.stderr.write(`AFK changed-file code check could not run: ${error.message}\n`);
  const reason = "AFK changed-file code check could not run.";
  process.stdout.write(`${JSON.stringify({ decision: "block", reason, hookSpecificOutput: { hookEventName: "PostToolUse", additionalContext: reason } })}\n`);
  process.exit(2);
}
