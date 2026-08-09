#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$ROOT_DIR/deploy/.env"
LOG_DIR="${TMPDIR:-/tmp}/alpha-vue-local-start"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"
BACKEND_PID_FILE="$LOG_DIR/backend.pid"
FRONTEND_PID_FILE="$LOG_DIR/frontend.pid"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-alpha-vue-idea}"

mkdir -p "$LOG_DIR"

load_env_file() {
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "Missing deploy/.env: $ENV_FILE" >&2
    exit 1
  fi

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" == \#* || "$line" != *=* ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    if [[ -z "${!key+x}" ]]; then
      export "$key=$value"
    fi
  done < "$ENV_FILE"
}

validate_system_settings_master_key() {
  local key="${SYSTEM_SETTINGS_MASTER_KEY:-}"
  if [[ ! "$key" =~ ^[0-9A-Fa-f]{64}$ ]]; then
    echo "SYSTEM_SETTINGS_MASTER_KEY must be a 64-character hexadecimal value in deploy/.env." >&2
    return 1
  fi
}

is_docker_process() {
  local pid="$1"
  local command
  command="$(ps -p "$pid" -o comm= 2>/dev/null || true)"
  [[ "$command" == *Docker* || "$command" == *docker* || "$command" == com.docke* ]]
}

port_is_listening() {
  local port="$1"
  lsof -tiTCP:"$port" -sTCP:LISTEN -n -P >/dev/null 2>&1
}

kill_non_docker_port() {
  local port="$1"
  local label="$2"
  local pids
  pids="$(lsof -tiTCP:"$port" -sTCP:LISTEN -n -P 2>/dev/null || true)"
  [[ -z "$pids" ]] && return 0

  while IFS= read -r pid; do
    [[ -z "$pid" ]] && continue
    if is_docker_process "$pid"; then
      echo "Skip Docker-owned listener on $label port $port: pid=$pid"
      continue
    fi
    echo "Stop listener on $label port $port: pid=$pid"
    kill "$pid" 2>/dev/null || true
    sleep 1
    kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null || true
  done <<< "$pids"
}

stop_managed_process() {
  local pid_file="$1"
  local port="$2"
  local label="$3"
  local pid
  local pgid

  if [[ -f "$pid_file" ]]; then
    pid="$(cat "$pid_file")"
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
      pgid="$(ps -o pgid= -p "$pid" | tr -d ' ')"
      if [[ "$pgid" == "$pid" ]]; then
        echo "Stop managed $label process group: pid=$pid"
        kill -TERM -- "-$pgid" 2>/dev/null || true
      else
        echo "Stop managed $label process: pid=$pid"
        kill -TERM "$pid" 2>/dev/null || true
      fi
      for _ in {1..10}; do
        kill -0 "$pid" 2>/dev/null || break
        sleep 1
      done
      kill -0 "$pid" 2>/dev/null && kill -KILL "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
  fi

  kill_non_docker_port "$port" "$label"
}

wait_for_text() {
  local url="$1"
  local expected="$2"
  local seconds="$3"
  local i
  for ((i = 1; i <= seconds; i++)); do
    if curl -fsS "$url" 2>/dev/null | grep -q "$expected"; then
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_for_http() {
  local url="$1"
  local seconds="$2"
  local i
  for ((i = 1; i <= seconds; i++)); do
    curl -fsSI "$url" >/dev/null 2>&1 && return 0
    sleep 1
  done
  return 1
}

spawn_detached() {
  local cwd="$1"
  local log_file="$2"
  local pid_file="$3"
  shift 3

  python3 - "$cwd" "$log_file" "$pid_file" "$@" <<'PY'
import os
import subprocess
import sys

cwd, log_file, pid_file, *command = sys.argv[1:]
os.makedirs(os.path.dirname(log_file), exist_ok=True)
with open(log_file, "ab", buffering=0) as output:
    process = subprocess.Popen(
        command,
        cwd=cwd,
        env=os.environ.copy(),
        stdin=subprocess.DEVNULL,
        stdout=output,
        stderr=subprocess.STDOUT,
        start_new_session=True,
    )
with open(pid_file, "w", encoding="utf-8") as handle:
    handle.write(str(process.pid))
PY
}

ensure_dependency_ports() {
  local missing=()
  local port
  for port in "${MYSQL_PORT:-13306}" "${REDIS_PORT:-16379}" "${MINIO_API_PORT:-19000}"; do
    port_is_listening "$port" || missing+=("$port")
  done

  if (( ${#missing[@]} == 0 )); then
    echo "Dependency ports are already listening; reuse existing MySQL/Redis/MinIO."
    return 0
  fi

  echo "Missing dependency ports: ${missing[*]}; starting Docker Compose dependencies."
  "$ROOT_DIR/scripts/start-dependencies.sh"
}
