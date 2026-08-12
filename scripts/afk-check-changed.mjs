#!/usr/bin/env node

import { readFile } from "node:fs/promises";
import { spawn } from "node:child_process";
import { extname, resolve, sep } from "node:path";

const workspace = process.cwd();
const files = process.argv.slice(2).map((file) => resolve(workspace, file));

function inside(directory, file) {
  const prefix = `${resolve(workspace, directory)}${sep}`;
  return file.startsWith(prefix);
}

function run(command, args) {
  return new Promise((resolveRun, reject) => {
    const child = spawn(command, args, { cwd: workspace, stdio: "inherit" });
    child.once("error", reject);
    child.once("exit", (code, signal) => {
      if (signal) reject(new Error(`${command} terminated by ${signal}`));
      else resolveRun(code ?? 1);
    });
  });
}

for (const file of files.filter((candidate) => extname(candidate) === ".json")) {
  JSON.parse(await readFile(file, "utf8"));
}

const nodeFiles = files.filter((file) => [".js", ".mjs"].includes(extname(file))
  && !inside("alpha-web", file)
  && !inside("alpha-web-soybean", file));
for (const file of nodeFiles) {
  if (await run(process.execPath, ["--check", file]) !== 0) process.exit(1);
}

for (const directory of ["alpha-web", "alpha-web-soybean"]) {
  const frontendFiles = files.filter((file) => inside(directory, file)
    && [".js", ".mjs", ".ts", ".tsx", ".vue"].includes(extname(file)));
  if (frontendFiles.length === 0) continue;
  const code = await run("pnpm", ["--dir", directory, "exec", "eslint", "--", ...frontendFiles]);
  if (code !== 0) process.exit(code);
}
