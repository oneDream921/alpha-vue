#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

load_env_file
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to start local dependencies." >&2
  exit 1
fi

docker compose --project-name "$COMPOSE_PROJECT_NAME" --env-file "$ENV_FILE" -f "$ROOT_DIR/deploy/docker-compose.yml" up -d
echo "MySQL, Redis, and MinIO dependencies are starting or already running for Compose project $COMPOSE_PROJECT_NAME."
