# ITRP Minimal Capture / Screenshot Manager

[![Release](https://img.shields.io/github/v/release/awhl666/itrp-minimal-capture?label=release)](https://github.com/awhl666/itrp-minimal-capture/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21--1.21.11-brightgreen)](docs/TESTED_VERSIONS.md)
[![Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20NeoForge-blue)](docs/DOWNLOADS.md)

A client-side Minecraft screenshot/capture manager with tested builds for selected Fabric and NeoForge versions.

## Download

For normal users, start here:

- **Download matrix:** [`docs/DOWNLOADS.md`](docs/DOWNLOADS.md)
- **Latest release:** [`v0.3.19-tested`](https://github.com/awhl666/itrp-minimal-capture/releases/tag/v0.3.19-tested)
- **Checksum file:** [`docs/CHECKSUMS_SHA256.txt`](docs/CHECKSUMS_SHA256.txt)

### Quick Pick

| Loader | Minecraft | Download |
| --- | --- | --- |
| Fabric | 1.21 - 1.21.11 | See [`docs/DOWNLOADS.md`](docs/DOWNLOADS.md#download-table) |
| NeoForge | 1.21.1 + NeoForge 21.1.193 | See [`docs/DOWNLOADS.md`](docs/DOWNLOADS.md#download-table) |

## Install

1. Open [`docs/DOWNLOADS.md`](docs/DOWNLOADS.md).
2. Choose the row matching your loader and Minecraft version.
3. Download that exact jar from the Release asset link.
4. Put the jar in the target instance `mods` folder.
5. Start Minecraft with the matching Fabric or NeoForge loader.

Do not install Fabric and NeoForge variants together.

## Source

- [`source/neoforge-1.21.1`](source/neoforge-1.21.1): NeoForge 1.21.1 source project.
- [`source/build_fabric_minimal_capture.py`](source/build_fabric_minimal_capture.py): Fabric multi-version build automation used for tested Fabric jars.

## Project status

| Area | Status |
| --- | --- |
| Fabric 1.21 - 1.21.11 builds | Published and launch-tested |
| NeoForge 1.21.1 build | Published and launch-tested |
| Source visibility | Available under `source/` |
| Release assets | Published under `v0.3.19-tested` |

See [`docs/TESTED_VERSIONS.md`](docs/TESTED_VERSIONS.md) for the full tested artifact list.

## Development and maintenance

Start here if you want to understand or continue the project:

- [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md): development environment, structure, testing checklist, common failure patterns.
- [`docs/MAINTAINER_CONTEXT.md`](docs/MAINTAINER_CONTEXT.md): project context for future maintainers and AI assistants.
- [`docs/RELEASE_PROCESS.md`](docs/RELEASE_PROCESS.md): how to add builds and publish releases.
- [`docs/BUILD_NOTES.md`](docs/BUILD_NOTES.md): version-specific build notes.
- [`docs/NEXT_STEPS.md`](docs/NEXT_STEPS.md): recommended next improvements.

## Notes

Heavy Gradle caches and generated build folders are intentionally excluded. The Gradle wrapper scripts/properties are present, but `gradle-wrapper.jar` was not uploaded during API-based repository creation because GitHub rejected that binary wrapper file. Use a local Gradle installation or regenerate the wrapper if needed.
