#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

FRONTEND_PORT="${FRONTEND_PORT:-5173}"
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
echo "Frontend ready: http://localhost:$FRONTEND_PORT"
