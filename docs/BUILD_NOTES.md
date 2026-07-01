# Build notes

## NeoForge 1.21.1

Built with Java 21 and NeoForge `21.1.193`. Compatibility fixes applied:

- Removed `SkyRendererSunMoonFreezeMixin` because `net.minecraft.client.renderer.SkyRenderer` is not present in Minecraft 1.21.1.
- Replaced preview thumbnail rendering with the 1.21.1-compatible `GuiGraphics.blit(ResourceLocation, int, int, int, int, int, int, int, int)` overload.

## Fabric

Fabric jars were produced by `source/build_fabric_minimal_capture.py` using Fabric Loom and version-specific Fabric API dependencies.
