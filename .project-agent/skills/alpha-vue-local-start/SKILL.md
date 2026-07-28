---
name: alpha-vue-local-start
description: Start the Alpha Vue local development environment quickly. Use when the user asks to start, restart, run, boot, or bring up the Alpha Vue project locally, including handling occupied app ports, reusing Docker dependency containers, launching the Spring Boot backend and Vue frontend, and reporting local URLs.
---

# Alpha Vue Local Start

For a normal local-project start or restart, call the one-click script once. Do not manually reconstruct ports, environment loading, Docker Compose dependencies, process management, or health checks. Wait for the script result before reporting success or failure.

## Quick Start

From the workspace root, run:

```bash
scripts/start-all.sh
```

Wait for the script to finish. It performs the backend health check and frontend availability check itself.

## Explicit Partial Operations

Only when the user explicitly asks to operate one service or the Docker Compose dependencies, call the matching script:

```bash
scripts/start-backend.sh
scripts/stop-backend.sh
scripts/start-frontend.sh
scripts/stop-frontend.sh
scripts/start-dependencies.sh
scripts/stop-dependencies.sh
```

## Report

After running the script, summarize:

- Backend URL and health result.
- Frontend URL.
- Whether dependency containers were reused or Compose was invoked.
- Any skipped Docker-owned port listeners.
- Log files from the script output.
