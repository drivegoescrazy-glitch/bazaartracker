#!/usr/bin/env bash
set -euo pipefail

JAVA21=/root/.local/share/mise/installs/java/21.0.2
if [[ -n "${JAVA_HOME:-}" ]]; then
  USE_JAVA_HOME="$JAVA_HOME"
elif [[ -d "$JAVA21" ]]; then
  USE_JAVA_HOME="$JAVA21"
else
  echo "Set JAVA_HOME to a JDK 21 installation before running this script." >&2
  exit 1
fi

JAVA_HOME="$USE_JAVA_HOME" PATH="$USE_JAVA_HOME/bin:$PATH" gradle --no-daemon clean releaseJar

echo "Built: release/BazaarTracker-1.1.0-mc1.21.1.jar"
