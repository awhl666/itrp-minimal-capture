package com.itrpminimal.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MinimalMoreSettingsScreen extends Screen {
    private static final int H = 20;
    private static final int GAP = 6;
    private final Screen parent;
    private String status = "只保留小补丁设置";

    public MinimalMoreSettingsScreen(Screen parent) {
        super(Component.literal("更多设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        int w = panelWidth();
        int left = this.width / 2 - w / 2;
        int y = 78;
        int half = (w - GAP) / 2;

        addRenderableWidget(Button.builder(Component.literal("太阳行星暂停: " + (MinimalCapturePreset.celestialPauseEnabled() ? "开" : "关")), b -> toggleCelestialPause())
                .bounds(left, y, w, H)
                .build());
        y += H + GAP;

        addRenderableWidget(Button.builder(Component.literal("结束后恢复光影: " + (MinimalCapturePreset.restoreShaderAfterCaptureEnabled() ? "开" : "关")), b -> toggleRestoreShader())
                .bounds(left, y, w, H)
                .build());
        y += H + GAP;

        addRenderableWidget(Button.builder(Component.literal("写入状态日志"), b -> writeStatus())
                .bounds(left, y, half, H)
                .build());
        addRenderableWidget(Button.builder(Component.literal("返回"), b -> Minecraft.getInstance().setScreen(parent))
                .bounds(left + half + GAP, y, half, H)
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int w = panelWidth();
        int left = this.width / 2 - w / 2;
        int y = 78;
        g.drawCenteredString(this.font, this.title, this.width / 2, 24, 0xFFFFFF);
        g.drawString(this.font, Component.literal("小补丁"), left, y - 13, 0xFFE8F2FF, false);
        g.drawCenteredString(this.font, Component.literal(status), this.width / 2, Math.min(this.height - 28, y + H * 4 + GAP * 4), 0x55FF55);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private void writeStatus() {
        MinimalLog.line("MORE SETTINGS STATUS capture=" + MinimalCaptureController.statusSummary());
        MinimalLog.line("MORE SETTINGS STATUS offline=" + MinimalOfflineRenderController.statusSummary());
        MinimalLog.line("MORE SETTINGS STATUS celestialPauseEnabled=" + MinimalCapturePreset.celestialPauseEnabled() + " restoreShaderAfterCaptureEnabled=" + MinimalCapturePreset.restoreShaderAfterCaptureEnabled() + " freeze=" + MinimalFreezeController.summary());
        MinimalLog.state("more settings status");
        status = "状态已写入日志";
        rebuildWidgets();
    }

    private void toggleCelestialPause() {
        boolean enabled = !MinimalCapturePreset.celestialPauseEnabled();
        MinimalCapturePreset.setCelestialPauseEnabled(enabled);
        MinimalCapturePreset.save(MinimalCapturePreset.offlineEnabled());
        status = "太阳行星暂停已" + (enabled ? "开启" : "关闭");
        rebuildWidgets();
    }

    private void toggleRestoreShader() {
        boolean enabled = !MinimalCapturePreset.restoreShaderAfterCaptureEnabled();
        MinimalCapturePreset.setRestoreShaderAfterCaptureEnabled(enabled);
        MinimalCapturePreset.save(MinimalCapturePreset.offlineEnabled());
        status = enabled ? "结束后将恢复截图前光影" : "结束后将保持截图光影";
        rebuildWidgets();
    }

    private int panelWidth() {
        return Math.min(420, Math.max(260, this.width - 96));
    }
}