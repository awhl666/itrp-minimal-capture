# ITRP Minimal Capture / Screenshot Manager

A small client-side Minecraft screenshot manager built and tested for selected Fabric and NeoForge versions.

This repository is maintained for two goals:

1. Users can download tested jars quickly.
2. Developers can inspect the source and continue maintenance without needing the original build chat history.

## Downloads

Prebuilt jars are available in two places:

- GitHub Releases: [`v0.3.19-tested`](https://github.com/awhl666/itrp-minimal-capture/releases/tag/v0.3.19-tested)
- Repository folder: [`builds/`](builds/)

## Supported tested builds

| Loader | Minecraft version | Status |
| --- | --- | --- |
| Fabric | 1.21 - 1.21.11 | Launch-tested in prepared instances |
| NeoForge | 1.21.1 + NeoForge 21.1.193 | Launch-tested successfully |

See [`docs/TESTED_VERSIONS.md`](docs/TESTED_VERSIONS.md) for the full artifact list.

## Install

1. Download the jar matching your Minecraft version and loader.
2. Put it into the target instance `mods` folder.
3. Start Minecraft with the matching Fabric or NeoForge loader.

## Source

- [`source/neoforge-1.21.1`](source/neoforge-1.21.1): NeoForge 1.21.1 source project.
- [`source/build_fabric_minimal_capture.py`](source/build_fabric_minimal_capture.py): Fabric multi-version build automation used for tested Fabric jars.

## Development and maintenance

Start here if you want to understand or continue the project:

- [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md): development environment, structure, testing checklist, common failure patterns.
- [`docs/MAINTAINER_CONTEXT.md`](docs/MAINTAINER_CONTEXT.md): project context for future maintainers and AI assistants.
- [`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md): how to add builds and publish releases.
- [`docs/BUILD_NOTES.md`](docs/BUILD_NOTES.md): version-specific build notes.
- [`docs/NEXT_STEPS.md`](docs/NEXT_STEPS.md): recommended next improvements.

## Notes

Heavy Gradle caches and generated build folders are intentionally excluded. The Gradle wrapper scripts/properties are present, but `gradle-wrapper.jar` was not uploaded during API-based repository creation because GitHub rejected that binary wrapper file. Use a local Gradle installation or regenerate the wrapper if needed.
