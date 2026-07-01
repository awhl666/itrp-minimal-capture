package com.itrpminimal.capture;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public final class MinimalLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("ITRP-Minimal-Capture");
    private static Path lastPath;

    public static Path lastPath() {
        return lastPath;
    }

    public static void line(String message) {
        LOGGER.info(message);
        try {
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("render_assistant_minimal");
            Files.createDirectories(dir);
            Path log = dir.resolve("minimal_capture.log");
            lastPath = log;
            Files.writeString(log, LocalDateTime.now() + "  " + message + System.lineSeparator(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    public static void state(String tag) {
        Minecraft client = Minecraft.getInstance();
        StringBuilder builder = new StringBuilder();
        builder.append("STATE ").append(tag);
        builder.append(" active=").append(MinimalCaptureController.isActive());
        builder.append(" frozen=").append(MinimalFreezeController.isFrozen());
        builder.append(" freeze=").append(MinimalFreezeController.summary());
        if (client.getWindow() != null) {
            builder.append(" window=").append(client.getWindow().getWidth()).append("x").append(client.getWindow().getHeight());
        }
        if (client.getMainRenderTarget() != null) {
            builder.append(" target=").append(client.getMainRenderTarget().width).append("x").append(client.getMainRenderTarget().height);
            builder.append(" view=").append(client.getMainRenderTarget().viewWidth).append("x").append(client.getMainRenderTarget().viewHeight);
        }
        builder.append(" screen=").append(client.screen == null ? "null" : client.screen.getClass().getName());
        if (client.player != null) {
            builder.append(" playerRot=").append(client.player.getXRot()).append(",").append(client.player.getYRot());
        }
        line(builder.toString());
    }

    private MinimalLog() {
    }
}