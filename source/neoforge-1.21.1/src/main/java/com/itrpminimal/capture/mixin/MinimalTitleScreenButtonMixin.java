package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalScreenshotPreviewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MinimalTitleScreenButtonMixin extends net.minecraft.client.gui.screens.Screen {
    private static final int VANILLA_BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SMALL_ICON_WIDTH = 28;

    protected MinimalTitleScreenButtonMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void itrpminimal$addScreenshotPreviewButton(CallbackInfo ci) {
        int vanillaLeft = this.width / 2 - VANILLA_BUTTON_WIDTH / 2;
        int x = Math.max(4, vanillaLeft - SMALL_ICON_WIDTH - 8);
        int y = titleMenuPreviewButtonY(this.height);
        addRenderableWidget(Button.builder(Component.literal("📷"), button -> Minecraft.getInstance().setScreen(new MinimalScreenshotPreviewScreen(this)))
                .bounds(x, y, SMALL_ICON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private static int titleMenuPreviewButtonY(int screenHeight) {
        int vanillaBase = screenHeight / 4 + 48;
        int y = vanillaBase + 24 * 3 - 22;
        return Math.max(32, Math.min(y, screenHeight - 28));
    }
}
