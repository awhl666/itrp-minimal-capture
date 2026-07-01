/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.NativeImage
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 */
package com.itrpminimal.capture;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MinimalScreenshotPreviewScreen
extends Screen {
    private static final int LIST_ITEM_HEIGHT = 24;
    private static final int LIST_TOP = 58;
    private static final int PANEL_PAD = 8;
    private static final int SCROLL_BAR_WIDTH = 12;
    private static final int SCROLL_BAR_HIT_WIDTH = 24;
    private static final int GRID_CARD_WIDTH = 176;
    private static final int GRID_CARD_HEIGHT = 132;
    private static final int GRID_GAP = 10;
    private static final int GRID_IMAGE_PAD = 4;
    private static final int GRID_LABEL_HEIGHT = 18;
    private static boolean gridLayout;
    private final Screen parent;
    private final List<Path> files = new ArrayList<Path>();
    private final List<Path> entries = new ArrayList<Path>();
    private final Map<Path, ThumbTexture> thumbnailCache = new HashMap<Path, ThumbTexture>();
    private Path screenshotsRoot;
    private Path currentFolder;
    private int scrollOffset;
    private Path viewingFile;
    private ResourceLocation viewingTexture;
    private int viewingWidth;
    private int viewingHeight;
    private float previewScale = 1.0f;
    private long lastPreviewClickMs;
    private double lastPreviewClickX;
    private double lastPreviewClickY;
    private float previewPanX;
    private float previewPanY;
    private long lastPreviewInteractionMs;
    private boolean draggingPreview;
    private boolean draggingScrollBar;
    private double lastDragX;
    private double lastDragY;
    private boolean actionsOpen;
    private boolean confirmingDelete;
    private long titleToastUntilMs;
    private long scaleToastUntilMs;
    private boolean renaming;
    private EditBox renameBox;
    private long transformAnimStartMs;
    private long transformAnimDurationMs;
    private float animFromScale;
    private float animFromPanX;
    private float animFromPanY;
    private float animToScale;
    private float animToPanX;
    private float animToPanY;
    private static final int CUSTOM_BUTTON_BG = -1440735200;
    private static final int CUSTOM_BUTTON_BG_HOVER = -868599238;
    private static final int CUSTOM_BUTTON_BORDER = -2039584;

    private static final class ThumbTexture {
        final ResourceLocation location;
        final int width;
        final int height;
        final long modified;

        ThumbTexture(ResourceLocation location, int width, int height, long modified) {
            this.location = location;
            this.width = width;
            this.height = height;
            this.modified = modified;
        }
    }
    public MinimalScreenshotPreviewScreen(Screen parent) {
        super((Component)Component.literal((String)"\u622a\u56fe\u9884\u89c8"));
        this.parent = parent;
        this.screenshotsRoot = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
        Path defaultFolder = this.screenshotsRoot.resolve("itrp_minimal_offline");
        this.currentFolder = Files.isDirectory(defaultFolder, new LinkOption[0]) ? defaultFolder : this.screenshotsRoot;
        this.scanFiles();
    }

    private Screen rootParent() {
        if (this.parent instanceof MinimalCaptureScreen captureScreen) {
            return captureScreen.returnParent();
        }
        return this.parent;
    }

    public static void openLatestScreenshot(Minecraft client, Screen parent) {
        MinimalScreenshotPreviewScreen screen = new MinimalScreenshotPreviewScreen(parent);
        client.setScreen((Screen)screen);
        screen.openLatestScreenshotInTree();
    }

    private void openLatestScreenshotInTree() {
        Path latest = this.findLatestPng(this.screenshotsRoot);
        if (latest == null) {
            latest = this.findLatestPng(this.currentFolder);
        }
        if (latest != null) {
            Path parentPath = latest.getParent();
            if (parentPath != null) {
                this.currentFolder = parentPath;
                this.scanFiles();
            }
            this.loadAndView(latest);
            this.rebuildWidgets();
        } else if (this.minecraft != null && this.minecraft.gui != null) {
            this.minecraft.gui.getChat().addMessage((Component)Component.literal((String)"截图管理器: 没有可预览的截图"));
        }
    }

    private Path findLatestPng(Path root) {
        if (root == null || !Files.exists(root, new LinkOption[0])) {
            return null;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(root, 4)) {
            return stream.filter(path -> Files.isRegularFile(path, new LinkOption[0]) && this.isPng(path))
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()))
                    .orElse(null);
        } catch (Exception e) {
            MinimalLog.line("preview latest scan failed " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }


    private void scanFiles() {
        this.files.clear();
        this.entries.clear();
        try {
            Files.createDirectories(this.currentFolder, new FileAttribute[0]);
        }
        catch (Exception exception) {
            // empty catch block
        }
        ArrayList<Path> folders = new ArrayList<Path>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(this.currentFolder);){
            for (Path entry : ds) {
                if (Files.isDirectory(entry, new LinkOption[0])) {
                    folders.add(entry);
                    continue;
                }
                if (!Files.isRegularFile(entry, new LinkOption[0]) || !this.isPng(entry)) continue;
                this.files.add(entry);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        folders.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
        this.files.sort(Comparator.comparingLong(p -> -p.toFile().lastModified()));
        this.entries.addAll(folders);
        this.entries.addAll(this.files);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScrollOffset()));
        this.pruneThumbnailCache();
    }

    private boolean isPng(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png");
    }

    private int itemsPerPage() {
        if (gridLayout) {
            return this.gridItemsPerPage();
        }
        int bottom = this.height - 76;
        return Math.max(3, (bottom - 58) / 24);
    }

    private int listItemsPerPage() {
        int bottom = this.height - 76;
        return Math.max(3, (bottom - 58) / 24);
    }

    private int gridColumns(int listW) {
        return Math.max(2, Math.max(1, (listW + GRID_GAP) / (GRID_CARD_WIDTH + GRID_GAP)));
    }

    private int gridRows() {
        int availableHeight = Math.max(GRID_CARD_HEIGHT, this.height - 58 - 76);
        return Math.max(2, availableHeight / (GRID_CARD_HEIGHT + GRID_GAP));
    }

    private int gridItemsPerPage() {
        return this.gridColumns(this.listWidth()) * this.gridRows();
    }

    private int gridStartX(int listX, int listW) {
        int columns = this.gridColumns(listW);
        int gridW = columns * GRID_CARD_WIDTH + (columns - 1) * GRID_GAP;
        return listX + Math.max(0, (listW - gridW) / 2);
    }

    private int listWidth() {
        return Math.min(Math.max(360, (int)((float)this.width * 0.72f)), this.width - 72);
    }

    private int listLeft() {
        return this.width / 2 - this.listWidth() / 2;
    }

    private int maxScrollOffset() {
        return Math.max(0, this.visibleEntryCount() - this.itemsPerPage());
    }

    private int visibleEntryCount() {
        return this.entries.size();
    }

    private boolean canGoUp() {
        Path normalizedRoot = this.screenshotsRoot.normalize();
        Path normalizedCurrent = this.currentFolder.normalize();
        return normalizedCurrent.startsWith(normalizedRoot) && !normalizedCurrent.equals(normalizedRoot);
    }

    private String folderLabel() {
        Path normalizedRoot = this.screenshotsRoot.normalize();
        Path normalizedCurrent = this.currentFolder.normalize();
        if (normalizedCurrent.equals(normalizedRoot)) {
            return "\u5168\u90e8\u622a\u56fe";
        }
        try {
            return "\u5168\u90e8\u622a\u56fe / " + normalizedRoot.relativize(normalizedCurrent).toString().replace('\\', '/');
        }
        catch (Exception ignored) {
            Path name = this.currentFolder.getFileName();
            return name == null ? this.currentFolder.toString() : name.toString();
        }
    }

    private void switchFolder(Path folder) {
        Path normalizedRoot;
        if (folder == null) {
            return;
        }
        Path normalized = folder.normalize();
        this.currentFolder = normalized.startsWith(normalizedRoot = this.screenshotsRoot.normalize()) ? normalized : normalizedRoot;
        this.scrollOffset = 0;
        this.scanFiles();
        this.rebuildWidgets();
    }

    private void openParentFolder() {
        Path parentPath = this.currentFolder.getParent();
        if (parentPath != null) {
            this.switchFolder(parentPath);
        }
    }

    private void openEntry(Path entry) {
        if (Files.isDirectory(entry, new LinkOption[0])) {
            this.switchFolder(entry);
        } else if (this.isPng(entry)) {
            this.loadAndView(entry);
        }
    }

    private String entryLabel(Path entry) {
        String name;
        String string = name = entry.getFileName() == null ? entry.toString() : entry.getFileName().toString();
        if (Files.isDirectory(entry, new LinkOption[0])) {
            return name + " /";
        }
        return name;
    }

    private String fileMeta(Path entry) {
        long size = 0L;
        long modified = 0L;
        try {
            size = Files.size(entry);
            modified = Files.getLastModifiedTime(entry, new LinkOption[0]).toMillis();
        }
        catch (Exception exception) {
            // empty catch block
        }
        String sizeText = size >= 0x100000L ? String.format(Locale.ROOT, "%.1fMB", (double)size / 1024.0 / 1024.0) : Math.max(1L, size / 1024L) + "KB";
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(modified), ZoneId.systemDefault());
        return sizeText + " \u00b7 " + DateTimeFormatter.ofPattern("MM-dd HH:mm").format(time);
    }

    protected void init() {
        super.init();
        int listW = this.listWidth();
        int listX = this.listLeft();
        if (this.viewingFile != null) {
            if (this.renaming) {
                int boxW = this.renamePanelWidth();
                int boxX = this.renamePanelX();
                int boxY = this.renamePanelY();
                this.renameBox = new EditBox(this.font, boxX + 8, boxY + 25, boxW - 104, 18, (Component)Component.literal((String)"\u6587\u4ef6\u540d"));
                this.renameBox.setValue(this.stripPng(this.viewingFile.getFileName().toString()));
                this.addRenderableWidget(this.renameBox);
            }
            return;
        }
        if (this.canGoUp()) {
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"‹ 返回上一级"), b -> this.openParentFolder()).bounds(listX, 34, Math.min(140, listW), 20).build());
        }
        if (this.visibleEntryCount() == 0) {
            this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u6253\u5f00\u5f53\u524d\u6587\u4ef6\u5939"), b -> Util.getPlatform().openFile(this.currentFolder.toFile())).bounds(listX, 108, Math.min(180, listW), 20).build());
        }
        int bottomY = Math.max(8, this.height - 32);
        int bottomW = Math.min(Math.max(240, listW), Math.max(240, this.width - 32));
        int bottomX = Math.max(8, this.width / 2 - bottomW / 2);
        if (bottomX + bottomW > this.width - 8) {
            bottomX = Math.max(8, this.width - bottomW - 8);
        }
        int fourth = Math.max(48, (bottomW - 12) / 4);
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)(gridLayout ? "\u5e03\u5c40: \u7f51\u683c" : "\u5e03\u5c40: \u5217\u8868")), b -> {
            gridLayout = !gridLayout;
            this.clampScrollOffset();
            this.rebuildWidgets();
        }).bounds(bottomX, bottomY, fourth, 20).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"截图管理器"), b -> Minecraft.getInstance().setScreen((Screen)new MinimalCaptureScreen(this.rootParent()))).bounds(bottomX + fourth + 4, bottomY, fourth, 20).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u6253\u5f00\u6839\u76ee\u5f55"), b -> Util.getPlatform().openFile(this.screenshotsRoot.toFile())).bounds(bottomX + (fourth + 4) * 2, bottomY, fourth, 20).build());
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"\u8fd4\u56de"), b -> Minecraft.getInstance().setScreen(this.parent)).bounds(bottomX + (fourth + 4) * 3, bottomY, fourth, 20).build());
    }

    private void loadAndView(Path file) {
        block8: {
            this.releaseTexture();
            try {
                NativeImage image = NativeImage.read((InputStream)Files.newInputStream(file, new OpenOption[0]));
                this.viewingWidth = image.getWidth();
                this.viewingHeight = image.getHeight();
                this.viewingTexture = this.makeResourceLocation("itrp_minimal_capture", "preview_" + Math.abs(file.toAbsolutePath().toString().hashCode()) + "_" + file.toFile().lastModified());
                Minecraft.getInstance().getTextureManager().register(this.viewingTexture, (AbstractTexture)new DynamicTexture(image));
                this.viewingFile = file;
                this.previewScale = 1.0f;
                this.previewPanX = 0.0f;
                this.previewPanY = 0.0f;
                this.transformAnimStartMs = 0L;
                this.lastPreviewInteractionMs = Util.getMillis();
                this.titleToastUntilMs = Util.getMillis() + 2500L;
                this.actionsOpen = false;
                this.confirmingDelete = false;
                this.renaming = false;
                this.renameBox = null;
                this.scaleToastUntilMs = 0L;
                this.rebuildWidgets();
            }
            catch (Exception e) {
                if (this.minecraft == null || this.minecraft.gui == null) break block8;
                this.minecraft.gui.getChat().addMessage((Component)Component.literal((String)("截图管理器: \u9884\u89c8\u5931\u8d25 " + e.getClass().getSimpleName() + ": " + e.getMessage())));
            }
        }
    }

    private ResourceLocation makeResourceLocation(String namespace, String path) throws Exception {
        Object value2;
        try {
            value2 = ResourceLocation.class.getMethod("fromNamespaceAndPath", String.class, String.class).invoke(null, namespace, path);
            if (value2 instanceof ResourceLocation) {
                return (ResourceLocation)value2;
            }
        }
        catch (Throwable ignoredFromNamespace) {
            // empty catch block
        }
        try {
            value2 = ResourceLocation.class.getMethod("tryBuild", String.class, String.class).invoke(null, namespace, path);
            if (value2 instanceof ResourceLocation) {
                return (ResourceLocation)value2;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            return (ResourceLocation)ResourceLocation.class.getConstructor(String.class, String.class).newInstance(namespace, path);
        }
        catch (Throwable throwable) {
            return ResourceLocation.tryParse((String)(namespace + ":" + path));
        }
    }

    private void viewRelative(int offset) {
        if (this.files.isEmpty() || this.viewingFile == null) {
            return;
        }
        int current = this.files.indexOf(this.viewingFile);
        if (current < 0) {
            current = 0;
        }
        int next = Math.floorMod(current + offset, this.files.size());
        this.loadAndView(this.files.get(next));
        this.titleToastUntilMs = Util.getMillis() + 2500L;
        this.scaleToastUntilMs = 0L;
    }

    private void returnToList() {
        this.releaseTexture();
        this.viewingFile = null;
        this.viewingWidth = 0;
        this.viewingHeight = 0;
        this.previewScale = 1.0f;
        this.previewPanX = 0.0f;
        this.previewPanY = 0.0f;
        this.transformAnimStartMs = 0L;
        this.lastPreviewInteractionMs = 0L;
        this.actionsOpen = false;
        this.confirmingDelete = false;
        this.renaming = false;
        this.renameBox = null;
        this.titleToastUntilMs = 0L;
        this.scaleToastUntilMs = 0L;
        this.scanFiles();
        this.rebuildWidgets();
    }

    private void releaseTexture() {
        if (this.viewingTexture != null) {
            Minecraft.getInstance().getTextureManager().release(this.viewingTexture);
            this.viewingTexture = null;
        }
    }

    public void onClose() {
        this.releaseTexture();
        this.releaseThumbnailCache();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    public void tick() {
        super.tick();
        if (this.viewingFile != null && this.lastPreviewInteractionMs > 0L && Util.getMillis() - this.lastPreviewInteractionMs > 15000L) {
            if (this.previewScale != 1.0f || this.previewPanX != 0.0f || this.previewPanY != 0.0f) {
                this.resetPreviewTransform();
                MinimalLog.line("preview auto reset to origin");
            }
            this.lastPreviewInteractionMs = Util.getMillis();
        }
    }

    private float previewFitScale() {
        int maxW = (int)((float)this.width * 0.86f);
        int maxH = (int)((float)this.height * 0.62f);
        return Math.min((float)maxW / (float)Math.max(1, this.viewingWidth), (float)maxH / (float)Math.max(1, this.viewingHeight));
    }

    private float previewDisplayScale() {
        return this.previewFitScale() * this.previewScale;
    }

    private float previewDrawWidthF() {
        return Math.max(1.0f, (float)this.viewingWidth * this.previewDisplayScale());
    }

    private float previewDrawHeightF() {
        return Math.max(1.0f, (float)this.viewingHeight * this.previewDisplayScale());
    }

    private float previewDrawXF() {
        return (float)this.width / 2.0f + this.previewPanX - this.previewDrawWidthF() / 2.0f;
    }

    private float previewDrawYF() {
        return (float)this.height / 2.0f - 6.0f + this.previewPanY - this.previewDrawHeightF() / 2.0f;
    }

    private int previewDrawWidth() {
        return Math.max(1, Math.round(this.previewDrawWidthF()));
    }

    private int previewDrawHeight() {
        return Math.max(1, Math.round(this.previewDrawHeightF()));
    }

    private int previewDrawX() {
        return Math.round(this.previewDrawXF());
    }

    private int previewDrawY() {
        return Math.round(this.previewDrawYF());
    }

    private int renamePanelWidth() {
        return Math.min(272, Math.max(180, this.previewDrawWidth() - 24));
    }

    private int renamePanelX() {
        int panelW = this.renamePanelWidth();
        int centered = this.previewDrawX() + this.previewDrawWidth() / 2 - panelW / 2;
        return Math.max(8, Math.min(this.width - panelW - 8, centered));
    }

    private int renamePanelY() {
        return this.previewDrawY() + this.previewDrawHeight() + 8;
    }

    private void renderFileManager(GuiGraphics g) {
        int listW = this.listWidth();
        int listX = this.listLeft();
        int panelTop = 26;
        int panelBottom = this.height - 50;
        g.fill(listX - 8, panelTop, listX + listW + 8, panelBottom, 0xF0101218);
        g.fill(listX - 8, panelTop, listX + listW + 8, panelTop + 1, 0x66FFFFFF);
        g.fill(listX - 8, panelBottom - 1, listX + listW + 8, panelBottom, 0x44FFFFFF);
        String title = "\u622a\u56fe\u7ba1\u7406\u5668";
        String stats = this.foldersCount() + " \u4e2a\u6587\u4ef6\u5939 \u00b7 " + this.files.size() + " \u5f20 PNG \u00b7 " + this.folderLabel();
        g.drawString(this.font, (Component)Component.literal((String)title), listX, 12, 0xFFFFFF, false);
        g.drawString(this.font, (Component)Component.literal((String)this.trimToWidth(stats, listW - 104)), listX + 104, 12, 11065599, false);
        if (gridLayout) {
            this.renderDirectoryGrid(g, listX, listW);
        } else {
            this.renderDirectoryRows(g, listX, listW);
        }
        this.renderScrollBar(g, listX, listW);
        if (this.visibleEntryCount() == 0) {
            g.drawCenteredString(this.font, (Component)Component.literal((String)"\u5f53\u524d\u76ee\u5f55\u4e3a\u7a7a"), this.width / 2, 78, 0xCCCCCC);
        }
    }

    private void renderDirectoryRows(GuiGraphics g, int listX, int listW) {
        int y = 58;
        int shown = 0;
        int index = 0;
        int pageSize = this.itemsPerPage();
        for (Path entry : this.entries) {
            if (index < this.scrollOffset) {
                ++index;
                continue;
            }
            if (shown >= pageSize) break;
            boolean folder = Files.isDirectory(entry, new LinkOption[0]);
            this.renderDirectoryRow(g, listX, y, listW, folder ? "\u25b8" : "\u25a1", this.entryLabel(entry), folder ? "\u6587\u4ef6\u5939" : this.fileMeta(entry), folder, false);
            y += 24;
            ++shown;
            ++index;
        }
    }

    private void renderDirectoryGrid(GuiGraphics g, int listX, int listW) {
        int columns = this.gridColumns(listW);
        int startX = this.gridStartX(listX, listW);
        int startY = 58;
        int shown = 0;
        int index = 0;
        int pageSize = this.itemsPerPage();
        for (Path entry : this.entries) {
            if (index < this.scrollOffset) {
                ++index;
                continue;
            }
            if (shown >= pageSize) break;
            boolean folder = Files.isDirectory(entry, new LinkOption[0]);
            this.renderDirectoryCard(g, startX, startY, shown, columns, entry, folder ? ">" : "[]", this.entryLabel(entry), folder ? "\u6587\u4ef6\u5939" : this.fileMeta(entry), folder);
            ++shown;
            ++index;
        }
    }

    private void renderDirectoryCard(GuiGraphics g, int startX, int startY, int shown, int columns, Path entry, String icon, String name, String meta, boolean folder) {
        int col = shown % columns;
        int row = shown / columns;
        int x = startX + col * (GRID_CARD_WIDTH + GRID_GAP);
        int y = startY + row * (GRID_CARD_HEIGHT + GRID_GAP);
        int bg = folder ? 0xCC182438 : 0xCC11151C;
        int border = folder ? 0x884A76A8 : 0x557A8A9A;
        g.fill(x, y, x + GRID_CARD_WIDTH, y + GRID_CARD_HEIGHT, bg);
        g.fill(x, y, x + GRID_CARD_WIDTH, y + 1, border);
        g.fill(x, y + GRID_CARD_HEIGHT - 1, x + GRID_CARD_WIDTH, y + GRID_CARD_HEIGHT, border);
        g.fill(x, y, x + 1, y + GRID_CARD_HEIGHT, border);
        g.fill(x + GRID_CARD_WIDTH - 1, y, x + GRID_CARD_WIDTH, y + GRID_CARD_HEIGHT, border);
        if (!folder && entry != null && this.isPng(entry)) {
            int imageX = x + GRID_IMAGE_PAD;
            int imageY = y + GRID_IMAGE_PAD;
            int imageW = GRID_CARD_WIDTH - GRID_IMAGE_PAD * 2;
            int imageH = GRID_CARD_HEIGHT - GRID_IMAGE_PAD * 2 - GRID_LABEL_HEIGHT;
            this.renderThumbnail(g, entry, imageX, imageY, imageW, imageH);
            int labelY = y + GRID_CARD_HEIGHT - GRID_LABEL_HEIGHT;
            g.fill(x, labelY, x + GRID_CARD_WIDTH, y + GRID_CARD_HEIGHT, 0xCC080A0E);
            g.drawString(this.font, (Component)Component.literal((String)this.trimToWidth(name, GRID_CARD_WIDTH - 8)), x + 4, labelY + 4, -1, false);
            return;
        }
        g.drawString(this.font, (Component)Component.literal((String)icon), x + 7, y + 7, folder ? -1654438 : -6367233, false);
        g.drawString(this.font, (Component)Component.literal((String)this.trimToWidth(name, GRID_CARD_WIDTH - 28)), x + 24, y + 7, folder ? -1 : -1513240, false);
        if (!meta.isEmpty()) {
            g.drawString(this.font, (Component)Component.literal((String)this.trimToWidth(meta, GRID_CARD_WIDTH - 14)), x + 7, y + 25, -7368817, false);
        }
    }

    private void renderThumbnail(GuiGraphics g, Path file, int x, int y, int w, int h) {
        ThumbTexture thumb = this.getThumbnail(file);
        if (thumb == null) {
            g.fill(x, y, x + w, y + h, 0x9920242C);
            g.drawCenteredString(this.font, (Component)Component.literal((String)"PNG"), x + w / 2, y + Math.max(2, h / 2 - 4), 0xFF8AA8C8);
            return;
        }
        float scale = Math.min((float)w / (float)Math.max(1, thumb.width), (float)h / (float)Math.max(1, thumb.height));
        int drawW = Math.max(1, Math.round((float)thumb.width * scale));
        int drawH = Math.max(1, Math.round((float)thumb.height * scale));
        int drawX = x + (w - drawW) / 2;
        int drawY = y + (h - drawH) / 2;
        g.fill(x, y, x + w, y + h, 0x9920242C);
        g.blit(thumb.location, drawX, drawY, 0, 0, drawW, drawH, thumb.width, thumb.height);
    }

    private ThumbTexture getThumbnail(Path file) {
        if (file == null || !this.isPng(file)) {
            return null;
        }
        long modified = file.toFile().lastModified();
        ThumbTexture cached = this.thumbnailCache.get(file);
        if (cached != null && cached.modified == modified) {
            return cached;
        }
        if (cached != null) {
            Minecraft.getInstance().getTextureManager().release(cached.location);
            this.thumbnailCache.remove(file);
        }
        try (NativeImage image = NativeImage.read((InputStream)Files.newInputStream(file, new OpenOption[0]))) {
            ResourceLocation location = this.makeResourceLocation("itrp_minimal_capture", "thumb_full_" + Math.abs(file.toAbsolutePath().toString().hashCode()) + "_" + modified);
            Minecraft.getInstance().getTextureManager().register(location, (AbstractTexture)new DynamicTexture(image));
            ThumbTexture thumb = new ThumbTexture(location, image.getWidth(), image.getHeight(), modified);
            this.thumbnailCache.put(file, thumb);
            return thumb;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private void pruneThumbnailCache() {
        Iterator<Map.Entry<Path, ThumbTexture>> iterator = this.thumbnailCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Path, ThumbTexture> entry = iterator.next();
            Path file = entry.getKey();
            ThumbTexture thumb = entry.getValue();
            if (!Files.isRegularFile(file, new LinkOption[0]) || file.toFile().lastModified() != thumb.modified) {
                Minecraft.getInstance().getTextureManager().release(thumb.location);
                iterator.remove();
            }
        }
    }

    private void releaseThumbnailCache() {
        for (ThumbTexture thumb : this.thumbnailCache.values()) {
            Minecraft.getInstance().getTextureManager().release(thumb.location);
        }
        this.thumbnailCache.clear();
    }

    private void renderScrollBar(GuiGraphics g, int listX, int listW) {
        int page;
        int total = this.visibleEntryCount();
        if (total <= (page = this.itemsPerPage())) {
            return;
        }
        int barX = this.scrollBarX(listX, listW);
        int buttonTop = this.scrollButtonUpY();
        int buttonSize = this.scrollButtonSize();
        int trackTop = this.scrollBarTop();
        int trackH = this.scrollBarHeight(page);
        int downY = this.scrollButtonDownY(page);
        int thumbH = this.scrollBarThumbHeight(trackH, page, total);
        int thumbY = this.scrollBarThumbY(trackTop, trackH, thumbH);
        g.fill(barX, buttonTop, barX + buttonSize, buttonTop + buttonSize, 0xCC151B24);
        g.fill(barX, downY, barX + buttonSize, downY + buttonSize, 0xCC151B24);
        g.fill(barX + 1, buttonTop + 1, barX + buttonSize - 1, buttonTop + buttonSize - 1, this.scrollOffset <= 0 ? 0x66334455 : 0xAA7FB9E6);
        g.fill(barX + 1, downY + 1, barX + buttonSize - 1, downY + buttonSize - 1, this.scrollOffset >= this.maxScrollOffset() ? 0x66334455 : 0xAA7FB9E6);
        g.drawCenteredString(this.font, (Component)Component.literal((String)"^"), barX + buttonSize / 2, buttonTop + 4, 0xFFFFFFFF);
        g.drawCenteredString(this.font, (Component)Component.literal((String)"v"), barX + buttonSize / 2, downY + 4, 0xFFFFFFFF);
        g.fill(barX + 2, trackTop, barX + buttonSize - 2, trackTop + trackH, 0xAA20242C);
        g.fill(barX + 2, trackTop, barX + 3, trackTop + trackH, 0x44FFFFFF);
        g.fill(barX + buttonSize - 3, trackTop, barX + buttonSize - 2, trackTop + trackH, 0x66000000);
        g.fill(barX + 4, thumbY, barX + buttonSize - 4, thumbY + thumbH, this.draggingScrollBar ? 0xFFE0C060 : 0xFF9AC2E2);
    }

    private int scrollButtonSize() {
        return 18;
    }

    private int scrollBarX(int listX, int listW) {
        return listX + listW + 8;
    }

    private int scrollButtonUpY() {
        return 58;
    }

    private int scrollBarTop() {
        return this.scrollButtonUpY() + this.scrollButtonSize() + 4;
    }

    private int scrollBarHeight(int page) {
        return Math.max(32, page * 24 - this.scrollButtonSize() * 2 - 12);
    }

    private int scrollButtonDownY(int page) {
        return this.scrollBarTop() + this.scrollBarHeight(page) + 4;
    }

    private int scrollBarThumbHeight(int trackH, int page, int total) {
        return Math.max(24, Math.min(trackH, trackH * page / Math.max(1, total)));
    }

    private int scrollBarThumbY(int trackTop, int trackH, int thumbH) {
        int maxOffset = Math.max(1, this.maxScrollOffset());
        int clamped = Math.max(0, Math.min(this.scrollOffset, this.maxScrollOffset()));
        return trackTop + (trackH - thumbH) * clamped / maxOffset;
    }

    private boolean isScrollUpButtonHit(double mouseX, double mouseY) {
        int page = this.itemsPerPage();
        if (this.visibleEntryCount() <= page) {
            return false;
        }
        int x = this.scrollBarX(this.listLeft(), this.listWidth());
        int y = this.scrollButtonUpY();
        int s = this.scrollButtonSize();
        return mouseX >= (double)x && mouseX <= (double)(x + s) && mouseY >= (double)y && mouseY <= (double)(y + s);
    }

    private boolean isScrollDownButtonHit(double mouseX, double mouseY) {
        int page = this.itemsPerPage();
        if (this.visibleEntryCount() <= page) {
            return false;
        }
        int x = this.scrollBarX(this.listLeft(), this.listWidth());
        int y = this.scrollButtonDownY(page);
        int s = this.scrollButtonSize();
        return mouseX >= (double)x && mouseX <= (double)(x + s) && mouseY >= (double)y && mouseY <= (double)(y + s);
    }

    private boolean isScrollBarHit(double mouseX, double mouseY) {
        int page;
        int total = this.visibleEntryCount();
        if (total <= (page = this.itemsPerPage())) {
            return false;
        }
        int listX = this.listLeft();
        int listW = this.listWidth();
        int hitX = this.scrollBarX(listX, listW) - 4;
        int top = this.scrollBarTop();
        int height = this.scrollBarHeight(page);
        return mouseX >= (double)hitX && mouseX <= (double)(hitX + this.scrollButtonSize() + 8) && mouseY >= (double)top && mouseY <= (double)(top + height);
    }

    private void updateScrollOffsetFromMouse(double mouseY) {
        int total = this.visibleEntryCount();
        int page = this.itemsPerPage();
        int trackTop = this.scrollBarTop();
        int trackH = this.scrollBarHeight(page);
        int thumbH = this.scrollBarThumbHeight(trackH, page, total);
        int available = Math.max(1, trackH - thumbH);
        double ratio = (Math.max(trackTop, Math.min(trackTop + trackH, mouseY)) - (double)trackTop - (double)thumbH / 2.0) / (double)available;
        int nextOffset = (int)Math.round(Math.max(0.0, Math.min(1.0, ratio)) * (double)this.maxScrollOffset());
        this.scrollOffset = Math.max(0, Math.min(this.maxScrollOffset(), nextOffset));
    }

    private void clampScrollOffset() {
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScrollOffset()));
    }

    private void renderDirectoryRow(GuiGraphics g, int x, int y, int width, String icon, String name, String meta, boolean folder, boolean selected) {
        int bg = selected ? 0xDD405070 : (folder ? 0xCC182438 : 0xCC11151C);
        int border = folder ? 0x884A76A8 : 0x557A8A9A;
        g.fill(x, y, x + width, y + 20, bg);
        g.fill(x, y + 19, x + width, y + 20, border);
        int iconColor = folder ? -1654438 : -6367233;
        g.drawString(this.font, (Component)Component.literal((String)icon), x + 8, y + 6, iconColor, false);
        int metaW = meta.isEmpty() ? 0 : this.font.width(meta);
        int nameMaxW = width - 42 - metaW - 18;
        g.drawString(this.font, (Component)Component.literal((String)this.trimToWidth(name, nameMaxW)), x + 28, y + 6, folder ? -1 : -1513240, false);
        if (!meta.isEmpty()) {
            g.drawString(this.font, (Component)Component.literal((String)meta), x + width - metaW - 10, y + 6, -7368817, false);
        }
    }

    private int foldersCount() {
        int count = 0;
        for (Path entry : this.entries) {
            if (!Files.isDirectory(entry, new LinkOption[0])) continue;
            ++count;
        }
        return count;
    }

    private String trimToWidth(String text, int maxWidth) {
        int limit;
        if (maxWidth <= 12 || this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        for (limit = Math.max(1, text.length()); limit > 1 && this.font.width(text.substring(0, limit) + ellipsis) > maxWidth; --limit) {
        }
        return text.substring(0, limit) + ellipsis;
    }

    private Path entryAtPosition(double mouseX, double mouseY) {
        return gridLayout ? this.entryAtGridPosition(mouseX, mouseY) : this.entryAtListPosition(mouseX, mouseY);
    }

    private Path entryAtListPosition(double mouseX, double mouseY) {
        int listX = this.listLeft();
        int listW = this.listWidth();
        if (mouseX < (double)listX || mouseX > (double)(listX + listW) || mouseY < 58.0) {
            return null;
        }
        int row = (int)((mouseY - 58.0) / 24.0);
        if (row < 0 || row >= this.itemsPerPage()) {
            return null;
        }
        int index = this.scrollOffset + row;
        return index >= 0 && index < this.entries.size() ? this.entries.get(index) : null;
    }

    private Path entryAtGridPosition(double mouseX, double mouseY) {
        int listX = this.listLeft();
        int listW = this.listWidth();
        int columns = this.gridColumns(listW);
        int startX = this.gridStartX(listX, listW);
        int startY = 58;
        if (mouseY < (double)startY) {
            return null;
        }
        int localX = (int)(mouseX - (double)startX);
        int localY = (int)(mouseY - (double)startY);
        int stepX = GRID_CARD_WIDTH + GRID_GAP;
        int stepY = GRID_CARD_HEIGHT + GRID_GAP;
        if (localX < 0 || localY < 0) {
            return null;
        }
        int col = localX / stepX;
        int row = localY / stepY;
        if (col < 0 || col >= columns || row < 0 || row >= this.gridRows()) {
            return null;
        }
        if (localX % stepX >= GRID_CARD_WIDTH || localY % stepY >= GRID_CARD_HEIGHT) {
            return null;
        }
        int index = this.scrollOffset + row * columns + col;
        return index >= 0 && index < this.entries.size() ? this.entries.get(index) : null;
    }

    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        if (this.viewingFile == null || this.viewingTexture == null) {
            this.renderFileManager(g);
            return;
        }
        this.updatePreviewAnimation();
        float displayScale = this.previewDisplayScale();
        float dx = this.previewDrawXF();
        float dy = this.previewDrawYF();
        g.pose().pushPose();
        g.pose().translate(dx, dy, 0.0f);
        g.pose().scale(displayScale, displayScale, 1.0f);
        g.blit(this.viewingTexture, 0, 0, this.viewingWidth, this.viewingHeight, 0.0f, 0.0f, this.viewingWidth, this.viewingHeight, this.viewingWidth, this.viewingHeight);
        g.pose().popPose();
        int border = -4671304;
        int bx = Math.round(dx);
        int by = Math.round(dy);
        int bw = Math.round(this.previewDrawWidthF());
        int bh = Math.round(this.previewDrawHeightF());
        g.fill(bx - 2, by - 2, bx + bw + 2, by, border);
        g.fill(bx - 2, by + bh, bx + bw + 2, by + bh + 2, border);
        g.fill(bx - 2, by, bx, by + bh, border);
        g.fill(bx + bw, by, bx + bw + 2, by + bh, border);
        long now = Util.getMillis();
        if (now < this.titleToastUntilMs) {
            String title = this.viewingFile.getFileName().toString();
            if (title.length() > 46) {
                title = title.substring(0, 43) + "...";
            }
            int infoX = 14;
            int infoY = this.height - 38;
            int infoW = Math.min(this.width - 220, this.font.width(title) + 16);
            g.fill(infoX - 4, infoY - 4, infoX + infoW, infoY + 14, -2013265920);
            g.drawString(this.font, (Component)Component.literal(title), infoX + 4, infoY, 15266047, false);
        } else if (now < this.scaleToastUntilMs) {
            String status = String.format(Locale.ROOT, "%.2fx", Float.valueOf(this.previewScale));
            int infoX = 18;
            int infoY = this.height - 38;
            int infoW = this.font.width(status) + 16;
            g.fill(infoX - 4, infoY - 4, infoX + infoW, infoY + 14, -2013265920);
            g.drawString(this.font, (Component)Component.literal((String)status), infoX + 4, infoY, 0xAACCFF, false);
        }
        if (this.renaming) {
            int panelW = this.renamePanelWidth();
            int panelX = this.renamePanelX();
            int panelY = this.renamePanelY();
            g.fill(panelX, panelY, panelX + panelW, panelY + 50, -872415232);
            g.fill(panelX, panelY, panelX + panelW, panelY + 1, -4671304);
            g.drawString(this.font, (Component)Component.literal((String)"\u91cd\u547d\u540d"), panelX + 8, panelY + 7, 0xFFFFFF, false);
            this.drawCustomButton(g, "\u786e\u8ba4", panelX + panelW - 90, panelY + 25, 40, 18, mx, my);
            this.drawCustomButton(g, "\u53d6\u6d88", panelX + panelW - 46, panelY + 25, 40, 18, mx, my);
        }
        this.renderPreviewControls(g, mx, my);
    }

    private void renderPreviewControls(GuiGraphics g, int mx, int my) {
        int right = this.width - 12;
        int bottom = this.height - 12;
        int controlsW = 150;
        int controlsX = right - controlsW;
        this.drawCustomButton(g, "<", controlsX, bottom - 20, 30, 20, mx, my);
        this.drawCustomButton(g, ">", controlsX + 36, bottom - 20, 30, 20, mx, my);
        this.drawCustomButton(g, "\u8fd4\u56de", controlsX + 72, bottom - 20, 46, 20, mx, my);
        this.drawCustomButton(g, "\u22ef", controlsX + 124, bottom - 20, 26, 20, mx, my);
        if (this.actionsOpen) {
            int menuX = right - 96;
            int menuY = bottom - 118;
            this.drawCustomButton(g, "\u6253\u5f00", menuX, menuY, 82, 20, mx, my);
            this.drawCustomButton(g, "\u91cd\u547d\u540d", menuX, menuY + 22, 82, 20, mx, my);
            this.drawCustomButton(g, this.confirmingDelete ? "\u786e\u8ba4\u5220\u9664" : "\u5220\u9664", menuX, menuY + 44, 82, 20, mx, my);
            this.drawCustomButton(g, "管理器", menuX, menuY + 66, 82, 20, mx, my);
        }
    }

    private void drawCustomButton(GuiGraphics g, String label, int x, int y, int w, int h, int mx, int my) {
        boolean hover = this.hit(mx, my, x, y, w, h);
        int bg = hover ? -868599238 : -1440735200;
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, -2039584);
        g.fill(x, y + h - 1, x + w, y + h, -2039584);
        g.fill(x, y, x + 1, y + h, -2039584);
        g.fill(x + w - 1, y, x + w, y + h, -2039584);
        if ("BACK_GEOM".equals(label)) {
            this.drawBackGlyph(g, x, y, w, h);
        } else {
            g.drawCenteredString(this.font, (Component)Component.literal((String)label), x + w / 2, y + (h - 8) / 2, -1);
        }
    }

    private void drawBackGlyph(GuiGraphics g, int x, int y, int w, int h) {
        int color = -1;
        int cy = y + h / 2;
        int left = x + 9;
        int right = x + w - 8;
        g.fill(left, cy - 1, right, cy + 1, color);
        g.fill(left, cy - 1, left + 6, cy + 1, color);
        g.fill(left + 1, cy - 3, left + 3, cy - 1, color);
        g.fill(left + 1, cy + 1, left + 3, cy + 3, color);
        g.fill(left + 3, cy - 5, left + 5, cy - 3, color);
        g.fill(left + 3, cy + 3, left + 5, cy + 5, color);
    }

    private boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + h);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.viewingFile != null) {
            float factor = verticalAmount > 0.0 ? 1.25f : 0.8f;
            this.actionsOpen = false;
            this.setPreviewScaleAnchored(this.previewScale * factor, mouseX, mouseY);
            MinimalLog.line("preview mouseScrolled vertical=" + verticalAmount + " scale=" + this.previewScale + " anchor=" + mouseX + "," + mouseY);
            return true;
        }
        if (verticalAmount > 0.0) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 3);
            this.clampScrollOffset();
            this.rebuildWidgets();
            return true;
        }
        if (verticalAmount < 0.0) {
            this.scrollOffset = Math.min(this.maxScrollOffset(), this.scrollOffset + 3);
            this.clampScrollOffset();
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean handlePreviewControlClick(double mouseX, double mouseY) {
        int bottom;
        int controlsW;
        int right;
        int controlsX;
        if (this.renaming) {
            int panelY;
            int panelW = this.renamePanelWidth();
            int panelX = this.renamePanelX();
            if (this.hit(mouseX, mouseY, panelX + panelW - 90, (panelY = this.renamePanelY()) + 25, 40, 18)) {
                this.applyRename();
                return true;
            }
            if (this.hit(mouseX, mouseY, panelX + panelW - 46, panelY + 25, 40, 18)) {
                this.renaming = false;
                this.renameBox = null;
                this.rebuildWidgets();
                return true;
            }
        }
        if (this.hit(mouseX, mouseY, controlsX = (right = this.width - 12) - (controlsW = 150), (bottom = this.height - 12) - 20, 30, 20)) {
            this.viewRelative(-1);
            return true;
        }
        if (this.hit(mouseX, mouseY, controlsX + 36, bottom - 20, 30, 20)) {
            this.viewRelative(1);
            return true;
        }
        if (this.hit(mouseX, mouseY, controlsX + 72, bottom - 20, 46, 20)) {
            this.returnToList();
            return true;
        }
        if (this.hit(mouseX, mouseY, controlsX + 124, bottom - 20, 26, 20)) {
            if (this.renaming) {
                this.renaming = false;
                this.renameBox = null;
                this.actionsOpen = false;
            } else {
                this.actionsOpen = !this.actionsOpen;
            }
            this.confirmingDelete = false;
            this.rebuildWidgets();
            return true;
        }
        if (this.actionsOpen) {
            int menuX = right - 96;
            int menuY = bottom - 118;
            if (this.hit(mouseX, mouseY, menuX, menuY, 82, 20)) {
                Util.getPlatform().openFile(this.viewingFile.toFile());
                return true;
            }
            if (this.hit(mouseX, mouseY, menuX, menuY + 22, 82, 20)) {
                this.renaming = true;
                this.actionsOpen = false;
                this.confirmingDelete = false;
                this.rebuildWidgets();
                return true;
            }
            if (this.hit(mouseX, mouseY, menuX, menuY + 44, 82, 20)) {
                this.deleteViewingFile();
                return true;
            }
            if (this.hit(mouseX, mouseY, menuX, menuY + 66, 82, 20)) {
                Minecraft.getInstance().setScreen((Screen)new MinimalCaptureScreen(this.rootParent()));
return true;
            }
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.viewingFile == null && button == 0) {
            if (this.isScrollUpButtonHit(mouseX, mouseY)) {
                this.scrollOffset = Math.max(0, this.scrollOffset - 1);
                this.rebuildWidgets();
                return true;
            }
            if (this.isScrollDownButtonHit(mouseX, mouseY)) {
                this.scrollOffset = Math.min(this.maxScrollOffset(), this.scrollOffset + 1);
                this.rebuildWidgets();
                return true;
            }
            if (this.isScrollBarHit(mouseX, mouseY)) {
                this.draggingScrollBar = true;
                this.updateScrollOffsetFromMouse(mouseY);
                this.rebuildWidgets();
                return true;
            }
            Path clickedEntry = this.entryAtPosition(mouseX, mouseY);
            if (clickedEntry != null) {
                if (clickedEntry.equals(this.currentFolder.getParent())) {
                    this.openParentFolder();
                } else {
                    this.openEntry(clickedEntry);
                }
                return true;
            }
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.viewingFile != null && button == 0 && this.handlePreviewControlClick(mouseX, mouseY)) {
            return true;
        }
        if (this.viewingFile != null && button == 0) {
            long now = Util.getMillis();
            double distance = Math.abs(mouseX - this.lastPreviewClickX) + Math.abs(mouseY - this.lastPreviewClickY);
            if (now - this.lastPreviewClickMs <= 500L && distance <= 24.0) {
                if (this.previewScale >= 2.9f) {
                    this.resetPreviewTransform();
                } else {
                    this.setPreviewScaleAnchored(3.0f, mouseX, mouseY);
                }
                MinimalLog.line("preview double click scale=" + this.previewScale + " anchor=" + mouseX + "," + mouseY);
                this.lastPreviewClickMs = 0L;
                return true;
            }
            this.updatePreviewAnimation();
            this.transformAnimStartMs = 0L;
            this.actionsOpen = false;
            this.draggingPreview = true;
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;
            this.lastPreviewClickMs = now;
            this.lastPreviewClickX = mouseX;
            this.lastPreviewClickY = mouseY;
            this.lastPreviewInteractionMs = now;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.viewingFile == null && this.draggingScrollBar && button == 0) {
            this.updateScrollOffsetFromMouse(mouseY);
            this.rebuildWidgets();
            return true;
        }
        if (this.viewingFile != null && this.draggingPreview && button == 0) {
            this.previewPanX += (float)(mouseX - this.lastDragX);
            this.previewPanY += (float)(mouseY - this.lastDragY);
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;
            this.lastPreviewInteractionMs = Util.getMillis();
            this.clampPreviewPan();
            if (this.renaming) {
                this.rebuildWidgets();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingScrollBar) {
            this.draggingScrollBar = false;
            this.rebuildWidgets();
            return true;
        }
        if (button == 0 && this.draggingPreview) {
            this.draggingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private String stripPng(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".png") ? name.substring(0, name.length() - 4) : name;
    }

    private String sanitizeFileName(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            if (c == '\\' || c == '/' || c == ':' || c == '*' || c == '?' || c == '\"' || c == '<' || c == '>' || c == '|') {
                builder.append('_');
                continue;
            }
            builder.append(c);
        }
        String result = builder.toString().trim();
        return result.isEmpty() ? "itrp_minimal" : result;
    }

    private void applyRename() {
        block7: {
            if (this.viewingFile == null || this.renameBox == null) {
                return;
            }
            String raw = this.renameBox.getValue().trim();
            if (raw.isEmpty()) {
                return;
            }
            Object safe = this.sanitizeFileName(raw);
            if (!((String)safe).toLowerCase(Locale.ROOT).endsWith(".png")) {
                safe = (String)safe + ".png";
            }
            Path target = this.viewingFile.resolveSibling((String)safe);
            try {
                if (Files.exists(target, new LinkOption[0])) {
                    if (this.minecraft != null && this.minecraft.gui != null) {
                        this.minecraft.gui.getChat().addMessage((Component)Component.literal((String)"截图管理器: \u6587\u4ef6\u540d\u5df2\u5b58\u5728"));
                    }
                    return;
                }
                Path old = this.viewingFile;
                this.releaseTexture();
                Files.move(old, target, new CopyOption[0]);
                this.scanFiles();
                this.renaming = false;
                this.actionsOpen = false;
                this.renameBox = null;
                this.loadAndView(target);
                this.titleToastUntilMs = Util.getMillis() + 2500L;
                this.scaleToastUntilMs = 0L;
                MinimalLog.line("preview renamed " + String.valueOf(old.getFileName()) + " -> " + String.valueOf(target.getFileName()));
            }
            catch (Exception e) {
                MinimalLog.line("preview rename failed " + String.valueOf(this.viewingFile) + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (this.minecraft == null || this.minecraft.gui == null) break block7;
                this.minecraft.gui.getChat().addMessage((Component)Component.literal((String)("截图管理器: \u91cd\u547d\u540d\u5931\u8d25 " + e.getMessage())));
            }
        }
    }

    private void deleteViewingFile() {
        block5: {
            if (this.viewingFile == null) {
                return;
            }
            if (!this.confirmingDelete) {
                this.confirmingDelete = true;
                this.rebuildWidgets();
                return;
            }
            Path deleting = this.viewingFile;
            int index = this.files.indexOf(deleting);
            try {
                this.releaseTexture();
                Files.deleteIfExists(deleting);
                this.scanFiles();
                if (this.files.isEmpty()) {
                    this.returnToList();
                    return;
                }
                int next = Math.max(0, Math.min(index, this.files.size() - 1));
                this.loadAndView(this.files.get(next));
                this.titleToastUntilMs = Util.getMillis() + 2500L;
                this.scaleToastUntilMs = 0L;
            }
            catch (Exception e) {
                MinimalLog.line("preview delete failed " + String.valueOf(deleting) + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (this.minecraft == null || this.minecraft.gui == null) break block5;
                this.minecraft.gui.getChat().addMessage((Component)Component.literal((String)("截图管理器: \u5220\u9664\u5931\u8d25 " + e.getMessage())));
            }
        }
    }

    private void updatePreviewAnimation() {
        if (this.transformAnimStartMs <= 0L || this.transformAnimDurationMs <= 0L) {
            return;
        }
        long now = Util.getMillis();
        float t = Math.min(1.0f, Math.max(0.0f, (float)(now - this.transformAnimStartMs) / (float)this.transformAnimDurationMs));
        float eased = t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
        this.previewScale = this.lerp(this.animFromScale, this.animToScale, eased);
        this.previewPanX = this.lerp(this.animFromPanX, this.animToPanX, eased);
        this.previewPanY = this.lerp(this.animFromPanY, this.animToPanY, eased);
        if (t >= 1.0f) {
            this.previewScale = this.animToScale;
            this.previewPanX = this.animToPanX;
            this.previewPanY = this.animToPanY;
            this.transformAnimStartMs = 0L;
        }
    }

    private float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private void animatePreviewTransform(float targetScale, float targetPanX, float targetPanY) {
        this.updatePreviewAnimation();
        this.animFromScale = this.previewScale;
        this.animFromPanX = this.previewPanX;
        this.animFromPanY = this.previewPanY;
        this.animToScale = targetScale;
        this.animToPanX = targetPanX;
        this.animToPanY = targetPanY;
        this.transformAnimStartMs = Util.getMillis();
        this.transformAnimDurationMs = 220L;
    }

    private void setPreviewScale(float value) {
        this.setPreviewScaleAnchored(value, (double)this.width / 2.0, (double)this.height / 2.0);
    }

    private void setPreviewScaleAnchored(float value, double anchorX, double anchorY) {
        this.updatePreviewAnimation();
        float oldScale = this.previewScale;
        float newScale = this.clampPreviewScale(value);
        if (oldScale <= 0.0f) {
            this.previewScale = newScale;
            return;
        }
        double oldCenterX = (double)this.width / 2.0 + (double)this.previewPanX;
        double oldCenterY = (double)this.height / 2.0 - 6.0 + (double)this.previewPanY;
        double imageX = (anchorX - oldCenterX) / (double)oldScale;
        double imageY = (anchorY - oldCenterY) / (double)oldScale;
        float targetPanX = (float)(anchorX - (double)this.width / 2.0 - imageX * (double)newScale);
        float targetPanY = (float)(anchorY - ((double)this.height / 2.0 - 6.0) - imageY * (double)newScale);
        float oldPanX = this.previewPanX;
        float oldPanY = this.previewPanY;
        this.previewScale = newScale;
        this.previewPanX = targetPanX;
        this.previewPanY = targetPanY;
        this.clampPreviewPan();
        targetPanX = this.previewPanX;
        targetPanY = this.previewPanY;
        this.previewScale = oldScale;
        this.previewPanX = oldPanX;
        this.previewPanY = oldPanY;
        this.animatePreviewTransform(newScale, targetPanX, targetPanY);
        this.lastPreviewInteractionMs = Util.getMillis();
        this.scaleToastUntilMs = Util.getMillis() + 1500L;
        if (this.renaming) {
            this.rebuildWidgets();
        }
        MinimalLog.line("preview animated scale target " + newScale + " pan=" + targetPanX + "," + targetPanY);
    }

    private void resetPreviewTransform() {
        this.animatePreviewTransform(1.0f, 0.0f, 0.0f);
        this.lastPreviewInteractionMs = Util.getMillis();
        this.scaleToastUntilMs = Util.getMillis() + 1500L;
        if (this.renaming) {
            this.rebuildWidgets();
        }
        MinimalLog.line("preview animated reset transform");
    }

    private void clampPreviewPan() {
        float limitX = Math.max(0.0f, (float)this.width * this.previewScale);
        float limitY = Math.max(0.0f, (float)this.height * this.previewScale);
        this.previewPanX = Math.max(-limitX, Math.min(limitX, this.previewPanX));
        this.previewPanY = Math.max(-limitY, Math.min(limitY, this.previewPanY));
    }

    private float clampPreviewScale(float value) {
        return Math.max(0.25f, Math.min(16.0f, value));
    }
}

