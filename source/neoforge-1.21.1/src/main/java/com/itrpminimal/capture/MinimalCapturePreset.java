package com.itrpminimal.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MinimalCapturePreset {
    private static final String FILE_NAME = "itrp_minimal_capture.properties";
    private static boolean loaded = false;
    private static boolean offlineEnabled = false;
    private static boolean celestialPauseEnabled = true;
    private static boolean restoreShaderAfterCaptureEnabled = true;

    public static void load() {
        Minecraft client = Minecraft.getInstance();
        Path file = configPath(client);
        if (!Files.isRegularFile(file)) {
            loaded = true;
            return;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
            offlineEnabled = Boolean.parseBoolean(properties.getProperty("offlineEnabled", "false"));
            celestialPauseEnabled = Boolean.parseBoolean(properties.getProperty("celestialPauseEnabled", properties.getProperty("starTrailEnabled", "true")));
            restoreShaderAfterCaptureEnabled = Boolean.parseBoolean(properties.getProperty("restoreShaderAfterCaptureEnabled", "true"));
            MinimalFreezeController.setCelestialPauseEnabled(celestialPauseEnabled);
            MinimalShaderManager.setSelectedPack(blankToNull(properties.getProperty("shaderPack", "")));
            MinimalHighResShell.setMultiplier(parseInt(properties, "multiplier", MinimalHighResShell.multiplier()));
            MinimalCaptureController.setTargetFrames(parseInt(properties, "normalFrames", MinimalCaptureController.targetFrames()));
            MinimalCaptureController.setTargetWarmup(parseInt(properties, "normalWarmup", MinimalCaptureController.targetWarmup()));
            MinimalOfflineRenderController.setTargetFrames(parseInt(properties, "offlineFrames", MinimalOfflineRenderController.targetFrames()));
            MinimalOfflineRenderController.setTargetWarmup(parseInt(properties, "offlineWarmup", MinimalOfflineRenderController.targetWarmup()));
            MinimalOfflineRenderController.setTargetPhysicsWait(parseInt(properties, "offlineWait", MinimalOfflineRenderController.targetPhysicsWait()));
            MinimalLog.line("PRESET loaded file=" + file);
        } catch (Throwable t) {
            MinimalLog.line("PRESET load failed " + t.getClass().getName() + ": " + t.getMessage());
        } finally {
            loaded = true;
        }
    }

    public static boolean offlineEnabled() {
        ensureLoaded();
        return offlineEnabled;
    }

    public static void setOfflineEnabled(boolean value) {
        offlineEnabled = value;
    }

    public static boolean celestialPauseEnabled() {
        ensureLoaded();
        return celestialPauseEnabled;
    }

    public static void setCelestialPauseEnabled(boolean value) {
        ensureLoaded();
        celestialPauseEnabled = value;
        MinimalFreezeController.setCelestialPauseEnabled(value);
    }

    public static boolean restoreShaderAfterCaptureEnabled() {
        ensureLoaded();
        return restoreShaderAfterCaptureEnabled;
    }

    public static void setRestoreShaderAfterCaptureEnabled(boolean value) {
        ensureLoaded();
        restoreShaderAfterCaptureEnabled = value;
    }

    public static void save(boolean offline) {
        ensureLoaded();
        offlineEnabled = offline;
        Minecraft client = Minecraft.getInstance();
        Path file = configPath(client);
        Properties properties = new Properties();
        properties.setProperty("offlineEnabled", Boolean.toString(offlineEnabled));
        properties.setProperty("celestialPauseEnabled", Boolean.toString(celestialPauseEnabled));
        properties.setProperty("restoreShaderAfterCaptureEnabled", Boolean.toString(restoreShaderAfterCaptureEnabled));
        properties.setProperty("shaderPack", safe(MinimalShaderManager.selectedPack()));
        properties.setProperty("multiplier", Integer.toString(MinimalHighResShell.multiplier()));
        properties.setProperty("normalFrames", Integer.toString(MinimalCaptureController.targetFrames()));
        properties.setProperty("normalWarmup", Integer.toString(MinimalCaptureController.targetWarmup()));
        properties.setProperty("offlineFrames", Integer.toString(MinimalOfflineRenderController.targetFrames()));
        properties.setProperty("offlineWarmup", Integer.toString(MinimalOfflineRenderController.targetWarmup()));
        properties.setProperty("offlineWait", Integer.toString(MinimalOfflineRenderController.targetPhysicsWait()));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                properties.store(out, "Screenshot manager preset");
            }
            say(client, "预设已保存");
            MinimalLog.line("PRESET saved file=" + file + " offline=" + offlineEnabled);
        } catch (IOException e) {
            say(client, "保存预设失败: " + e.getClass().getSimpleName());
            MinimalLog.line("PRESET save failed " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static void runSavedPreset() {
        load();
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (MinimalCaptureController.isActive() || MinimalOfflineRenderController.isActive()) {
                say(client, "已有截图/渲染正在运行");
                return;
            }
            if (offlineEnabled) {
                MinimalOfflineRenderController.start();
            } else {
                MinimalCaptureController.start();
            }
        });
    }

    public static String summary() {
        ensureLoaded();
        return (offlineEnabled ? "ITRP离线 " + MinimalOfflineRenderController.targetFrames() + "FPS" : "普通截图 " + MinimalCaptureController.targetFrames() + "帧") + "  " + MinimalHighResShell.multiplier() + "x";
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static Path configPath(Minecraft client) {
        return client.gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }

    private static int parseInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void say(Minecraft client, String message) {
        if (client.gui != null) {
            client.gui.getChat().addMessage(Component.literal("ITRP: " + message));
        }
    }

    private MinimalCapturePreset() {
    }
}
