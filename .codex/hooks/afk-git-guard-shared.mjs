const DANGEROUS_SUBCOMMANDS = new Set([
  "checkout", "cherry-pick", "clean", "merge", "pull", "push", "rebase", "reset", "restore", "revert", "stash", "switch",
]);

const LOCAL_WRITE_SUBCOMMANDS = new Set(["add", "commit"]);

const OPTIONS_WITH_VALUE = new Set(["-c", "-C", "--exec-path", "--git-dir", "--namespace", "--super-prefix", "--work-tree"]);

function splitShellCommands(command) {
  const commands = [];
  let current = "";
  let quote = "";
  for (let index = 0; index < command.length; index += 1) {
    const character = command[index];
    if (character === "\\" && quote !== "'") { current += character; if (index + 1 < command.length) current += command[++index]; }
    else if ((character === "'" || character === '"') && (!quote || quote === character)) { quote = quote ? "" : character; current += character; }
    else if (!quote && /[;&|\n]/u.test(character)) { if (current.trim()) commands.push(current.trim()); current = ""; }
    else current += character;
  }
  if (current.trim()) commands.push(current.trim());
  return commands;
}

function tokenize(command) {
  const tokens = [];
  let current = "";
  let quote = "";
  for (let index = 0; index < command.length; index += 1) {
    const character = command[index];
    if (character === "\\" && quote !== "'") { if (index + 1 < command.length) current += command[++index]; }
    else if ((character === "'" || character === '"') && (!quote || quote === character)) quote = quote ? "" : character;
    else if (!quote && /\s/u.test(character)) { if (current) tokens.push(current); current = ""; }
    else current += character;
  }
  if (current) tokens.push(current);
  return tokens;
}

function subcommandOf(tokens) {
  for (let index = 0; index < tokens.length; index += 1) {
    const token = tokens[index];
    if (!token.startsWith("-")) return token.replace(/^["']|["']$/gu, "").replace(/[^a-z-].*$/iu, "").toLowerCase();
    const optionName = token.split("=", 1)[0];
    if (OPTIONS_WITH_VALUE.has(optionName) && !token.includes("=")) index += 1;
  }
  return "";
}

function gitSubcommands(command) {
  const subcommands = [];
  for (const substitution of command.matchAll(/\$\(([^()]*)\)/gu)) subcommands.push(...gitSubcommands(substitution[1]));
  for (const shellCommand of splitShellCommands(command)) {
    const tokens = tokenize(shellCommand);
    let index = 0;
    while (index < tokens.length && /^[A-Za-z_][A-Za-z0-9_]*=/u.test(tokens[index])) index += 1;
    if (tokens[index] === "env") { index += 1; while (index < tokens.length && (tokens[index].startsWith("-") || /^[A-Za-z_][A-Za-z0-9_]*=/u.test(tokens[index]))) index += 1; }
    while (["command", "sudo"].includes(tokens[index])) index += 1;
    const executable = tokens[index]?.split("/").at(-1);
    if (["bash", "sh", "zsh"].includes(executable)) { const commandIndex = tokens.indexOf("-c", index + 1); if (commandIndex >= 0 && tokens[commandIndex + 1]) subcommands.push(...gitSubcommands(tokens[commandIndex + 1])); }
    else if (executable === "git") subcommands.push(subcommandOf(tokens.slice(index + 1)));
  }
  return subcommands;
}

export function findBlockedGitSubcommand(command) {
  return gitSubcommands(command).find((subcommand) => DANGEROUS_SUBCOMMANDS.has(subcommand));
}

export function findLocalGitWriteSubcommand(command) {
  return gitSubcommands(command).find((subcommand) => LOCAL_WRITE_SUBCOMMANDS.has(subcommand));
}
