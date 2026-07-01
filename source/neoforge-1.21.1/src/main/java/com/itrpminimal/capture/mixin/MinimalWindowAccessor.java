package com.itrpminimal.capture.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Window.class)
public interface MinimalWindowAccessor {
    @Accessor("framebufferWidth")
    int itrpminimal$getFramebufferWidth();

    @Accessor("framebufferHeight")
    int itrpminimal$getFramebufferHeight();

    @Accessor("framebufferWidth")
    void itrpminimal$setFramebufferWidth(int width);

    @Accessor("framebufferHeight")
    void itrpminimal$setFramebufferHeight(int height);

    @Accessor("width")
    void itrpminimal$setWidth(int width);

    @Accessor("height")
    void itrpminimal$setHeight(int height);
}