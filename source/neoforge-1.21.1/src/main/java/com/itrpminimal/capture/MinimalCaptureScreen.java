package com.itrpminimal.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MinimalCaptureScreen extends Screen {
    private static final int H = 20;
    private static final int GAP = 5;
    private final Screen parent;
    private boolean offlineEnabled;
    private String status = "就绪";
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private boolean draggingScrollBar = false;
    private int dragStartMouseY = 0;
    private int dragStartScrollOffset = 0;

    public MinimalCaptureScreen(Screen parent) {
        super(Component.literal("截图管理器"));
        this.parent = parent;
        this.offlineEnabled = MinimalCapturePreset.offlineEnabled();
    }

    public Screen returnParent() {
        return this.parent;
    }

    @Override
    protected void init() {
        clampScroll();
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        int w = panelWidth();
        int left = this.width / 2 - w / 2;
        int y = contentTop() - scrollOffset;
        int label = labelWidth();
        int labelX = left + label;
        int valueW = Math.max(120, w - label);
        int q = Math.max(34, (valueW - GAP * 3) / 4);
        int half = (w - GAP) / 2;
        int third = Math.max(36, (valueW - GAP * 2) / 3);

        addButton("截图光影: " + trim(displayPack(), smallLayout() ? 22 : 36), left, y, w, H, () -> Minecraft.getInstance().setScreen(new MinimalShaderSelectScreen(this)));
        y += H + 11;

        addButton("1x", labelX, y, q, H, () -> setMultiplier(1));
        addButton("2x", labelX + q + GAP, y, q, H, () -> setMultiplier(2));
        addButton("3x", labelX + (q + GAP) * 2, y, q, H, () -> setMultiplier(3));
        addButton("4x", labelX + (q + GAP) * 3, y, q, H, () -> setMultiplier(4));
        y += H + 11;

        if (!offlineEnabled) {
            addButton("-60", labelX, y, q, H, () -> adjustNormalFrames(-60));
            addButton("-10", labelX + q + GAP, y, q, H, () -> adjustNormalFrames(-10));
            addButton("+10", labelX + (q + GAP) * 2, y, q, H, () -> adjustNormalFrames(10));
            addButton("+60", labelX + (q + GAP) * 3, y, q, H, () -> adjustNormalFrames(60));
            y += H + GAP;

            addButton("-20", labelX, y, q, H, () -> adjustNormalWarmup(-20));
            addButton("-5", labelX + q + GAP, y, q, H, () -> adjustNormalWarmup(-5));
            addButton("+5", labelX + (q + GAP) * 2, y, q, H, () -> adjustNormalWarmup(5));
            addButton("+20", labelX + (q + GAP) * 3, y, q, H, () -> adjustNormalWarmup(20));
            y += H + 13;
        }

        addButton(offlineEnabled ? "ITRP离线渲染: 开" : "ITRP离线渲染: 关", left, y, w, H, this::toggleOffline);
        y += H + GAP;

        if (offlineEnabled) {
            addButton("-1", left + w - 145, y, 70, H, () -> adjustWait(-20));
            addButton("+1", left + w - 70, y, 70, H, () -> adjustWait(20));
            y += H + GAP;

            addButton("-60", labelX, y, q, H, () -> adjustOfflineWarmup(-60));
            addButton("-10", labelX + q + GAP, y, q, H, () -> adjustOfflineWarmup(-10));
            addButton("+10", labelX + (q + GAP) * 2, y, q, H, () -> adjustOfflineWarmup(10));
            addButton("+60", labelX + (q + GAP) * 3, y, q, H, () -> adjustOfflineWarmup(60));
            y += H + GAP;

            addButton("-120", labelX, y, q, H, () -> adjustOfflineFrames(-120));
            addButton("-10", labelX + q + GAP, y, q, H, () -> adjustOfflineFrames(-10));
            addButton("+10", labelX + (q + GAP) * 2, y, q, H, () -> adjustOfflineFrames(10));
            addButton("+120", labelX + (q + GAP) * 3, y, q, H, () -> adjustOfflineFrames(120));
            y += H + GAP;

            addButton("480", labelX, y, third, H, () -> setOfflineFrames(480));
            addButton("960", labelX + third + GAP, y, third, H, () -> setOfflineFrames(960));
            addButton("2040", labelX + (third + GAP) * 2, y, third, H, () -> setOfflineFrames(2040));
            y += H + 12;
        }

        addButton("截图预览", left, y, half, H, () -> Minecraft.getInstance().setScreen(new MinimalScreenshotPreviewScreen(this)));
        addButton(smallLayout() ? "最新" : "立即预览最新", left + half + GAP, y, half, H, () -> MinimalScreenshotPreviewScreen.openLatestScreenshot(Minecraft.getInstance(), this));
        y += H + GAP;
        if (maxScroll() > 0) {
            addButton("更多设置", left, y, half, H, () -> Minecraft.getInstance().setScreen(new MinimalMoreSettingsScreen(this)));
            addButton(scrollButtonText(), left + half + GAP, y, half, H, this::pageScrollDown);
        } else {
            addButton("更多设置", left, y, w, H, () -> Minecraft.getInstance().setScreen(new MinimalMoreSettingsScreen(this)));
        }
        y += H + GAP;
        addButton("取消运行", left, y, w, H, this::cancelRunning);
        y += H + 12;

        int bw = (w - GAP * 2) / 3;
        addButton("返回", left, y, bw, H, () -> Minecraft.getInstance().setScreen(parent));
        addButton("保存预设", left + bw + GAP, y, bw, H, this::savePreset);
        addButton("运行预设", left + (bw + GAP) * 2, y, bw, H, this::runPreset);
        y += H + 12;

        contentHeight = y + scrollOffset - contentTop();
        clampScroll();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int w = panelWidth();
        int left = this.width / 2 - w / 2;
        int right = left + w;
        int y = contentTop() - scrollOffset;

        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.literal(trim(summaryLine() + "  " + status, smallLayout() ? 54 : 96)), this.width / 2, 31, 0x55FF55);
        drawVisibleLabel(g, left, y - 14, "基础截图");
        drawVisibleLabel(g, left, y, "光影包");
        y += H + 11;
        drawVisibleLabel(g, left, y, "倍率: " + MinimalHighResShell.multiplier() + "x");
        y += H + 11;

        if (!offlineEnabled) {
            drawVisibleLabel(g, left, y, "截图帧: " + MinimalCaptureController.targetFrames());
            y += H + GAP;
            drawVisibleLabel(g, left, y, "预热帧: " + MinimalCaptureController.targetWarmup());
            y += H + 13;
        }

        drawVisibleLabel(g, left, y - 14, "离线渲染");
        y += H + GAP;
        if (offlineEnabled) {
            drawVisibleLabel(g, left, y, "地图加载: " + (MinimalOfflineRenderController.targetPhysicsWait() / 20) + "秒");
            y += H + GAP;
            drawVisibleLabel(g, left, y, "正式预热: " + MinimalOfflineRenderController.targetWarmup() + "帧");
            y += H + GAP;
            drawVisibleLabel(g, left, y, "累积FPS: " + MinimalOfflineRenderController.targetFrames());
            y += H + GAP;
            drawVisibleLabel(g, left, y, "FPS预设");
        }

        if (maxScroll() > 0) {
            int top = viewportTop();
            int bottom = viewportBottom();
            int barX = Math.min(right + 5, this.width - 5);
            int trackH = Math.max(10, bottom - top);
            int thumbH = Math.max(12, trackH * Math.max(1, visibleHeight()) / Math.max(1, contentHeight));
            int thumbY = top + (trackH - thumbH) * scrollOffset / Math.max(1, maxScroll());
            g.fill(barX, top, barX + 3, bottom, 0x66333333);
            g.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xCCFFFFFF);
        }

        if (maxScroll() > 0) {
            g.drawString(this.font, Component.literal(scrollOffset + "/" + maxScroll()), Math.max(4, right - 44), Math.max(4, this.height - 13), 0xCCCCCC, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int step = Math.max(12, H + GAP);
        scrollOffset -= (int) Math.round(scrollY * step);
        clampScroll();
        rebuildWidgets();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll() > 0 && isOnScrollBar(mouseX, mouseY)) {
            draggingScrollBar = true;
            dragStartMouseY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
            setScrollFromMouse(mouseY);
            rebuildWidgets();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollBar && button == 0 && maxScroll() > 0) {
            int trackH = Math.max(1, viewportBottom() - viewportTop());
            int delta = (int) Math.round((mouseY - dragStartMouseY) * maxScroll() / (double) trackH);
            scrollOffset = dragStartScrollOffset + delta;
            clampScroll();
            rebuildWidgets();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollBar) {
            draggingScrollBar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void addButton(String text, int x, int y, int w, int h, Runnable action) {
        Button button = Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, w, h).build();
        button.visible = y + h >= viewportTop() && y <= viewportBottom();
        addRenderableWidget(button);
    }

    private boolean isOnScrollBar(double mouseX, double mouseY) {
        int x = scrollBarX();
        return mouseX >= x - 8 && mouseX <= x + 12 && mouseY >= viewportTop() && mouseY <= viewportBottom();
    }

    private int scrollBarX() {
        return Math.min(this.width - 8, this.width / 2 + panelWidth() / 2 + 5);
    }

    private void setScrollFromMouse(double mouseY) {
        int top = viewportTop();
        int bottom = viewportBottom();
        int trackH = Math.max(1, bottom - top);
        double ratio = (mouseY - top) / (double) trackH;
        scrollOffset = (int) Math.round(ratio * maxScroll());
        clampScroll();
    }

    private String scrollButtonText() {
        if (maxScroll() <= 0) {
            return "已适配";
        }
        return scrollOffset >= maxScroll() ? "滚到顶部" : "向下滚动";
    }

    private void pageScrollDown() {
        if (maxScroll() <= 0) {
            status = "当前无需滚动";
        } else if (scrollOffset >= maxScroll()) {
            scrollOffset = 0;
            status = "已回到顶部";
        } else {
            scrollOffset += Math.max(40, visibleHeight() - H);
            clampScroll();
            status = "已向下滚动";
        }
        rebuildWidgets();
    }

    private void drawVisibleLabel(GuiGraphics g, int left, int y, String text) {
        if (y + H < viewportTop() || y > viewportBottom()) {
            return;
        }
        g.drawString(this.font, Component.literal(text), left + 10, y + 6, 0xFFFFFF, true);
    }

    private String summaryLine() {
        if (offlineEnabled) {
            return "ITRP离线 " + MinimalOfflineRenderController.targetFrames() + "FPS  " + MinimalHighResShell.multiplier() + "x  地图加载" + (MinimalOfflineRenderController.targetPhysicsWait() / 20) + "秒  正式预热" + MinimalOfflineRenderController.targetWarmup() + "帧";
        }
        return "普通截图  " + MinimalCaptureController.targetFrames() + "帧  " + MinimalHighResShell.multiplier() + "x  预热" + MinimalCaptureController.targetWarmup() + "帧";
    }

    private void toggleOffline() {
        offlineEnabled = !offlineEnabled;
        MinimalCapturePreset.setOfflineEnabled(offlineEnabled);
        status = offlineEnabled ? "已展开离线渲染设置" : "已切回普通截图";
        if (offlineEnabled && !MinimalShaderManager.isSelectedIterationRp()) {
            say("目前仅支持iterationRP光影开启离线渲染。");
        }
        clampScroll();
        rebuildWidgets();
    }

    private void cancelRunning() {
        MinimalCaptureController.cancel("gui");
        MinimalOfflineRenderController.cancel("gui");
        status = "已请求取消";
        rebuildWidgets();
    }

    private void savePreset() {
        MinimalCapturePreset.save(offlineEnabled);
        status = "预设已保存";
        rebuildWidgets();
    }

    private void runPreset() {
        MinimalCapturePreset.save(offlineEnabled);
        Minecraft.getInstance().setScreen(null);
        MinimalCapturePreset.runSavedPreset();
    }

    private void setMultiplier(int value) {
        MinimalHighResShell.setMultiplier(value);
        status = "倍率=" + value + "x";
        rebuildWidgets();
    }

    private void adjustNormalFrames(int delta) {
        MinimalCaptureController.setTargetFrames(MinimalCaptureController.targetFrames() + delta);
        rebuildWidgets();
    }

    private void adjustNormalWarmup(int delta) {
        MinimalCaptureController.setTargetWarmup(MinimalCaptureController.targetWarmup() + delta);
        rebuildWidgets();
    }

    private void adjustOfflineFrames(int delta) {
        MinimalOfflineRenderController.setTargetFrames(MinimalOfflineRenderController.targetFrames() + delta);
        rebuildWidgets();
    }

    private void setOfflineFrames(int value) {
        MinimalOfflineRenderController.setTargetFrames(value);
        rebuildWidgets();
    }

    private void adjustOfflineWarmup(int delta) {
        MinimalOfflineRenderController.setTargetWarmup(MinimalOfflineRenderController.targetWarmup() + delta);
        rebuildWidgets();
    }

    private void adjustWait(int deltaTicks) {
        MinimalOfflineRenderController.setTargetPhysicsWait(MinimalOfflineRenderController.targetPhysicsWait() + deltaTicks);
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private int panelWidth() {
        return Math.min(520, Math.max(280, this.width - 40));
    }

    private int labelWidth() {
        return smallLayout() ? 86 : 122;
    }

    private boolean smallLayout() {
        return this.width < 420;
    }

    private int contentTop() {
        return 58;
    }

    private int viewportTop() {
        return 43;
    }

    private int viewportBottom() {
        return Math.max(viewportTop() + 40, this.height - 8);
    }

    private int visibleHeight() {
        return Math.max(1, viewportBottom() - contentTop());
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - visibleHeight());
    }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll()));
    }

    private static void say(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null) {
            client.gui.getChat().addMessage(Component.literal("截图管理器: " + message));
        }
    }

    private static String displayPack() {
        String pack = MinimalShaderManager.selectedPack();
        return pack == null || pack.isBlank() ? "未选择" : pack;
    }

    private static String trim(String text, int max) {
        return text == null ? "" : text.length() <= max ? text : text.substring(0, Math.max(0, max - 3)) + "...";
    }
}