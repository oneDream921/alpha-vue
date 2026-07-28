#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

load_env_file
: "${SERVER_PORT:=8080}"
stop_managed_process "$BACKEND_PID_FILE" "$SERVER_PORT" "backend"
echo "Backend stopped."
