# Release process

Use this process whenever publishing a new jar or adding support for another Minecraft version.

## Release principles

- Do not mark a build as tested until it has launched in a matching Minecraft instance.
- Keep user downloads easy: upload jars to both `builds/` and GitHub Release assets.
- Keep developer context current: update docs whenever compatibility decisions are made.

## Version naming

Current naming pattern:

Fabric:

```text
itrp-minimal-capture-fabric-<minecraft-version>-<mod-version>+fabric.jar
```

Example:

```text
itrp-minimal-capture-fabric-1.21.11-0.3.19+fabric.jar
```

NeoForge:

```text
itrp-minimal-capture-neoforge-<mod-version>+mc<minecraft-version>.jar
```

Example:

```text
itrp-minimal-capture-neoforge-0.3.19+mc1.21.1.jar
```

## Build checklist

1. Confirm target loader and Minecraft version.
2. Check dependency versions for that exact target.
3. Build the jar.
4. Check jar metadata/mod descriptor.
5. Copy the jar to a clean test instance.
6. Launch the game.
7. Test core UI and capture behavior.
8. Record the result.

## GitHub update checklist

For every new tested build:

1. Add the jar to `builds/`.
2. Update `docs/TESTED_VERSIONS.md`.
3. Update `docs/BUILD_NOTES.md` if there were compatibility fixes.
4. Update `README.md` if the download table or support matrix changed.
5. Create a new GitHub Release or update the current release.
6. Attach the jar as a Release asset.
7. Verify the public GitHub Release page.

## Recommended release tag style

For a tested set:

```text
v<mod-version>-tested
```

Example:

```text
v0.3.19-tested
```

If publishing a loader-specific hotfix:

```text
v<mod-version>-<loader>-<minecraft-version>-hotfix<N>
```

Example:

```text
v0.3.20-neoforge-1.21.1-hotfix1
```

## What to include in release notes

Release notes should include:

- Supported Minecraft versions.
- Supported loaders.
- Important dependency notes.
- Known limitations.
- Whether the build was launch-tested.

## Example release notes

```text
Tested builds for Fabric 1.21-1.21.11 and NeoForge 1.21.1.

Install:
1. Download the jar matching your Minecraft version and loader.
2. Put it into the target instance mods folder.
3. Start Minecraft with the matching loader.

Known notes:
- NeoForge 1.21.1 was tested with NeoForge 21.1.193.
- Fabric versions require matching Fabric API and compatible optional shader dependencies.
```

## Rollback policy

If a published jar is later found broken:

1. Do not silently delete context.
2. Mark the affected version in `docs/TESTED_VERSIONS.md` as broken or superseded.
3. Upload a corrected jar with a new version or hotfix suffix.
4. Mention the broken artifact in release notes.
5. Keep enough information for users to avoid the broken version.