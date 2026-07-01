package com.itrpminimal.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class MinimalShaderSelectScreen extends Screen {
    private static final int ITEM_HEIGHT = 22;
    private static final int GAP = 3;
    private final Screen parent;
    private List<String> packs;
    private int scrollOffset;
    private String status = "选择一个光影包";

    public MinimalShaderSelectScreen(Screen parent) {
        super(Component.literal("选择光影包"));
        this.parent = parent;
        this.packs = MinimalShaderManager.listShaderPacks(Minecraft.getInstance());
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        int buttonWidth = Math.min(420, Math.max(180, this.width - 24));
        int left = this.width / 2 - buttonWidth / 2;
        int y = 34;
        int bottom = this.height - 54;
        int visibleCount = Math.max(1, (bottom - y) / (ITEM_HEIGHT + GAP));

        if (packs.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("shaderpacks 文件夹为空，点击刷新"), b -> refresh()).bounds(left, y, buttonWidth, ITEM_HEIGHT).build());
        } else {
            int end = Math.min(packs.size(), scrollOffset + visibleCount);
            for (int i = scrollOffset; i < end; i++) {
                String pack = packs.get(i);
                String label = pack.equals(MinimalShaderManager.selectedPack()) ? "已选: " + trim(pack, 36) : trim(pack, 40);
                int rowY = y + (i - scrollOffset) * (ITEM_HEIGHT + GAP);
                addRenderableWidget(Button.builder(Component.literal(label), b -> select(pack)).bounds(left, rowY, buttonWidth, ITEM_HEIGHT).build());
            }
        }

        int footerY = this.height - 44;
        int third = (buttonWidth - GAP * 2) / 3;
        addRenderableWidget(Button.builder(Component.literal("上翻"), b -> scroll(-1)).bounds(left, footerY, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("刷新"), b -> refresh()).bounds(left + third + GAP, footerY, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("下翻"), b -> scroll(1)).bounds(left + (third + GAP) * 2, footerY, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("返回"), b -> Minecraft.getInstance().setScreen(parent)).bounds(left, this.height - 22, buttonWidth, 20).build());
    }

    private void select(String pack) {
        MinimalShaderManager.setSelectedPack(pack);
        status = "已选择: " + pack;
        Minecraft.getInstance().setScreen(parent);
    }

    private void refresh() {
        packs = MinimalShaderManager.listShaderPacks(Minecraft.getInstance());
        scrollOffset = 0;
        status = "已刷新，共 " + packs.size() + " 个光影包";
        rebuildWidgets();
    }

    private void scroll(int direction) {
        if (packs.isEmpty()) {
            return;
        }
        int max = Math.max(0, packs.size() - 1);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset + direction * 6));
        rebuildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            scroll(-1);
            return true;
        }
        if (scrollY < 0) {
            scroll(1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("已选择: " + trim(String.valueOf(MinimalShaderManager.selectedPack()), 48)), this.width / 2, 22, 0xA8FFA8);
        graphics.drawCenteredString(this.font, Component.literal(status), this.width / 2, this.height - 66, 0xCCCCCC);
    }

    @Override
public boolean isPauseScreen() {
return true;
}

    private static String trim(String text, int max) {
        if (text == null || "null".equals(text)) {
            return "未选择";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }
}