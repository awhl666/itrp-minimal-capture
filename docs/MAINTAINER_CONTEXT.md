# Maintainer context

This file preserves the project context needed for future maintenance. It is intended for humans and AI assistants that continue work without access to the original conversation.

## Current project state

Repository:

```text
https://github.com/awhl666/itrp-minimal-capture
```

Current release:

```text
v0.3.19-tested
```

Release URL:

```text
https://github.com/awhl666/itrp-minimal-capture/releases/tag/v0.3.19-tested
```

## Published tested builds

Fabric:

- Minecraft 1.21
- Minecraft 1.21.1
- Minecraft 1.21.2
- Minecraft 1.21.3
- Minecraft 1.21.4
- Minecraft 1.21.5
- Minecraft 1.21.6
- Minecraft 1.21.7
- Minecraft 1.21.8
- Minecraft 1.21.9
- Minecraft 1.21.10
- Minecraft 1.21.11

NeoForge:

- Minecraft 1.21.1 + NeoForge 21.1.193

## User-facing project requirements

The repository should serve two audiences:

1. Normal users who only want to download a jar.
2. Developers or maintainers who want to understand and continue development.

Therefore, keep both paths obvious:

- Downloads: GitHub Releases and `builds/`.
- Source: `source/` and docs.

## Current source status

- NeoForge 1.21.1 source is included under `source/neoforge-1.21.1/`.
- Fabric multi-version build automation is included as `source/build_fabric_minimal_capture.py`.
- The exact temporary build workspace used during original packaging is not required for ordinary users, but the published build and test reports are included in `docs/`.

## Known decisions already made

### Repository structure

The repository intentionally keeps generated Gradle caches and build directories out of version control.

### `gradle-wrapper.jar`

`gradle-wrapper.jar` was skipped because the GitHub Contents API rejected that binary during upload. This is not a functional decision about the project itself. If a maintainer wants wrapper support, regenerate or re-add the wrapper using normal git tooling.

### NeoForge 1.21.1 compatibility fixes

The 1.21.1 NeoForge build required version-specific compatibility work:

- Removed a mixin targeting `net.minecraft.client.renderer.SkyRenderer` because that class is not present in Minecraft 1.21.1.
- Adjusted screenshot preview rendering to a 1.21.1-compatible `GuiGraphics.blit` overload.

### Fabric dependency alignment

During Fabric multi-version testing, dependency mismatches were found and fixed, including Iris/Sodium combinations for 1.21.9 and 1.21.11.

## How to respond to future maintenance requests

When a future request asks for a new version, bug fix, feature, or repackaging:

1. Inspect repository files first.
2. Check `docs/DEVELOPMENT.md`, `docs/RELEASE_PROCESS.md`, and `docs/TESTED_VERSIONS.md`.
3. Identify whether the request affects Fabric, NeoForge, or both.
4. Confirm the exact Minecraft version and loader version.
5. Build in a clean, version-specific environment.
6. Test launch before claiming success.
7. Update docs and release artifacts.
8. Do not rely on memory of previous chat unless repository files confirm it.

## Preferred maintenance quality bar

A change is complete only when:

- Source is updated.
- Build artifact is produced.
- The artifact is tested in a matching instance.
- `docs/TESTED_VERSIONS.md` is updated.
- `docs/BUILD_NOTES.md` is updated if compatibility work was needed.
- Release assets are updated or a new Release is created.
- The public GitHub page is checked after upload.

## Next likely work areas

- Add clearer per-loader download table in README.
- Add checksums for published jars.
- Add source for Fabric project generation rather than only the build automation script.
- Add GitHub Actions build workflow if Gradle/JDK setup is stabilized.
- Add issue templates for bug reports and version requests.