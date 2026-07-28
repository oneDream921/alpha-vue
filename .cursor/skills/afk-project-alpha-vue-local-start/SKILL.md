---
name: alpha-vue-local-start
description: Start the Alpha Vue local development environment quickly. Use when
  the user asks to start, restart, run, boot, or bring up the Alpha Vue project
  locally, including handling occupied app ports, reusing Docker dependency
  containers, launching the Spring Boot backend and Vue frontend, and reporting
  local URLs.
---

# Alpha Vue Local Start

Use the bundled script for local startup instead of rediscovering ports, env loading, dependency containers, and health checks.

## Quick Start

From the workspace root, run:

```bash
.project-agent/skills/alpha-vue-local-start/scripts/start-local.sh
```

The script will:

- Read `deploy/.env` without shell-sourcing it, so JDBC URLs containing `&` are safe.
- Reuse existing MySQL, Redis, and MinIO listeners on the configured ports.
- Run Docker Compose only when dependency ports are missing.
- Kill non-Docker listeners on the backend and frontend app ports before starting.
- Never kill Docker or Docker Desktop processes.
- Provide a local `FILE_ACCESS_TOKEN_SECRET` for the process when `.env` does not define one.
- Start backend and frontend in the background, then check health and print URLs and log paths.

## Report

After running the script, summarize:

- Backend URL and health result.
- Frontend URL.
- Whether dependency containers were reused or Compose was invoked.
- Any skipped Docker-owned port listeners.
- Log files from the script output.
