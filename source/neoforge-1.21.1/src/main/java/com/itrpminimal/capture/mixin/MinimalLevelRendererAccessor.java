package com.itrpminimal.capture.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface MinimalLevelRendererAccessor {
    @Accessor("entityEffect")
    PostChain itrpminimal$getEntityEffect();

    @Accessor("entityTarget")
    RenderTarget itrpminimal$getEntityTarget();

    @Accessor("translucentTarget")
    RenderTarget itrpminimal$getTranslucentTarget();

    @Accessor("particlesTarget")
    RenderTarget itrpminimal$getParticlesTarget();

    @Accessor("weatherTarget")
    RenderTarget itrpminimal$getWeatherTarget();

    @Accessor("cloudsTarget")
    RenderTarget itrpminimal$getCloudsTarget();

    @Accessor("itemEntityTarget")
    RenderTarget itrpminimal$getItemEntityTarget();
}