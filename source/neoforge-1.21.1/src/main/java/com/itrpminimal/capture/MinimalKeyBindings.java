package com.itrpminimal.capture;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class MinimalKeyBindings {
    private static final String CATEGORY = "key.categories.itrp_minimal_capture";
    private static final KeyMapping OPEN_MENU = new KeyMapping("key.itrp_minimal_capture.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    private static final KeyMapping FORCE_CANCEL = new KeyMapping("key.itrp_minimal_capture.force_cancel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    private static final KeyMapping RUN_PRESET = new KeyMapping("key.itrp_minimal_capture.run_preset", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    private static final KeyMapping PREVIEW_LATEST = new KeyMapping("key.itrp_minimal_capture.preview_latest", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
        event.register(FORCE_CANCEL);
        event.register(RUN_PRESET);
        event.register(PREVIEW_LATEST);
    }

    public static void tick(Minecraft client) {
        while (OPEN_MENU.consumeClick()) {
            client.setScreen(new MinimalCaptureScreen(client.screen));
        }
        while (FORCE_CANCEL.consumeClick()) {
            MinimalCaptureController.cancel("hotkey");
            MinimalOfflineRenderController.cancel("hotkey");
        }
        while (RUN_PRESET.consumeClick()) {
            MinimalCapturePreset.runSavedPreset();
        }
        while (PREVIEW_LATEST.consumeClick()) {
            MinimalScreenshotPreviewScreen.openLatestScreenshot(client, client.screen);
        }
    }

    private MinimalKeyBindings() {
    }
}
