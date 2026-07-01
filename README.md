# ITRP Minimal Capture / Screenshot Manager

A small client-side Minecraft screenshot manager built and tested for selected Fabric and NeoForge versions.

## Downloads

Prebuilt jars are stored in [`builds/`](builds/) and will also be attached to GitHub Releases.

## Tested versions

- Fabric 1.21 - 1.21.11: tested in the multi-version test set after dependency alignment.
- NeoForge 1.21.1: tested successfully in the user's launcher instance.

See [`docs/TESTED_VERSIONS.md`](docs/TESTED_VERSIONS.md) for details.

## Install

1. Pick the jar matching your Minecraft version and loader.
2. Put it into the target instance `mods` folder.
3. Start Minecraft with the matching Fabric/NeoForge loader.

## Source

- [`source/neoforge-1.21.1`](source/neoforge-1.21.1): NeoForge 1.21.1 source project.
- [`source/build_fabric_minimal_capture.py`](source/build_fabric_minimal_capture.py): Fabric multi-version build automation used for tested Fabric jars.

## Notes

This repository is arranged for practical downloading and source inspection. Heavy Gradle caches and generated build folders are intentionally excluded.
