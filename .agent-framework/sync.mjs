#!/usr/bin/env node
import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const PACKAGE_NAME = "@onedream921/agent-framework-kit";
const frameworkRoot = dirname(fileURLToPath(import.meta.url));
const workspace = dirname(frameworkRoot);
const manifestPath = join(frameworkRoot, "manifest.json");
const forwardedArgs = process.argv.slice(2);
const allowedArgs = new Set(["--dry-run", "--help", "-h"]);

if (forwardedArgs.length > 1 || forwardedArgs.some((arg) => !allowedArgs.has(arg))) {
  console.error("Error: 本地 AFK 同步脚本只接受 --dry-run、--help 或 -h。");
  process.exitCode = 1;
} else {
  try {
    const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
    const version = manifest?.frameworkVersion;
    if (manifest?.schemaVersion !== 1 || typeof version !== "string" || !/^[0-9A-Za-z][0-9A-Za-z.+-]*$/u.test(version)) {
      throw new Error("manifest.json 中的 AFK 版本无效");
    }

    const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";
    const npmArgs = [
      "exec",
      "--yes",
      "--prefer-offline",
      `--package=${PACKAGE_NAME}@${version}`,
      "--",
      "agent",
      "sync",
      "--workspace",
      workspace,
      ...forwardedArgs,
    ];
    const exitCode = await new Promise((resolve) => {
      const child = spawn(npmCommand, npmArgs, { cwd: workspace, stdio: "inherit" });
      child.once("error", (error) => {
        console.error(`Error: 无法启动 npm 执行 AFK ${version}: ${error.message}。请检查私有 Registry 与读取权限。`);
        resolve(1);
      });
      child.once("exit", (code, signal) => {
        if (signal) console.error(`Error: AFK 同步进程被信号 ${signal} 终止`);
        resolve(code ?? 1);
      });
    });
    process.exitCode = exitCode;
  } catch (error) {
    console.error(`Error: 无法通过 ${manifestPath} 启动本地 AFK 同步: ${error.message}`);
    process.exitCode = 1;
  }
}
