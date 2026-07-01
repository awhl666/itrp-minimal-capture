package com.itrpminimal.capture;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

public final class MinimalShaderManager {
    private static final String[] OFFLINE_OPTIONS = {
            "RENDERING_MODE",
            "ITRP_SOFT_PAUSE_OFF",
            "RENDERING_MODE_COOLDOWN",
            "CLOUD_SPEED",
            "WAVE_SPEED",
            "WAVING_SPEED",
            "RAIN_SPLASH_SPEED"
    };

    private static String selectedPack = null;
    private static String previousPack = null;
    private static boolean previousShadersEnabled = true;
    private static boolean offlinePrepared = false;
    private static OptionSnapshot optionSnapshot = null;

    public static String selectedPack() {
        return selectedPack;
    }

    public static boolean isSelectedIterationRp() {
        return isIterationRpName(selectedPack);
    }

    public static boolean isIterationRpName(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(java.util.Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
        return normalized.contains("iterationrp");
    }

    public static void setSelectedPack(String packName) {
        selectedPack = normalizePackName(packName);
        MinimalLog.line("SHADER selected=" + selectedPack);
    }

    public static List<String> listShaderPacks(Minecraft client) {
        List<String> names = new ArrayList<>();
        Path dir = shaderpacksDirectory(client);
        try {
            Files.createDirectories(dir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    String name = entry.getFileName().toString();
                    if (name.startsWith(".")) {
                        continue;
                    }
                    if (name.endsWith(".zip") || Files.isDirectory(entry)) {
                        names.add(name);
                    }
                }
            }
        } catch (IOException e) {
            MinimalLog.line("SHADER list failed " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        names.sort(Comparator.naturalOrder());
        return names;
    }

    public static void applySelected() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            try {
                if (selectedPack == null || selectedPack.isBlank()) {
                    say(client, "请先选择光影包");
                    return;
                }
                applyPack(client, selectedPack, true);
                say(client, "已应用光影: " + selectedPack);
            } catch (Throwable t) {
                MinimalLog.line("SHADER apply failed " + t.getClass().getName() + ": " + t.getMessage());
                say(client, "应用光影失败: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        });
    }

    public static void reloadIris() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            try {
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                iris.getMethod("reload").invoke(null);
                MinimalLog.line("SHADER iris reload requested");
                say(client, "已请求 Iris 重载");
            } catch (Throwable t) {
                MinimalLog.line("SHADER reload failed " + t.getClass().getName() + ": " + t.getMessage());
                say(client, "Iris 重载失败: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        });
    }

    public static String currentSummary(Minecraft client) {
        try {
            return "当前=" + currentPack(client) + " 已启用=" + shadersEnabled(client) + " 已选择=" + selectedPack + " 离线已准备=" + offlinePrepared;
        } catch (Throwable t) {
            return "光影状态读取失败: " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    public static boolean prepareOfflineSelected(Minecraft client, int cooldownSeconds) throws Exception {
if (selectedPack == null || selectedPack.isBlank()) {
throw new IOException("请先选择光影包");
}
previousPack = currentPack(client);
previousShadersEnabled = shadersEnabled(client);
optionSnapshot = OptionSnapshot.capture(client, selectedPack, OFFLINE_OPTIONS);
optionSnapshot.savePersistent(client);
writeAndQueueOption(client, selectedPack, "RENDERING_MODE", "true");
writeAndQueueOption(client, selectedPack, "ITRP_SOFT_PAUSE_OFF", "true");
writeAndQueueOption(client, selectedPack, "RENDERING_MODE_COOLDOWN", String.format(java.util.Locale.ROOT, "%.1f", Math.max(0.5F, Math.min(60.0F, cooldownSeconds))));
writeAndQueueOption(client, selectedPack, "CLOUD_SPEED", "0.0");
writeAndQueueOption(client, selectedPack, "WAVE_SPEED", "0.0");
writeAndQueueOption(client, selectedPack, "WAVING_SPEED", "0.0");
writeAndQueueOption(client, selectedPack, "RAIN_SPLASH_SPEED", "0.0");
applyPack(client, selectedPack, true);
offlinePrepared = true;
MinimalLog.line("OFFLINE SHADER prepared selected=" + selectedPack + " previous=" + previousPack + " previousEnabled=" + previousShadersEnabled);
MinimalLog.line("OFFLINE SHADER preset=" + debugPreset(client, selectedPack));
return true;
}

    public static void restoreOffline(Minecraft client) {
        restoreOffline(client, true);
    }

    public static void restoreOffline(Minecraft client, boolean restorePreviousPack) {
        try {
            if (optionSnapshot != null) {
                optionSnapshot.restore(client);
                optionSnapshot = null;
                OptionSnapshot.deletePersistent(client);
            } else {
                OptionSnapshot persisted = OptionSnapshot.loadPersistent(client);
                if (persisted != null) {
                    persisted.restore(client);
                    OptionSnapshot.deletePersistent(client);
                }
            }
            if (offlinePrepared) {
                if (restorePreviousPack) {
                    String restorePack = previousPack == null || previousPack.isBlank() ? selectedPack : previousPack;
                    applyPack(client, restorePack, previousShadersEnabled);
                    MinimalLog.line("OFFLINE SHADER restored pack=" + restorePack + " enabled=" + previousShadersEnabled);
                } else {
                    applyPack(client, selectedPack, true);
                    MinimalLog.line("OFFLINE SHADER kept selected pack=" + selectedPack + " enabled=true previous=" + previousPack + " previousEnabled=" + previousShadersEnabled);
                }
            }
        } catch (Throwable t) {
            MinimalLog.line("OFFLINE SHADER restore failed " + t.getClass().getName() + ": " + t.getMessage());
            try {
                if (restorePreviousPack && previousPack != null && !previousPack.isBlank()) {
                    writeShaderPack(irisProperties(client), previousPack, previousShadersEnabled);
                }
            } catch (Throwable ignored) {
            }
        } finally {
            previousPack = null;
            previousShadersEnabled = true;
            offlinePrepared = false;
        }
    }

    public static boolean offlinePrepared() {
        return offlinePrepared;
    }
    public static void cleanupResidualOfflineState(Minecraft client) {
        try {
            OptionSnapshot persisted = OptionSnapshot.loadPersistent(client);
            if (persisted != null) {
                persisted.restore(client);
                OptionSnapshot.deletePersistent(client);
                optionSnapshot = null;
                MinimalLog.line("SHADER startup restored persisted offline option snapshot");
            } else {
                MinimalLog.line("SHADER startup cleanup skipped preset writes because no persisted snapshot exists");
            }
            offlinePrepared = false;
            previousPack = null;
            previousShadersEnabled = true;
        } catch (Throwable t) {
            MinimalLog.line("SHADER startup cleanup failed " + t.getClass().getName() + ": " + t.getMessage());
        }
    }


    private static void applyPack(Minecraft client, String shaderPack, boolean enabled) throws Exception {
        writeShaderPack(irisProperties(client), shaderPack, enabled);
        Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
        Object config = iris.getMethod("getIrisConfig").invoke(null);
        Method setShaderPackName = config.getClass().getMethod("setShaderPackName", String.class);
        setShaderPackName.invoke(config, shaderPack);
        Method setShadersEnabled = config.getClass().getMethod("setShadersEnabled", boolean.class);
        setShadersEnabled.invoke(config, enabled);
        Method save = config.getClass().getMethod("save");
        save.invoke(config);
        iris.getMethod("reload").invoke(null);
        MinimalLog.line("SHADER applied pack=" + shaderPack + " enabled=" + enabled);
    }

    private static String currentPack(Minecraft client) throws IOException {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object config = iris.getMethod("getIrisConfig").invoke(null);
            Object optional = config.getClass().getMethod("getShaderPackName").invoke(config);
            Object value = optional.getClass().getMethod("orElse", Object.class).invoke(optional, readShaderPack(irisProperties(client)));
            return value == null ? null : value.toString();
        } catch (Throwable e) {
            return readShaderPack(irisProperties(client));
        }
    }

    private static boolean shadersEnabled(Minecraft client) throws IOException {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object config = iris.getMethod("getIrisConfig").invoke(null);
            Object enabled = config.getClass().getMethod("areShadersEnabled").invoke(config);
            return Boolean.TRUE.equals(enabled);
        } catch (Throwable e) {
            return readShadersEnabled(irisProperties(client));
        }
    }

    private static Path shaderpacksDirectory(Minecraft client) {
        return client.gameDirectory.toPath().resolve("shaderpacks");
    }

    private static Path irisProperties(Minecraft client) {
        return client.gameDirectory.toPath().resolve("config").resolve("iris.properties");
    }

    private static Path persistentSnapshotPath(Minecraft client) {
        return client.gameDirectory.toPath().resolve("render_assistant_minimal").resolve("shader_option_snapshot.tsv");
    }

    private static String encodeSnapshotField(String value) {
        String safe = value == null ? "" : value;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeSnapshotField(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String readShaderPack(Path config) throws IOException {
        if (!Files.exists(config)) {
            return null;
        }
        for (String line : Files.readAllLines(config)) {
            if (line.startsWith("shaderPack=")) {
                return line.substring("shaderPack=".length());
            }
        }
        return null;
    }

    private static boolean readShadersEnabled(Path config) throws IOException {
        if (!Files.exists(config)) {
            return true;
        }
        for (String line : Files.readAllLines(config)) {
            if (line.startsWith("enableShaders=")) {
                return Boolean.parseBoolean(line.substring("enableShaders=".length()));
            }
        }
        return true;
    }

    private static void writeShaderPack(Path config, String shaderPack, boolean enabled) throws IOException {
        Files.createDirectories(config.getParent());
        List<String> lines = Files.exists(config) ? Files.readAllLines(config) : new ArrayList<>();
        boolean packWritten = false;
        boolean enabledWritten = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("shaderPack=")) {
                lines.set(i, "shaderPack=" + shaderPack);
                packWritten = true;
            } else if (line.startsWith("enableShaders=")) {
                lines.set(i, "enableShaders=" + enabled);
                enabledWritten = true;
            }
        }
        if (!enabledWritten) {
            lines.add("enableShaders=" + enabled);
        }
        if (!packWritten) {
            lines.add("shaderPack=" + shaderPack);
        }
        Files.write(config, lines);
    }

    private static void writeAndQueueOption(Minecraft client, String shaderPack, String name, String value) {
        try {
            queueShaderOption(name, value);
        } catch (Throwable t) {
            MinimalLog.line("SHADER option queue failed " + name + "=" + value + " " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        writeShaderOptionPreset(client, shaderPack, name, value);
    }

    private static void queueShaderOption(String name, String value) throws Exception {
        Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
        Object queue = iris.getMethod("getShaderPackOptionQueue").invoke(null);
        Method put = queue.getClass().getMethod("put", Object.class, Object.class);
        put.invoke(queue, name, value);
        MinimalLog.line("SHADER option queued " + name + "=" + value);
    }

    private static void writeShaderOptionPreset(Minecraft client, String shaderPack, String name, String value) {
        try {
            Path preset = shaderpacksDirectory(client).resolve(shaderPack + ".txt");
            List<String> lines = Files.exists(preset) ? Files.readAllLines(preset) : new ArrayList<>();
            boolean replaced = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(name + "=")) {
                    lines.set(i, name + "=" + value);
                    replaced = true;
                }
            }
            if (!replaced) {
                lines.add(name + "=" + value);
            }
            Files.write(preset, lines);
            MinimalLog.line("SHADER preset written " + preset.getFileName() + " " + name + "=" + value);
        } catch (Throwable t) {
            MinimalLog.line("SHADER preset write failed " + name + " " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static String readShaderOptionPreset(Minecraft client, String shaderPack, String name) {
        try {
            Path preset = shaderpacksDirectory(client).resolve(shaderPack + ".txt");
            if (!Files.exists(preset)) {
                return null;
            }
            for (String line : Files.readAllLines(preset)) {
                if (line.startsWith(name + "=")) {
                    return line.substring((name + "=").length());
                }
            }
        } catch (Throwable t) {
            MinimalLog.line("SHADER preset read failed " + name + " " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        return null;
    }

    private static void removeShaderOptionPreset(Minecraft client, String shaderPack, String name) {
        try {
            Path preset = shaderpacksDirectory(client).resolve(shaderPack + ".txt");
            if (!Files.exists(preset)) {
                return;
            }
            List<String> oldLines = Files.readAllLines(preset);
            List<String> newLines = new ArrayList<>();
            for (String line : oldLines) {
                if (!line.startsWith(name + "=")) {
                    newLines.add(line);
                }
            }
            Files.write(preset, newLines);
            MinimalLog.line("SHADER preset removed " + preset.getFileName() + " " + name);
        } catch (Throwable t) {
            MinimalLog.line("SHADER preset remove failed " + name + " " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private static String debugPreset(Minecraft client, String shaderPack) {
        try {
            Path preset = shaderpacksDirectory(client).resolve(shaderPack + ".txt");
            if (!Files.exists(preset)) {
                return preset.getFileName() + " <missing>";
            }
            return preset.getFileName() + " :: " + String.join(" | ", Files.readAllLines(preset));
        } catch (Throwable t) {
            return "debugPreset failed " + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    private static String normalizePackName(String packName) {
        if (packName == null || packName.isBlank()) {
            return null;
        }
        return packName.trim();
    }

    private static final class OptionSnapshot {
        private final String shaderPack;
        private final List<OptionValue> values;

        private OptionSnapshot(String shaderPack, List<OptionValue> values) {
            this.shaderPack = shaderPack;
            this.values = values;
        }

        private static OptionSnapshot capture(Minecraft client, String shaderPack, String[] optionNames) {
            List<OptionValue> captured = new ArrayList<>();
            for (String name : optionNames) {
                String value = readShaderOptionPreset(client, shaderPack, name);
                captured.add(new OptionValue(name, value, value != null));
            }
            return new OptionSnapshot(shaderPack, captured);
        }

        private static OptionSnapshot loadPersistent(Minecraft client) {
            Path path = persistentSnapshotPath(client);
            if (!Files.exists(path)) {
                return null;
            }
            try {
                String shaderPack = null;
                List<OptionValue> captured = new ArrayList<>();
                for (String line : Files.readAllLines(path)) {
                    if (line == null || line.isBlank()) {
                        continue;
                    }
                    String[] parts = line.split("\t", -1);
                    if (parts.length >= 2 && "shaderPack".equals(parts[0])) {
                        shaderPack = decodeSnapshotField(parts[1]);
                    } else if (parts.length >= 4 && "option".equals(parts[0])) {
                        String name = decodeSnapshotField(parts[1]);
                        boolean existed = Boolean.parseBoolean(parts[2]);
                        String value = decodeSnapshotField(parts[3]);
                        captured.add(new OptionValue(name, value, existed));
                    }
                }
                if (shaderPack == null || shaderPack.isBlank() || captured.isEmpty()) {
                    MinimalLog.line("SHADER persistent snapshot ignored incomplete path=" + path);
                    return null;
                }
                return new OptionSnapshot(shaderPack, captured);
            } catch (Throwable t) {
                MinimalLog.line("SHADER persistent snapshot load failed " + t.getClass().getSimpleName() + ": " + t.getMessage());
                return null;
            }
        }

        private void savePersistent(Minecraft client) {
            Path path = persistentSnapshotPath(client);
            try {
                Files.createDirectories(path.getParent());
                List<String> lines = new ArrayList<>();
                lines.add("shaderPack	" + encodeSnapshotField(shaderPack));
                for (OptionValue value : values) {
                    lines.add("option	" + encodeSnapshotField(value.name) + "	" + value.existed + "	" + encodeSnapshotField(value.value));
                }
                Files.write(path, lines);
                MinimalLog.line("SHADER persistent snapshot saved " + path);
            } catch (Throwable t) {
                MinimalLog.line("SHADER persistent snapshot save failed " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        private static void deletePersistent(Minecraft client) {
            try {
                Files.deleteIfExists(persistentSnapshotPath(client));
            } catch (Throwable t) {
                MinimalLog.line("SHADER persistent snapshot delete failed " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }

        private void restore(Minecraft client) {
            for (OptionValue value : values) {
                if (value.existed) {
                    writeAndQueueOption(client, shaderPack, value.name, value.value);
                } else {
                    removeShaderOptionPreset(client, shaderPack, value.name);
                }
            }
            MinimalLog.line("SHADER option snapshot restored pack=" + shaderPack);
        }
    }

    private static final class OptionValue {
        private final String name;
        private final String value;
        private final boolean existed;

        private OptionValue(String name, String value, boolean existed) {
            this.name = name;
            this.value = value;
            this.existed = existed;
        }
    }

    private static void say(Minecraft client, String text) {
        if (client != null && client.gui != null) {
            client.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("截图管理器: " + text));
        }
    }

    private MinimalShaderManager() {
    }
}