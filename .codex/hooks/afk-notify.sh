#!/usr/bin/env sh

sound="$(dirname "$0")/assets/notify.mp3"
if command -v afplay >/dev/null 2>&1 && [ -f "$sound" ]; then
  afplay "$sound" >/dev/null 2>&1 || true
fi
printf '{}\n'
