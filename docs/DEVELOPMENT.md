# Development guide

This document is written so a future maintainer can work on the project without reading the original chat history.

## Project goal

ITRP Minimal Capture is a small client-side Minecraft screenshot/capture manager. The repository is arranged for two practical goals:

1. Users can download tested jars quickly.
2. Developers can inspect and continue the source with enough context.

## Repository layout

```text
README.md
builds/
  Prebuilt tested jars for direct download.
docs/
  Maintenance, release, build, and tested-version notes.
source/
  build_fabric_minimal_capture.py
  neoforge-1.21.1/
```

Important paths:

- `builds/`: prebuilt mod jars. These are also attached to the GitHub Release.
- `source/neoforge-1.21.1/`: NeoForge 1.21.1 source project.
- `source/build_fabric_minimal_capture.py`: Fabric multi-version build automation used for the published Fabric jars.

## Java and Gradle expectations

- Use Java 21 for Minecraft 1.21.x work.
- The NeoForge source was built against NeoForge `21.1.193`.
- The Gradle wrapper scripts/properties are present, but `gradle-wrapper.jar` was intentionally not uploaded because GitHub Contents API rejected that binary wrapper file during repository maintenance upload. If wrapper execution is needed, regenerate it or use a local Gradle installation.

## NeoForge 1.21.1 development

Source directory:

```text
source/neoforge-1.21.1
```

Typical build command when a valid Gradle runtime is available:

```bash
cd source/neoforge-1.21.1
gradle build
```

If using a regenerated wrapper:

```bash
cd source/neoforge-1.21.1
./gradlew build
```

Known 1.21.1 compatibility notes:

- `SkyRendererSunMoonFreezeMixin` was removed because `net.minecraft.client.renderer.SkyRenderer` is not present in Minecraft 1.21.1.
- Screenshot preview rendering uses the 1.21.1-compatible `GuiGraphics.blit(ResourceLocation, int, int, int, int, int, int, int, int)` overload.
- Keep mixin target classes version-specific. Do not copy high-version mixins into 1.21.1 without checking class existence.

## Fabric multi-version development

The published Fabric jars were produced by:

```text
source/build_fabric_minimal_capture.py
```

This script was used as the build automation entry point for 1.21.x Fabric jars. When extending Fabric support:

1. Add or update the target Minecraft version configuration in the script.
2. Verify Fabric Loader, Fabric API, and optional shader dependencies for that exact Minecraft version.
3. Build the jar.
4. Test in a matching Minecraft instance.
5. Add the jar to `builds/` and to a GitHub Release.
6. Update `docs/TESTED_VERSIONS.md`.

## Testing checklist

For each target version:

1. Start a clean Minecraft instance with the matching loader.
2. Put only the matching jar and required dependencies into `mods/`.
3. Confirm the game reaches title screen.
4. Confirm the mod UI entry points work.
5. Confirm screenshot/capture actions do not crash.
6. If shader integration is relevant, test with the matching Iris/Sodium or NeoForge shader stack.
7. Record the result in `docs/TESTED_VERSIONS.md`.

## Common failure patterns

### Missing Minecraft class in mixin target

Symptom: crash during launch or mixin application.

Likely cause: a mixin targets a class that does not exist in that Minecraft version.

Fix:

- Check the target class against the version's mappings/classes.
- Remove, split, or gate the mixin for that version.
- Update the mixin config.

### Dependency mismatch

Symptom: loader rejects mod set or crashes before title screen.

Likely cause: Fabric API, Sodium, Iris, or loader version does not match the Minecraft version.

Fix:

- Use dependency versions built for the exact Minecraft target.
- Keep per-version test instances clean.

### GUI method signature mismatch

Symptom: compilation error around `GuiGraphics`, rendering, or blit calls.

Likely cause: Minecraft GUI rendering signatures changed between versions.

Fix:

- Check the target version method signature.
- Use version-specific source patches when needed.

## Maintainer rule

Do not treat the published jars as enough proof for a new change. Every new jar should be built, installed into a matching instance, launched, and recorded before being marked tested.