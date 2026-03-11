# Bazaar Tracker (Fabric)

Client-side Fabric mod that polls the Hypixel Bazaar API every 30 seconds and renders an in-game HUD overlay with the top 5 flips for a **50,000,000 coin** budget.

## Features

- Pulls data from `https://api.hypixel.net/skyblock/bazaar` every 30 seconds.
- Computes per-item:
  - buys per hour (`buyMovingWeek / 168`)
  - buy offer price (`buyPrice`)
  - sell offer price (`sellPrice`)
  - margin (`sellPrice - buyPrice`)
- Estimates max hourly profit using budget-constrained tradable volume.
- Displays the top 5 items by estimated hourly profit directly on HUD.

## Version note

Fabric is available for Java Edition versions (configured here for **Minecraft 1.21.1**, the closest practical Java Edition Fabric target for your requested `1.21.10`).

## Build a single in-game jar

This project includes a dedicated `releaseJar` task that produces one mod jar file ready to drop in your `mods/` folder.

```bash
export JAVA_HOME=/path/to/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
gradle clean releaseJar
```

Output jar path:

- `release/BazaarTracker-1.1.0-mc1.21.1.jar`

## Install in game

1. Install Fabric Loader for Minecraft 1.21.1.
2. Put `BazaarTracker-1.1.0-mc1.21.1.jar` in `.minecraft/mods/`.
3. Also install the matching Fabric API jar in `.minecraft/mods/`.

## Download prebuilt jar from GitHub Actions

1. Push this repo to GitHub and make sure this workflow file exists on your **default branch** (`main`/`master`).
2. Open the **Actions** tab and run **Build Fabric Mod** (or push a commit to `main`/`master`).
3. Download the artifact named `BazaarTracker-<commit_sha>`.
4. Use the jar inside in your `.minecraft/mods/` folder.

If you create a tag like `v1.1.0` (matches `v*`), the workflow also publishes a GitHub Release with the jar attached.

If you still do not see the action, check **Settings → Actions → General** and ensure Actions are enabled for this repository.

### CI build compatibility note

Fabric Loom `1.6-SNAPSHOT` can fail on newer Gradle versions with `Problems.forNamespace(...)` errors.
The workflow pins Gradle to `8.8` to avoid this compatibility issue.
