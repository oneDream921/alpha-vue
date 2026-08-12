#!/usr/bin/env node

import { readFile } from "node:fs/promises";
import { spawn } from "node:child_process";
import { dirname, isAbsolute, join, normalize, parse, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const hookWorkspaceRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../..");

async function findWorkspaceRoot(start) {
  let current = resolve(start);
  while (true) {
    try {
      await readFile(join(current, ".codex", "hooks", "afk-checks.json"), "utf8");
      return current;
    } catch (error) {
      if (error.code !== "ENOENT") throw error;
    }
    const parent = dirname(current);
    if (parent === current || current === parse(current).root) return null;
    current = parent;
  }
}

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

function safeRelativePaths(root, cwd, input) {
  const paths = new Set([
    ...collectPaths(input.tool_input),
    ...patchPaths(input.tool_input?.patch),
    ...patchPaths(input.tool_input?.command),
  ]);
  return [...paths].map((path) => {
    const normalized = normalize(path);
    const absolute = isAbsolute(normalized) ? resolve(normalized) : resolve(cwd, normalized);
    const relativePath = relative(root, absolute);
    if (relativePath === "" || relativePath === ".." || relativePath.startsWith(`..${process.platform === "win32" ? "\\" : "/"}`)) return null;
    return relativePath.replaceAll("\\", "/");
  }).filter(Boolean);
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
  const cwd = typeof input.cwd === "string" ? resolve(input.cwd) : process.cwd();
  const root = await findWorkspaceRoot(hookWorkspaceRoot) ?? await findWorkspaceRoot(cwd);
  if (root === null) throw new Error("Could not locate .codex/hooks/afk-checks.json");
  const config = JSON.parse(await readFile(join(root, ".codex", "hooks", "afk-checks.json"), "utf8"));
  const paths = safeRelativePaths(root, cwd, input);
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
