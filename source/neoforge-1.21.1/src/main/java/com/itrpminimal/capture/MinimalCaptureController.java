package com.itrpminimal.capture;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class MinimalCaptureController {
    private enum State {
        IDLE,
        WARMUP,
        WAIT_FRAMES,
        WRITING,
        RESTORING
    }

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static State state = State.IDLE;
    private static int targetFrames = 60;
    private static int targetWarmup = 20;
    private static int frames = 0;
    private static int warmup = 0;
    private static boolean oldHideGui = false;
    private static Screen oldScreen = null;
    private static CameraType oldCamera = null;
    private static boolean savedView = false;
    private static float oldXRot = 0.0F;
    private static float oldYRot = 0.0F;
    private static float oldYHeadRot = 0.0F;
    private static float oldYHeadRotO = 0.0F;
    private static float oldYBodyRot = 0.0F;
    private static float oldYBodyRotO = 0.0F;

    public static void start() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> begin(client));
    }

    public static int targetFrames() {
        return targetFrames;
    }

    public static int targetWarmup() {
        return targetWarmup;
    }

    public static void setTargetFrames(int value) {
        targetFrames = clamp(value, 1, 6000);
        MinimalLog.line("CONFIG frames=" + targetFrames);
    }

    public static void setTargetWarmup(int value) {
        targetWarmup = clamp(value, 0, 1200);
        MinimalLog.line("CONFIG warmup=" + targetWarmup);
    }

    public static String statusSummary() {
        return "active=" + isActive() + " frames=" + targetFrames + " warmup=" + targetWarmup + " freeze=" + MinimalFreezeController.summary();
    }

    public static void reportStatus() {
        Minecraft client = Minecraft.getInstance();
        MinimalLog.state("gui status");
        say(client, statusSummary() + " log=" + MinimalLog.lastPath());
    }

    public static void tick(Minecraft client) {
        if (state == State.IDLE) {
            return;
        }
        if (client.level == null || client.player == null) {
            cancel("world closed");
            return;
        }
        lockView(client);
        if (state == State.WARMUP) {
            warmup++;
            if (warmup == 1 || warmup == targetWarmup || warmup % 10 == 0) {
                MinimalLog.line("WARMUP " + warmup + "/" + targetWarmup + " " + MinimalFreezeController.summary());
                MinimalLog.state("warmup " + warmup);
            }
            if (warmup >= targetWarmup) {
                frames = 0;
                state = State.WAIT_FRAMES;
                MinimalLog.line("PHASE WAIT_FRAMES targetFrames=" + targetFrames);
            }
            return;
        }
        if (state == State.WAIT_FRAMES) {
            frames++;
            if (frames == 1 || frames == targetFrames || frames % 10 == 0) {
                MinimalLog.line("WAIT_FRAMES " + frames + "/" + targetFrames + " " + MinimalFreezeController.summary());
                MinimalLog.state("wait frames " + frames);
            }
            if (frames >= targetFrames) {
                writeScreenshot(client);
            }
        }
    }

    public static void cancel(String reason) {
        if (state == State.IDLE) {
            return;
        }
        MinimalLog.line("CANCEL " + reason);
        restore(Minecraft.getInstance(), "cancel");
        say(Minecraft.getInstance(), "minimal capture cancelled: " + reason);
    }

    public static boolean isActive() {
        return state != State.IDLE;
    }

    private static void begin(Minecraft client) {
        if (state != State.IDLE) {
            say(client, "minimal capture is already active");
            return;
        }
        if (client.level == null || client.player == null) {
            say(client, "enter a world first");
            return;
        }
        try {
            oldHideGui = client.options.hideGui;
            oldScreen = client.screen;
            oldCamera = client.options.getCameraType();
            saveView(client);
            frames = 0;
            warmup = 0;
            MinimalLog.line("========== MINIMAL CAPTURE BEGIN ==========");
            MinimalLog.line("PHASE BEGIN frames=" + targetFrames + " warmup=" + targetWarmup + " freeze=true");
            MinimalLog.state("before setup");
            MinimalHighResShell.applyIfNeeded(client);
            client.setScreen(null);
            client.options.hideGui = true;
            MinimalFreezeController.freeze("minimal capture");
            state = targetWarmup > 0 ? State.WARMUP : State.WAIT_FRAMES;
            MinimalLog.state("after setup state=" + state);
            say(client, "minimal capture started: " + targetFrames + " frames");
        } catch (Throwable t) {
            MinimalLog.line("BEGIN FAILED " + t.getClass().getName() + ": " + t.getMessage());
            restore(client, "begin failed");
            say(client, "minimal capture failed to start: " + t.getMessage());
        }
    }

    private static void writeScreenshot(Minecraft client) {
        state = State.WRITING;
        try {
            Path dir = client.gameDirectory.toPath().resolve("screenshots").resolve("itrp_minimal_capture");
            Files.createDirectories(dir);
            Path out = dir.resolve("itrp_minimal_" + FILE_TIME.format(LocalDateTime.now()) + ".png");
            RenderTarget target = client.getMainRenderTarget();
            MinimalLog.line("PHASE WRITE output=" + out + " target=" + target.width + "x" + target.height + " view=" + target.viewWidth + "x" + target.viewHeight);
            NativeImage image = Screenshot.takeScreenshot(target);
            try {
                MinimalLog.line("IMAGE width=" + image.getWidth() + " height=" + image.getHeight() + " frames=" + frames);
                image.writeToFile(out);
                MinimalLog.line("DONE output=" + out);
                say(client, "minimal capture done: " + out.getFileName());
            } finally {
                image.close();
            }
        } catch (Throwable t) {
            MinimalLog.line("WRITE FAILED " + t.getClass().getName() + ": " + t.getMessage());
            say(client, "minimal capture write failed: " + t.getMessage());
        } finally {
            restore(client, "finish");
        }
    }

    private static void restore(Minecraft client, String reason) {
        state = State.RESTORING;
        try {
            if (client != null) {
                client.options.hideGui = oldHideGui;
                if (oldCamera != null) {
                    client.options.setCameraType(oldCamera);
                }
                restoreView(client);
                MinimalHighResShell.restoreIfNeeded(client);
                if (oldScreen != null) {
                    client.setScreen(oldScreen);
                    oldScreen.resize(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                }
            }
        } catch (Throwable t) {
            MinimalLog.line("RESTORE STATE FAILED " + t.getClass().getName() + ": " + t.getMessage());
        } finally {
            MinimalFreezeController.unfreeze(reason);
            oldScreen = null;
            oldCamera = null;
            savedView = false;
            state = State.IDLE;
            MinimalLog.state("after restore reason=" + reason);
            MinimalLog.line("========== MINIMAL CAPTURE END ==========");
        }
    }

    private static void saveView(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            savedView = false;
            return;
        }
        oldXRot = player.getXRot();
        oldYRot = player.getYRot();
        oldYHeadRot = player.yHeadRot;
        oldYHeadRotO = player.yHeadRotO;
        oldYBodyRot = player.yBodyRot;
        oldYBodyRotO = player.yBodyRotO;
        savedView = true;
    }

    private static void lockView(Minecraft client) {
        if (!savedView || client.player == null) {
            return;
        }
        LocalPlayer player = client.player;
        player.setXRot(oldXRot);
        player.setYRot(oldYRot);
        player.yHeadRot = oldYHeadRot;
        player.yHeadRotO = oldYHeadRotO;
        player.yBodyRot = oldYBodyRot;
        player.yBodyRotO = oldYBodyRotO;
    }

    private static void restoreView(Minecraft client) {
        lockView(client);
        savedView = false;
    }

    private static void say(Minecraft client, String text) {
        if (client != null && client.gui != null) {
            client.gui.getChat().addMessage(Component.literal("截图管理器: " + text));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private MinimalCaptureController() {
    }
}
