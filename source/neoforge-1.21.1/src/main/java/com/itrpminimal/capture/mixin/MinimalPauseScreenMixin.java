package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalCaptureScreen;
import com.itrpminimal.capture.MinimalScreenshotPreviewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class MinimalPauseScreenMixin extends Screen {
    private static final int VANILLA_BUTTON_WIDTH = 204;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SMALL_ICON_WIDTH = 36;
    private static final int SMALL_TEXT_WIDTH = 44;

    protected MinimalPauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void itrpminimal$addPauseButtons(CallbackInfo ci) {
        PauseScreen self = (PauseScreen) (Object) this;
        int vanillaLeft = self.width / 2 - VANILLA_BUTTON_WIDTH / 2;
        int feedbackY = pauseMenuSmallButtonY(self.height);
        int optionsY = Math.min(self.height - BUTTON_HEIGHT - 4, feedbackY + 24);
        int sideX = Math.max(4, vanillaLeft - SMALL_TEXT_WIDTH - 8);
        addRenderableWidget(Button.builder(Component.literal("📷"), button -> Minecraft.getInstance().setScreen(new MinimalScreenshotPreviewScreen(self)))
                .bounds(sideX, feedbackY, SMALL_ICON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("截图"), button -> Minecraft.getInstance().setScreen(new MinimalCaptureScreen(self)))
                .bounds(sideX, optionsY, SMALL_TEXT_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private static int pauseMenuSmallButtonY(int screenHeight) {
        int vanillaStart = Math.max(8, screenHeight / 4 + 8);
        int y = vanillaStart + 24 * 2;
        return Math.max(8, Math.min(y, screenHeight - 52));
    }
}
