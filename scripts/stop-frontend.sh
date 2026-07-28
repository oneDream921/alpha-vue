#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

FRONTEND_PORT="${FRONTEND_PORT:-5173}"
stop_managed_process "$FRONTEND_PID_FILE" "$FRONTEND_PORT" "frontend"
echo "Frontend stopped."
