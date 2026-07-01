# NeoForge 1.21.1 build report

- status: success
- source project: `/data/user/0/com.ai.assistance.operit/files/workspace/1e9d9302-e5d4-49be-b7d3-183760e4574d/workspace/minimal_clean_from_uploaded_0319`
- built jar: `/data/user/0/com.ai.assistance.operit/files/workspace/1e9d9302-e5d4-49be-b7d3-183760e4574d/workspace/minimal_clean_from_uploaded_0319/build/libs/itrp-minimal-capture-neoforge-0.3.19+mc1.21.1.jar`
- copied jar: `/storage/emulated/0/AW资源工作区/mc.资源/新基底高版本打包/1.21.1-neoforge/itrp-minimal-capture-neoforge-0.3.19+mc1.21.1.jar`
- size: `109459`
- log: `/data/user/0/com.ai.assistance.operit/files/workspace/1e9d9302-e5d4-49be-b7d3-183760e4574d/workspace/neoforge_1_21_1_build.log`
- error summary: `/data/user/0/com.ai.assistance.operit/files/workspace/1e9d9302-e5d4-49be-b7d3-183760e4574d/workspace/neoforge_1_21_1_build_error_summary.log`

## Compatibility patches applied
- Removed `SkyRendererSunMoonFreezeMixin` because `net.minecraft.client.renderer.SkyRenderer` is not present in Minecraft 1.21.1.
- Replaced preview thumbnail draw call with the Minecraft 1.21.1-compatible `GuiGraphics.blit(ResourceLocation, int, int, int, int, int, int, int, int)` overload.
