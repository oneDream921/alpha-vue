#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
ENV_FILE="$ROOT_DIR/deploy/.env"
LOG_DIR="${TMPDIR:-/tmp}/alpha-vue-local-start"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"
BACKEND_PID_FILE="$LOG_DIR/backend.pid"
FRONTEND_PID_FILE="$LOG_DIR/frontend.pid"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

mkdir -p "$LOG_DIR"

load_env_file() {
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "Missing deploy/.env: $ENV_FILE" >&2
    exit 1
  fi

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" != *=* ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
    export "$key=$value"
  done < "$ENV_FILE"
}

is_docker_process() {
  local pid="$1"
  local command
  command="$(ps -p "$pid" -o comm= 2>/dev/null || true)"
  [[ "$command" == *Docker* || "$command" == *docker* || "$command" == com.docke* ]]
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
    echo "Kill listener on $label port $port: pid=$pid"
    kill "$pid" 2>/dev/null || true
    sleep 1
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" 2>/dev/null || true
    fi
  done <<< "$pids"
}

port_is_listening() {
  local port="$1"
  lsof -tiTCP:"$port" -sTCP:LISTEN -n -P >/dev/null 2>&1
}

ensure_dependency_ports() {
  local missing=()
  for port in "${MYSQL_PORT:-13306}" "${REDIS_PORT:-16379}" "${MINIO_API_PORT:-19000}"; do
    if ! port_is_listening "$port"; then
      missing+=("$port")
    fi
  done

  if (( ${#missing[@]} == 0 )); then
    echo "Dependency ports are already listening; reuse existing MySQL/Redis/MinIO."
    return 0
  fi

  echo "Missing dependency ports: ${missing[*]}; starting Docker Compose dependencies."
  docker compose --env-file "$ENV_FILE" -f "$ROOT_DIR/deploy/docker-compose.yml" up -d
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
    if curl -fsSI "$url" >/dev/null 2>&1; then
      return 0
    fi
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

start_backend() {
  : "${SERVER_PORT:=8080}"
  : "${FILE_ACCESS_TOKEN_SECRET:=alpha-local-dev-file-access-token-secret}"
  export SERVER_PORT FILE_ACCESS_TOKEN_SECRET

  kill_non_docker_port "$SERVER_PORT" "backend"

  : > "$BACKEND_LOG"
  spawn_detached "$ROOT_DIR" "$BACKEND_LOG" "$BACKEND_PID_FILE" \
    ./mvnw -f alpha-server/pom.xml spring-boot:run

  echo "Backend starting: pid=$(cat "$BACKEND_PID_FILE"), log=$BACKEND_LOG"
  if ! wait_for_text "http://localhost:$SERVER_PORT/actuator/health" '"status":"UP"' 90; then
    echo "Backend did not become healthy. Last log lines:" >&2
    tail -80 "$BACKEND_LOG" >&2 || true
    exit 1
  fi
}

start_frontend() {
  if ! command -v pnpm >/dev/null 2>&1; then
    echo "pnpm is required to start alpha-web." >&2
    exit 1
  fi

  kill_non_docker_port "$FRONTEND_PORT" "frontend"

  : > "$FRONTEND_LOG"
  spawn_detached "$ROOT_DIR" "$FRONTEND_LOG" "$FRONTEND_PID_FILE" \
    pnpm --dir alpha-web dev -- --host localhost --port "$FRONTEND_PORT" --strictPort

  echo "Frontend starting: pid=$(cat "$FRONTEND_PID_FILE"), log=$FRONTEND_LOG"
  if ! wait_for_http "http://localhost:$FRONTEND_PORT/" 60; then
    echo "Frontend did not respond. Last log lines:" >&2
    tail -80 "$FRONTEND_LOG" >&2 || true
    exit 1
  fi
}

main() {
  load_env_file
  ensure_dependency_ports
  start_backend
  start_frontend

  echo
  echo "Alpha Vue local environment is ready."
  echo "Backend:  http://localhost:${SERVER_PORT:-8080}"
  echo "Health:   http://localhost:${SERVER_PORT:-8080}/actuator/health"
  echo "Frontend: http://localhost:$FRONTEND_PORT"
  echo "Logs:     $BACKEND_LOG"
  echo "          $FRONTEND_LOG"
}

main "$@"
