const DANGEROUS_SUBCOMMANDS = new Set([
  "checkout", "cherry-pick", "clean", "fetch", "merge", "pull", "push", "rebase", "reset", "restore", "revert", "switch",
]);

const READ_ONLY_SUBCOMMANDS = new Set([
  "annotate", "blame", "cat-file", "count-objects", "describe", "diff", "diff-tree", "for-each-ref", "grep", "help",
  "log", "ls-files", "ls-remote", "ls-tree", "merge-base", "name-rev", "range-diff", "rev-list", "rev-parse",
  "shortlog", "show", "show-ref", "status", "version", "whatchanged",
]);

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

function invocationOf(tokens) {
  for (let index = 0; index < tokens.length; index += 1) {
    const token = tokens[index];
    if (!token.startsWith("-")) return {
      subcommand: token.replace(/^['"]|['"]$/gu, "").replace(/[^a-z-].*$/iu, "").toLowerCase(),
      args: tokens.slice(index + 1),
    };
    const optionName = token.split("=", 1)[0];
    if (OPTIONS_WITH_VALUE.has(optionName) && !token.includes("=")) index += 1;
  }
  return { subcommand: "", args: [] };
}

function nestedShellCommand(tokens, index) {
  const commandIndex = tokens.findIndex((token, tokenIndex) => tokenIndex > index && /^(?:-[a-zA-Z]*c[a-zA-Z]*|--command)$/u.test(token));
  return commandIndex >= 0 ? tokens[commandIndex + 1] : undefined;
}

function gitInvocations(command) {
  const invocations = [];
  for (const substitution of command.matchAll(/\$\(([^()]*)\)/gu)) invocations.push(...gitInvocations(substitution[1]));
  for (const shellCommand of splitShellCommands(command)) {
    const tokens = tokenize(shellCommand);
    let index = 0;
    while (index < tokens.length && /^[A-Za-z_][A-Za-z0-9_]*=/u.test(tokens[index])) index += 1;
    if (tokens[index] === "env") { index += 1; while (index < tokens.length && (tokens[index].startsWith("-") || /^[A-Za-z_][A-Za-z0-9_]*=/u.test(tokens[index]))) index += 1; }
    while (["command", "sudo"].includes(tokens[index])) index += 1;
    const executable = tokens[index]?.split("/").at(-1);
    if (["bash", "sh", "zsh"].includes(executable)) {
      const nested = nestedShellCommand(tokens, index);
      if (nested) invocations.push(...gitInvocations(nested));
    } else if (executable === "xargs") {
      const gitIndex = tokens.findIndex((token, tokenIndex) => tokenIndex > index && token.split("/").at(-1) === "git");
      if (gitIndex >= 0) invocations.push(invocationOf(tokens.slice(gitIndex + 1)));
    } else if (executable === "git") invocations.push(invocationOf(tokens.slice(index + 1)));
  }
  return invocations;
}

function isReadOnlyRefList(args, { allowShortList = false, allowShowCurrent = false, allowVerify = false } = {}) {
  if (args.length === 0) return true;
  let permitsPatterns = false;
  const flags = new Set(["-a", "--all", "-r", "--remotes", "-v", "-vv", "--verbose", "--no-abbrev", "--column", "--no-column", "--ignore-case", "--omit-empty"]);
  const optionsWithRequiredValue = new Set(["--format", "--sort", "--points-at"]);
  const optionsWithOptionalValue = new Set(["--color", "--contains", "--no-contains", "--merged", "--no-merged"]);

  for (let index = 0; index < args.length; index += 1) {
    const token = args[index];
    if (token === "--list" || (allowShortList && token === "-l")) permitsPatterns = true;
    else if (allowShowCurrent && token === "--show-current") permitsPatterns = true;
    else if (allowVerify && (token === "--verify" || token === "-v")) permitsPatterns = true;
    else if (flags.has(token) || (allowVerify && /^-n\d*$/u.test(token))) continue;
    else if ([...optionsWithRequiredValue, ...optionsWithOptionalValue].some((option) => token.startsWith(`${option}=`))) continue;
    else if (optionsWithRequiredValue.has(token)) {
      if (!args[index + 1]) return false;
      index += 1;
    } else if (optionsWithOptionalValue.has(token)) {
      if (args[index + 1] && !args[index + 1].startsWith("-")) index += 1;
    } else if (token.startsWith("-") || !permitsPatterns) return false;
  }
  return true;
}

function isReadOnlyConfig(args) {
  const flags = new Set([
    "--global", "--system", "--local", "--worktree", "--includes", "--no-includes", "--show-origin", "--show-scope",
    "--null", "-z", "--name-only", "--fixed-value",
  ]);
  const readActions = new Set(["--get", "--get-all", "--get-regexp", "--get-urlmatch", "--list", "-l"]);
  const optionsWithValue = new Set(["--default", "--type"]);
  let action = "";
  const positionals = [];

  for (let index = 0; index < args.length; index += 1) {
    const token = args[index];
    if (flags.has(token)) continue;
    if (readActions.has(token)) { action = token; continue; }
    if ([...optionsWithValue].some((option) => token.startsWith(`${option}=`))) continue;
    if (optionsWithValue.has(token)) {
      if (!args[index + 1]) return false;
      index += 1;
    } else if (token.startsWith("-")) return false;
    else positionals.push(token);
  }

  if (!action) return positionals.length === 1;
  if (action === "--list" || action === "-l") return positionals.length === 0;
  return positionals.length >= 1;
}

function isReadOnlyRemote(args) {
  const filtered = args.filter((token) => token !== "-v" && token !== "--verbose");
  if (filtered.length === 0) return true;
  const [mode, ...rest] = filtered;
  if (mode === "show") return rest.every((token) => token === "-n" || token === "--no-query" || !token.startsWith("-"));
  if (mode === "get-url") return rest.every((token) => token === "--all" || token === "--push" || !token.startsWith("-"));
  return false;
}

function isReadOnlyMixedSubcommand({ subcommand, args }) {
  if (subcommand === "branch") return isReadOnlyRefList(args, { allowShowCurrent: true });
  if (subcommand === "tag") return isReadOnlyRefList(args, { allowShortList: true, allowVerify: true });
  if (subcommand === "config") return isReadOnlyConfig(args);
  if (subcommand === "remote") return isReadOnlyRemote(args);
  if (subcommand === "stash") return ["list", "show"].includes(args[0]);
  if (subcommand === "submodule") return ["status", "summary"].includes(args[0]);
  if (subcommand === "worktree") return args[0] === "list";
  return false;
}

function isReadOnlyInvocation(invocation) {
  return invocation.subcommand === "" || READ_ONLY_SUBCOMMANDS.has(invocation.subcommand) || isReadOnlyMixedSubcommand(invocation);
}

export function findBlockedGitSubcommand(command) {
  return gitInvocations(command).map(({ subcommand }) => subcommand).find((subcommand) => DANGEROUS_SUBCOMMANDS.has(subcommand));
}

export function findLocalGitWriteSubcommand(command) {
  return gitInvocations(command).find((invocation) => !DANGEROUS_SUBCOMMANDS.has(invocation.subcommand) && !isReadOnlyInvocation(invocation))?.subcommand;
}
