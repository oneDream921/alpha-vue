#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

load_env_file
validate_system_settings_master_key
: "${SERVER_PORT:=8080}"
: "${FILE_ACCESS_TOKEN_SECRET:=alpha-local-dev-file-access-token-secret}"
export SERVER_PORT FILE_ACCESS_TOKEN_SECRET

ensure_dependency_ports
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
echo "Backend ready: http://localhost:$SERVER_PORT"
