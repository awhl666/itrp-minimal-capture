package com.itrpminimal.capture;

import com.itrpminimal.capture.mixin.MinimalLevelRendererAccessor;
import com.itrpminimal.capture.mixin.MinimalWindowAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import org.lwjgl.opengl.GL11;

public final class MinimalHighResShell {
    private static int multiplier = 1;
    private static ResizeSnapshot snapshot = null;

    public static int multiplier() {
        return multiplier;
    }

    public static void setMultiplier(int value) {
        multiplier = Math.max(1, Math.min(4, value));
        MinimalLog.line("HIGHRES CONFIG multiplier=" + multiplier);
    }

    public static boolean isApplied() {
        return snapshot != null;
    }

    public static int targetWidth(Minecraft client) {
        ResizeSnapshot current = snapshot;
        if (current != null) {
            return current.targetWidth;
        }
        MinimalWindowAccessor window = (MinimalWindowAccessor) (Object) client.getWindow();
        return window.itrpminimal$getFramebufferWidth() * multiplier;
    }

    public static int targetHeight(Minecraft client) {
        ResizeSnapshot current = snapshot;
        if (current != null) {
            return current.targetHeight;
        }
        MinimalWindowAccessor window = (MinimalWindowAccessor) (Object) client.getWindow();
        return window.itrpminimal$getFramebufferHeight() * multiplier;
    }

    public static void applyIfNeeded(Minecraft client) {
        if (multiplier <= 1) {
            MinimalLog.line("HIGHRES skip multiplier=1");
            return;
        }
        if (snapshot != null) {
            MinimalLog.line("HIGHRES already applied target=" + snapshot.targetWidth + "x" + snapshot.targetHeight);
            return;
        }
        MinimalWindowAccessor window = (MinimalWindowAccessor) (Object) client.getWindow();
        int originalWidth = window.itrpminimal$getFramebufferWidth();
        int originalHeight = window.itrpminimal$getFramebufferHeight();
        int targetWidth = safeTarget(originalWidth, multiplier);
        int targetHeight = safeTarget(originalHeight, multiplier);
        int maxTextureSize = RenderSystem.maxSupportedTextureSize();
        if (targetWidth > maxTextureSize || targetHeight > maxTextureSize) {
            throw new IllegalStateException("target " + targetWidth + "x" + targetHeight + " exceeds max texture size " + maxTextureSize);
        }
        snapshot = new ResizeSnapshot(originalWidth, originalHeight, targetWidth, targetHeight);
        MinimalLog.line("HIGHRES APPLY original=" + originalWidth + "x" + originalHeight + " target=" + targetWidth + "x" + targetHeight + " multiplier=" + multiplier + " maxTexture=" + maxTextureSize);
        resize(client, targetWidth, targetHeight);
    }

    public static void restoreIfNeeded(Minecraft client) {
        ResizeSnapshot current = snapshot;
        if (current == null) {
            return;
        }
        try {
            MinimalLog.line("HIGHRES RESTORE original=" + current.originalWidth + "x" + current.originalHeight + " from=" + current.targetWidth + "x" + current.targetHeight);
            int bumpWidth = Math.max(1, current.originalWidth - 1);
            resize(client, bumpWidth, current.originalHeight);
            resize(client, current.originalWidth, current.originalHeight);
            RenderSystem.viewport(0, 0, current.originalWidth, current.originalHeight);
            client.getMainRenderTarget().bindWrite(true);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            MinimalLog.line("HIGHRES RESTORE done");
        } finally {
            snapshot = null;
        }
    }

    public static void ensureTargetStillApplied(Minecraft client) {
        ResizeSnapshot current = snapshot;
        if (current == null) {
            return;
        }
        MinimalWindowAccessor window = (MinimalWindowAccessor) (Object) client.getWindow();
        if (window.itrpminimal$getFramebufferWidth() != current.targetWidth || window.itrpminimal$getFramebufferHeight() != current.targetHeight) {
            MinimalLog.line("HIGHRES reapply target=" + current.targetWidth + "x" + current.targetHeight + " current=" + window.itrpminimal$getFramebufferWidth() + "x" + window.itrpminimal$getFramebufferHeight());
            resize(client, current.targetWidth, current.targetHeight);
        }
    }

    public static String summary(Minecraft client) {
        ResizeSnapshot current = snapshot;
        if (current != null) {
            return "倍率=" + multiplier + " 已应用=" + current.targetWidth + "x" + current.targetHeight + " 原始=" + current.originalWidth + "x" + current.originalHeight;
        }
        try {
            MinimalWindowAccessor window = (MinimalWindowAccessor) (Object) client.getWindow();
            return "倍率=" + multiplier + " 当前=" + window.itrpminimal$getFramebufferWidth() + "x" + window.itrpminimal$getFramebufferHeight();
        } catch (Throwable t) {
            return "倍率=" + multiplier + " 状态读取失败=" + t.getClass().getSimpleName();
        }
    }

    private static void resize(Minecraft client, int width, int height) {
        MinimalWindowAccessor window = (MinimalWindowAccessor) (Object) client.getWindow();
        window.itrpminimal$setFramebufferWidth(width);
        window.itrpminimal$setFramebufferHeight(height);
        window.itrpminimal$setWidth(width);
        window.itrpminimal$setHeight(height);
        RenderSystem.viewport(0, 0, width, height);

        RenderTarget mainTarget = client.getMainRenderTarget();
        if (mainTarget.width != width || mainTarget.height != height) {
            mainTarget.resize(width, height, Minecraft.ON_OSX);
        }

        MinimalLevelRendererAccessor levelRenderer = (MinimalLevelRendererAccessor) client.levelRenderer;
        resizePostChain(levelRenderer.itrpminimal$getEntityEffect(), width, height);
        resizeTarget(levelRenderer.itrpminimal$getEntityTarget(), width, height);
        resizeTarget(levelRenderer.itrpminimal$getTranslucentTarget(), width, height);
        resizeTarget(levelRenderer.itrpminimal$getParticlesTarget(), width, height);
        resizeTarget(levelRenderer.itrpminimal$getWeatherTarget(), width, height);
        resizeTarget(levelRenderer.itrpminimal$getCloudsTarget(), width, height);
        resizeTarget(levelRenderer.itrpminimal$getItemEntityTarget(), width, height);
        client.gameRenderer.resize(width, height);
        MinimalLog.line("HIGHRES resize applied " + width + "x" + height + " main=" + mainTarget.width + "x" + mainTarget.height);
    }

    private static void resizePostChain(PostChain chain, int width, int height) {
        if (chain != null) {
            chain.resize(width, height);
        }
    }

    private static void resizeTarget(RenderTarget target, int width, int height) {
        if (target != null) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
    }

    private static int safeTarget(int original, int factor) {
        long result = (long) original * (long) factor;
        if (result > Integer.MAX_VALUE) {
            throw new IllegalStateException("target dimension overflow: " + original + " * " + factor);
        }
        return Math.max(1, (int) result);
    }

    private static final class ResizeSnapshot {
        private final int originalWidth;
        private final int originalHeight;
        private final int targetWidth;
        private final int targetHeight;

        private ResizeSnapshot(int originalWidth, int originalHeight, int targetWidth, int targetHeight) {
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
        }
    }

    private MinimalHighResShell() {
    }
}