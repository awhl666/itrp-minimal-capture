package com.itrpminimal.capture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MinimalScreenshotGalleryScreen extends Screen {
    private static final int THUMB_W = 176;
    private static final int THUMB_H = 118;
    private static final int GAP = 10;
    private static final int TOP = 38;
    private static final int BOTTOM = 34;
    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();
    private int scroll = 0;
    private String status = "读取截图中";

    public MinimalScreenshotGalleryScreen(Screen parent) {
        super(Component.literal("ITRP 截图图库"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int buttonW = Math.min(140, Math.max(80, width / 4));
        addRenderableWidget(Button.builder(Component.literal("刷新"), b -> loadEntries()).bounds(width / 2 - buttonW - 4, height - 26, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("返回"), b -> Minecraft.getInstance().setScreen(parent)).bounds(width / 2 + 4, height - 26, buttonW, 20).build());
        loadEntries();
    }

    private void loadEntries() {
        closeTextures();
        entries.clear();
        Minecraft mc = Minecraft.getInstance();
        Path dir = screenshotDir(mc);
        try {
            Files.createDirectories(dir);
            List<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
                for (Path path : stream) {
                    files.add(path);
                }
            }
            files.sort(Comparator.comparingLong(this::lastModified).reversed());
            int limit = Math.min(files.size(), 60);
            for (int i = 0; i < limit; i++) {
                Path file = files.get(i);
                Entry entry = new Entry(file);
                loadThumbnail(mc, entry, i);
                entries.add(entry);
            }
            status = entries.isEmpty() ? "没有找到截图" : "已加载 " + entries.size() + " 张截图";
        } catch (Throwable t) {
            status = "读取失败: " + t.getClass().getSimpleName() + ": " + t.getMessage();
            MinimalLog.line("GALLERY load failed " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private void loadThumbnail(Minecraft mc, Entry entry, int index) {
        try (InputStream input = Files.newInputStream(entry.path)) {
            NativeImage image = NativeImage.read(input);
            NativeImage thumb = createThumbnail(image, THUMB_W, THUMB_H);
            image.close();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MinimalCaptureMod.MOD_ID, "gallery/" + System.nanoTime() + "_" + index);
            DynamicTexture texture = new DynamicTexture(thumb);
            mc.getTextureManager().register(id, texture);
            entry.textureId = id;
            entry.texture = texture;
            entry.width = thumb.getWidth();
            entry.height = thumb.getHeight();
        } catch (Throwable t) {
            MinimalLog.line("GALLERY thumbnail failed " + entry.path + " " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private NativeImage createThumbnail(NativeImage src, int targetW, int targetH) {
        NativeImage dst = new NativeImage(targetW, targetH, false);
        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                dst.setPixelRGBA(x, y, 0xFF101010);
            }
        }
        float scale = Math.min((float) targetW / Math.max(1, src.getWidth()), (float) targetH / Math.max(1, src.getHeight()));
        int drawW = Math.max(1, Math.min(targetW, Math.round(src.getWidth() * scale)));
        int drawH = Math.max(1, Math.min(targetH, Math.round(src.getHeight() * scale)));
        int offX = (targetW - drawW) / 2;
        int offY = (targetH - drawH) / 2;
        for (int y = 0; y < drawH; y++) {
            int sy = Math.min(src.getHeight() - 1, Math.max(0, (int) (y / scale)));
            for (int x = 0; x < drawW; x++) {
                int sx = Math.min(src.getWidth() - 1, Math.max(0, (int) (x / scale)));
                dst.setPixelRGBA(offX + x, offY + y, src.getPixelRGBA(sx, sy));
            }
        }
        return dst;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xF0101010);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(status), width / 2, 23, 0xCCCCCC);
        renderGrid(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int cols = Math.max(1, Math.min(4, (width - GAP) / (THUMB_W + GAP)));
        int gridW = cols * THUMB_W + (cols - 1) * GAP;
        int startX = (width - gridW) / 2;
        int visibleBottom = height - BOTTOM;
        int contentH = rows(cols) * (THUMB_H + GAP);
        int maxScroll = Math.max(0, contentH - Math.max(1, visibleBottom - TOP));
        scroll = clamp(scroll, 0, maxScroll);
        graphics.enableScissor(0, TOP, width, visibleBottom);
        for (int i = 0; i < entries.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (THUMB_W + GAP);
            int y = TOP + row * (THUMB_H + GAP) - scroll;
            if (y + THUMB_H < TOP || y > visibleBottom) {
                continue;
            }
            Entry entry = entries.get(i);
            boolean hover = mouseX >= x && mouseX < x + THUMB_W && mouseY >= y && mouseY < y + THUMB_H;
            graphics.fill(x - 1, y - 1, x + THUMB_W + 1, y + THUMB_H + 1, hover ? 0xFFFFFFFF : 0xFF555555);
            if (entry.textureId != null) {
                graphics.blit(entry.textureId, x, y, 0, 0, THUMB_W, THUMB_H, THUMB_W, THUMB_H);
            } else {
                graphics.fill(x, y, x + THUMB_W, y + THUMB_H, 0xFF202020);
                graphics.drawCenteredString(font, Component.literal("读取失败"), x + THUMB_W / 2, y + THUMB_H / 2 - 4, 0xFF7777);
            }
            String name = entry.path.getFileName().toString();
            if (name.length() > 20) {
                name = name.substring(0, 17) + "...";
            }
            graphics.fill(x, y + THUMB_H - 12, x + THUMB_W, y + THUMB_H, 0xAA000000);
            graphics.drawString(font, name, x + 3, y + THUMB_H - 10, 0xFFFFFF, false);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Entry entry = entryAt(mouseX, mouseY);
            if (entry != null) {
                net.minecraft.Util.getPlatform().openFile(entry.path.toFile());
                status = "已请求打开: " + entry.path.getFileName();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scroll = Math.max(0, scroll - (int) (deltaY * 24));
        return true;
    }

    private Entry entryAt(double mouseX, double mouseY) {
        int cols = Math.max(1, Math.min(4, (width - GAP) / (THUMB_W + GAP)));
        int gridW = cols * THUMB_W + (cols - 1) * GAP;
        int startX = (width - gridW) / 2;
        for (int i = 0; i < entries.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (THUMB_W + GAP);
            int y = TOP + row * (THUMB_H + GAP) - scroll;
            if (mouseX >= x && mouseX < x + THUMB_W && mouseY >= y && mouseY < y + THUMB_H) {
                return entries.get(i);
            }
        }
        return null;
    }

    @Override
public boolean isPauseScreen() {
return true;
}

    @Override
    public void onClose() {
        closeTextures();
        Minecraft.getInstance().setScreen(parent);
    }

    private void closeTextures() {
        for (Entry entry : entries) {
            if (entry.texture != null) {
                entry.texture.close();
                entry.texture = null;
            }
        }
    }

    private int rows(int cols) {
        if (entries.isEmpty()) {
            return 0;
        }
        return (entries.size() + cols - 1) / cols;
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static Path screenshotDir(Minecraft client) {
        return client.gameDirectory.toPath().resolve("screenshots").resolve("itrp_minimal_offline");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Entry {
        private final Path path;
        private ResourceLocation textureId;
        private DynamicTexture texture;
        private int width;
        private int height;

        private Entry(Path path) {
            this.path = path;
        }
    }
}