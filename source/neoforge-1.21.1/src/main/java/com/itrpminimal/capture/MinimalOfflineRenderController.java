package com.itrpminimal.capture;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.CameraType;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class MinimalOfflineRenderController {
    private enum State {
        IDLE,
        PREPARE_SHADER,
        PHYSICS_WAIT,
        WARMUP,
        ACCUMULATE,
        WRITING,
        RESTORING
    }

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static State state = State.IDLE;
    private static int targetFrames = 240;
    private static int targetWarmup = 60;
    private static int targetPhysicsWait = 40;
    private static int frames = 0;
    private static int warmup = 0;
    private static int physicsWait = 0;
    private static boolean oldHideGui = false;
    private static CloudStatus oldCloudStatus = CloudStatus.FANCY;
    private static boolean changedCloudStatus = false;
    private static Screen oldScreen = null;
    private static CameraType oldCamera = null;
    private static boolean savedView = false;
    private static Path lastOutput = null;
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

    public static void cancel(String reason) {
        if (state == State.IDLE) {
            return;
        }
        MinimalLog.line("OFFLINE CANCEL " + reason);
        restore(Minecraft.getInstance(), "cancel " + reason);
        say(Minecraft.getInstance(), "离线渲染已取消: " + reason);
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
        MinimalHighResShell.ensureTargetStillApplied(client);
        if (oldCamera != null && client.options.getCameraType() != oldCamera) {
            client.options.setCameraType(oldCamera);
        }
        if (state == State.PHYSICS_WAIT) {
physicsWait++;
if (physicsWait == 1 || physicsWait == targetPhysicsWait || physicsWait % 20 == 0) {
MinimalLog.line("OFFLINE LOAD_WAIT " + physicsWait + "/" + targetPhysicsWait + " freeze=" + MinimalFreezeController.summary());
MinimalLog.state("offline load wait " + physicsWait);
}
if (physicsWait >= targetPhysicsWait) {
enterFrozenPhase(client);
}
return;
}
        if (state == State.WARMUP) {
            warmup++;
            if (warmup == 1 || warmup == targetWarmup || warmup % 20 == 0) {
                MinimalLog.line("OFFLINE WARMUP " + warmup + "/" + targetWarmup + " prepared=" + MinimalShaderManager.offlinePrepared());
                MinimalLog.state("offline warmup " + warmup);
            }
            if (warmup >= targetWarmup) {
                frames = 0;
                state = State.ACCUMULATE;
                MinimalLog.line("OFFLINE PHASE ACCUMULATE targetFrames=" + targetFrames);
            }
            return;
        }
        if (state == State.ACCUMULATE) {
            frames++;
            if (frames == 1 || frames == 2 || frames == 3 || frames == 5 || frames == 10 || frames % 30 == 0 || frames >= targetFrames) {
                MinimalLog.line("OFFLINE ACCUMULATE " + frames + "/" + targetFrames + " " + progress(frames, targetFrames));
                if (frames <= 10 || frames % 120 == 0 || frames >= targetFrames) {
                    MinimalLog.state("offline accumulate " + frames);
                }
            }
            if (frames >= targetFrames) {
                writeScreenshot(client);
            }
        }
    }

    public static boolean isActive() {
        return state != State.IDLE;
    }

    public static int targetFrames() {
        return targetFrames;
    }

    public static int targetWarmup() {
        return targetWarmup;
    }

    public static int targetPhysicsWait() {
        return targetPhysicsWait;
    }

    public static void setTargetFrames(int value) {
        targetFrames = clamp(value, 1, 12000);
        MinimalLog.line("OFFLINE CONFIG frames=" + targetFrames);
    }

    public static void setTargetWarmup(int value) {
        targetWarmup = clamp(value, 0, 2400);
        MinimalLog.line("OFFLINE CONFIG warmup=" + targetWarmup);
    }

    public static void setTargetPhysicsWait(int value) {
        targetPhysicsWait = clamp(value, 0, 2400);
        MinimalLog.line("OFFLINE CONFIG mapLoadWait=" + targetPhysicsWait);
    }

    public static Path lastOutput() {
        return lastOutput;
    }

    public static String lastOutputName() {
        return lastOutput == null ? "无" : lastOutput.getFileName().toString();
    }

    public static void openLastOutput() {
        Minecraft client = Minecraft.getInstance();
        Path output = lastOutput;
        if (output == null || !Files.exists(output)) {
            say(client, "没有最近截图");
            return;
        }
        net.minecraft.Util.getPlatform().openFile(output.toFile());
        say(client, "已请求打开最近截图: " + output.getFileName());
    }

    public static String statusSummary() {
        Minecraft client = Minecraft.getInstance();
        return "offlineActive=" + isActive() + " state=" + state + " frames=" + targetFrames + " warmup=" + targetWarmup + " mapLoadWait=" + targetPhysicsWait + " restoreShaderAfterCapture=" + MinimalCapturePreset.restoreShaderAfterCaptureEnabled() + " highres=" + MinimalHighResShell.summary(client) + " shader=" + MinimalShaderManager.currentSummary(client);
    }

    private static void begin(Minecraft client) {
        if (state != State.IDLE) {
            say(client, "已有离线渲染任务正在运行");
            return;
        }
        if (MinimalCaptureController.isActive()) {
            say(client, "普通截图正在运行，先取消或等待完成");
            return;
        }
        if (client.level == null || client.player == null) {
            say(client, "请先进入世界");
            return;
        }
        if (!MinimalShaderManager.isSelectedIterationRp()) {
            say(client, "目前仅支持iterationRP光影开启离线渲染。");
            MinimalLog.line("OFFLINE blocked unsupported shader selected=" + MinimalShaderManager.selectedPack());
            return;
        }
        try {
            state = State.PREPARE_SHADER;
            oldHideGui = client.options.hideGui;
            oldCloudStatus = client.options.cloudStatus().get();
            changedCloudStatus = false;
            oldScreen = client.screen;
            oldCamera = client.options.getCameraType();
            saveView(client);
            frames = 0;
            warmup = 0;
            physicsWait = 0;
            MinimalLog.line("========== OFFLINE RENDER BEGIN ==========");
            MinimalLog.line("OFFLINE BEGIN frames=" + targetFrames + " warmup=" + targetWarmup + " mapLoadWait=" + targetPhysicsWait + " selectedShader=" + MinimalShaderManager.selectedPack());
            MinimalLog.state("offline before prepare");
            MinimalShaderManager.prepareOfflineSelected(client, Math.max(1, targetPhysicsWait / 20));
            MinimalHighResShell.applyIfNeeded(client);
            client.setScreen(null);
            client.options.hideGui = true;
            if (oldCloudStatus != CloudStatus.OFF) {
                client.options.cloudStatus().set(CloudStatus.OFF);
                changedCloudStatus = true;
            }
            MinimalFreezeController.setCelestialPauseEnabled(MinimalCapturePreset.celestialPauseEnabled());
            if (targetPhysicsWait > 0) {
                state = State.PHYSICS_WAIT;
                MinimalLog.line("OFFLINE PHASE LOAD_WAIT ticks=" + targetPhysicsWait + " seconds=" + (targetPhysicsWait / 20.0F) + " freeze=false");
            } else {
                enterFrozenPhase(client);
            }
            MinimalLog.state("offline after setup state=" + state);
            say(client, "离线渲染开始: " + targetFrames + "帧，地图加载" + (targetPhysicsWait / 20) + "秒，正式预热" + targetWarmup + "帧");
        } catch (Throwable t) {
            MinimalLog.line("OFFLINE BEGIN FAILED " + t.getClass().getName() + ": " + t.getMessage());
            say(client, "离线渲染启动失败: " + t.getMessage());
            restore(client, "begin failed");
        }
    }

    private static void enterFrozenPhase(Minecraft client) {
        warmup = 0;
        frames = 0;
        MinimalFreezeController.freeze("minimal offline render");
        state = targetWarmup > 0 ? State.WARMUP : State.ACCUMULATE;
        MinimalLog.line("OFFLINE PHASE " + state + " warmup=" + targetWarmup + " frames=" + targetFrames + " freeze=true");
        MinimalLog.state("offline frozen phase state=" + state);
    }

    private static void writeScreenshot(Minecraft client) {
        state = State.WRITING;
        try {
            Path dir = client.gameDirectory.toPath().resolve("screenshots").resolve("itrp_minimal_offline");
            Files.createDirectories(dir);
            Path out = dir.resolve("itrp_offline_" + targetFrames + "f_" + MinimalHighResShell.multiplier() + "x_" + FILE_TIME.format(LocalDateTime.now()) + ".png");
            RenderTarget target = client.getMainRenderTarget();
            MinimalLog.line("OFFLINE WRITE output=" + out + " target=" + target.width + "x" + target.height + " view=" + target.viewWidth + "x" + target.viewHeight);
            NativeImage image = Screenshot.takeScreenshot(target);
            try {
                MinimalLog.line("OFFLINE IMAGE width=" + image.getWidth() + " height=" + image.getHeight() + " frames=" + frames);
                image.writeToFile(out);
                lastOutput = out;
                MinimalLog.line("OFFLINE DONE output=" + out);
                say(client, "离线渲染完成: " + out.getFileName());
            } finally {
                image.close();
            }
        } catch (Throwable t) {
            MinimalLog.line("OFFLINE WRITE FAILED " + t.getClass().getName() + ": " + t.getMessage());
            say(client, "离线渲染写出失败: " + t.getMessage());
        } finally {
            restore(client, "finish");
        }
    }

    private static void restore(Minecraft client, String reason) {
        state = State.RESTORING;
        try {
            if (client != null) {
                client.options.hideGui = oldHideGui;
                if (changedCloudStatus) {
                    client.options.cloudStatus().set(oldCloudStatus);
                    changedCloudStatus = false;
                }
                if (oldCamera != null) {
                    client.options.setCameraType(oldCamera);
                }
                restoreView(client);
                MinimalHighResShell.restoreIfNeeded(client);
                MinimalShaderManager.restoreOffline(client, MinimalCapturePreset.restoreShaderAfterCaptureEnabled());
                if (oldScreen != null) {
                    client.setScreen(oldScreen);
                    oldScreen.resize(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                }
            }
        } catch (Throwable t) {
            MinimalLog.line("OFFLINE RESTORE STATE FAILED " + t.getClass().getName() + ": " + t.getMessage());
        } finally {
            MinimalFreezeController.unfreeze("offline " + reason);
            oldScreen = null;
            oldCamera = null;
            savedView = false;
            state = State.IDLE;
            MinimalLog.state("offline after restore reason=" + reason);
            MinimalLog.line("========== OFFLINE RENDER END ==========");
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

    private static String progress(int current, int total) {
        int safeTotal = Math.max(1, total);
        int width = 20;
        int filled = Math.max(0, Math.min(width, current * width / safeTotal));
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            builder.append(i < filled ? '#' : '-');
        }
        builder.append("] ").append(current).append('/').append(total);
        return builder.toString();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void say(Minecraft client, String text) {
        if (client != null && client.gui != null) {
            client.gui.getChat().addMessage(Component.literal("截图管理器: " + text));
        }
    }

    private MinimalOfflineRenderController() {
    }
}
