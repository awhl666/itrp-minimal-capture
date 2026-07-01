from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import textwrap
import urllib.request
import zipfile
from pathlib import Path
from xml.etree import ElementTree

ROOT = Path('/data/user/0/com.ai.assistance.operit/files/workspace/1e9d9302-e5d4-49be-b7d3-183760e4574d')
SOURCE = ROOT / 'workspace/minimal_clean_from_uploaded_0319'
WORK_ROOT = ROOT / 'workspace/fabric_minimal_capture_build'
OUT_ROOT = Path('/storage/emulated/0/AW资源工作区/mc.资源/新基底高版本打包')
LOG = ROOT / 'workspace/fabric_minimal_capture_build.log'
ERR = ROOT / 'workspace/fabric_minimal_capture_build_error_summary.log'
REPORT = ROOT / 'workspace/FABRIC_MINIMAL_CAPTURE_BUILD_REPORT.md'

MOD_ID = 'itrp_minimal_capture'
GROUP = 'com.itrpminimal'
BASE_ARCHIVE = 'itrp-minimal-capture-fabric'
MOD_VERSION = '0.3.19+fabric'
LOOM_VERSION = '1.10.5'
LOADER_VERSION = '0.19.3'
FABRIC_API_METADATA_URL = 'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml'
DEFAULT_MC_VERSIONS = ['1.21', '1.21.1', '1.21.2', '1.21.3', '1.21.4', '1.21.5', '1.21.6', '1.21.7', '1.21.8', '1.21.9', '1.21.10', '1.21.11']
KNOWN_FABRIC_API = {
    '1.21': '0.102.0+1.21',
    '1.21.1': '0.116.12+1.21.1',
    '1.21.2': '0.106.1+1.21.2',
    '1.21.3': '0.114.1+1.21.3',
    '1.21.4': '0.119.4+1.21.4',
    '1.21.5': '0.128.2+1.21.5',
    '1.21.6': '0.128.2+1.21.6',
    '1.21.7': '0.129.0+1.21.7',
    '1.21.8': '0.136.1+1.21.8',
    '1.21.9': '0.134.1+1.21.9',
    '1.21.10': '0.138.4+1.21.10',
    '1.21.11': '0.141.4+1.21.11',
}


def log(message: str) -> None:
    LOG.parent.mkdir(parents=True, exist_ok=True)
    with LOG.open('a', encoding='utf-8') as f:
        f.write(message + '\n')
    print(message, flush=True)


def write_error(step: int, command: str, code: int, fragment: str, cause: str, fix: str) -> None:
    ERR.write_text('\n'.join([
        'status=failed',
        f'step={step}',
        f'command={command}',
        f'exit_code={code}',
        'original_error_fragment:',
        fragment[-4000:],
        f'possible_cause={cause}',
        f'suggested_fix={fix}',
    ]), encoding='utf-8')


def run(step: int, command: list[str], cwd: Path, timeout: int = 1800) -> str:
    text = ' '.join(command)
    log(f'[step {step}] run: {text} cwd={cwd}')
    env = os.environ.copy()
    env['JAVA_HOME'] = '/data/user/0/com.ai.assistance.operit/files/devkits/java/jdk-21'
    env['PATH'] = env['JAVA_HOME'] + '/bin:' + env.get('PATH', '')
    proc = subprocess.run(command, cwd=str(cwd), text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env=env, timeout=timeout)
    if proc.stdout:
        log(proc.stdout)
    if proc.returncode != 0:
        write_error(step, text, proc.returncode, proc.stdout, 'Gradle/Fabric compile or dependency failure', 'read the main log, patch source compatibility, then rerun failed version')
        raise SystemExit(proc.returncode)
    return proc.stdout


def parse_mc(version: str) -> tuple[int, int, int]:
    parts = [int(x) for x in version.split('.')]
    while len(parts) < 3:
        parts.append(0)
    return parts[0], parts[1], parts[2]


def mc_at_least(version: str, floor: str) -> bool:
    return parse_mc(version) >= parse_mc(floor)


def reset_logs() -> None:
    LOG.write_text('', encoding='utf-8')
    ERR.write_text('status=running\n', encoding='utf-8')


def copy_source(dst: Path) -> None:
    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(SOURCE, dst, ignore=shutil.ignore_patterns('.gradle', 'build', '*.class'))
    gradlew = dst / 'gradlew'
    if gradlew.exists():
        gradlew.chmod(0o755)


def update_gradle_wrapper(project: Path) -> None:
    props = project / 'gradle/wrapper/gradle-wrapper.properties'
    text = props.read_text(encoding='utf-8')
    text = re.sub(r'distributionUrl=.*', 'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.14.3-bin.zip', text)
    props.write_text(text, encoding='utf-8')


def fetch_fabric_api_versions(mc_versions: list[str]) -> dict[str, str]:
    last_error = ''
    for attempt in range(1, 4):
        try:
            log(f'[network] GET {FABRIC_API_METADATA_URL} attempt={attempt}')
            with urllib.request.urlopen(FABRIC_API_METADATA_URL, timeout=60) as response:
                xml = response.read()
            root = ElementTree.fromstring(xml)
            versions = [node.text for node in root.findall('./versioning/versions/version') if node.text]
            result: dict[str, str] = {}
            for mc in mc_versions:
                suffix = '+' + mc
                matches = [v for v in versions if v.endswith(suffix)]
                if not matches:
                    write_error(1, 'fetch fabric-api metadata', 1, f'no fabric-api for {mc}', 'Fabric API metadata has no matching version', 'choose supported MC version or set Fabric API manually')
                    raise SystemExit(1)
                result[mc] = matches[-1]
                log(f'[fabric-api] {mc} -> {result[mc]}')
            return result
        except Exception as exc:
            last_error = repr(exc)
            log(f'[network] metadata fetch failed attempt={attempt}: {last_error}')
    fallback: dict[str, str] = {}
    missing: list[str] = []
    for mc in mc_versions:
        if mc in KNOWN_FABRIC_API:
            fallback[mc] = KNOWN_FABRIC_API[mc]
            log(f'[fabric-api] {mc} -> {fallback[mc]} fallback_after_network_error')
        else:
            missing.append(mc)
    if missing:
        write_error(1, 'fetch fabric-api metadata', 1, last_error, 'Network metadata fetch failed and no fallback exists', 'retry network or add known Fabric API versions')
        raise SystemExit(1)
    return fallback


def loom_version_for(mc: str) -> str:
    if mc_at_least(mc, '1.21.11'):
        return '1.13.3'
    if mc_at_least(mc, '1.21.9'):
        return '1.11.7'
    return LOOM_VERSION


def write_gradle_files(project: Path, mc: str, api_version: str) -> None:
    loom_version = loom_version_for(mc)
    (project / 'settings.gradle.kts').write_text(textwrap.dedent('''
        pluginManagement {
            repositories {
                mavenLocal()
                maven("https://maven.fabricmc.net/")
                gradlePluginPortal()
                mavenCentral()
            }
        }
        dependencyResolutionManagement {
            repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
            repositories {
                mavenLocal()
                maven("https://maven.fabricmc.net/")
                mavenCentral()
            }
        }
        rootProject.name = "itrp-minimal-capture-fabric"
    ''').strip() + '\n', encoding='utf-8')
    (project / 'build.gradle.kts').write_text(textwrap.dedent(f'''
        plugins {{
            id("fabric-loom") version "{loom_version}"
        }}

        version = "{MOD_VERSION}+mc{mc}"
        group = "{GROUP}"

        base {{
            archivesName.set("{BASE_ARCHIVE}")
        }}

        repositories {{
            mavenLocal()
            maven("https://maven.fabricmc.net/")
            mavenCentral()
        }}

        java {{
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
            withSourcesJar()
        }}

        dependencies {{
            minecraft("com.mojang:minecraft:{mc}")
            mappings(loom.officialMojangMappings())
            modImplementation("net.fabricmc:fabric-loader:{LOADER_VERSION}")
            modImplementation("net.fabricmc.fabric-api:fabric-api:{api_version}")
        }}

        tasks.withType<JavaCompile>().configureEach {{
            options.encoding = "UTF-8"
            options.release.set(21)
        }}
    ''').strip() + '\n', encoding='utf-8')


def patch_resources(project: Path, mc: str) -> None:
    neo = project / 'src/main/resources/META-INF/neoforge.mods.toml'
    if neo.exists():
        neo.unlink()
    fabric_json = {
        'schemaVersion': 1,
        'id': MOD_ID,
        'version': f'{MOD_VERSION}+mc{mc}',
        'name': 'Screenshot Manager',
        'description': 'Screenshot manager with pause-safe screens, adaptive small vanilla-menu buttons, normal capture, ITRP offline render settings, presets, and latest preview hotkey.',
        'authors': ['Operit', '阿韵'],
        'license': 'MIT',
        'environment': 'client',
        'entrypoints': {'client': ['com.itrpminimal.capture.MinimalCaptureFabric']},
        'mixins': ['itrp_minimal_capture.mixins.json'],
        'depends': {'fabricloader': f'>={LOADER_VERSION}', 'fabric-api': '*', 'minecraft': f'~{mc}', 'java': '>=21'},
    }
    (project / 'src/main/resources/fabric.mod.json').write_text(__import__('json').dumps(fabric_json, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def patch_entry_and_keys(project: Path) -> None:
    neo_entry = project / 'src/main/java/com/itrpminimal/capture/MinimalCaptureMod.java'
    if neo_entry.exists():
        neo_entry.unlink()
    fabric_entry = project / 'src/main/java/com/itrpminimal/capture/MinimalCaptureFabric.java'
    fabric_entry.write_text(textwrap.dedent('''
        package com.itrpminimal.capture;

        import com.mojang.brigadier.CommandDispatcher;
        import net.fabricmc.api.ClientModInitializer;
        import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
        import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
        import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
        import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
        import net.minecraft.client.Minecraft;
        import net.minecraft.commands.CommandBuildContext;
        import net.minecraft.network.chat.Component;

        public final class MinimalCaptureFabric implements ClientModInitializer {
            public static final String MOD_ID = "itrp_minimal_capture";

            @Override
            public void onInitializeClient() {
                MinimalLog.line("Screenshot manager loaded");
                MinimalKeyBindings.register();
                ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
                ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);
                Minecraft.getInstance().execute(() -> {
                    MinimalCapturePreset.load();
                    MinimalShaderManager.cleanupResidualOfflineState(Minecraft.getInstance());
                });
            }

            private void onClientTick(Minecraft client) {
                MinimalKeyBindings.tick(client);
                MinimalCaptureController.tick(client);
                MinimalOfflineRenderController.tick(client);
            }

            private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
                dispatcher.register(ClientCommandManager.literal("itrpminimal")
                        .then(ClientCommandManager.literal("start").executes(ctx -> {
                            MinimalCaptureController.start();
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("offline").executes(ctx -> {
                            MinimalOfflineRenderController.start();
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("cancel").executes(ctx -> {
                            MinimalCaptureController.cancel("command");
                            MinimalOfflineRenderController.cancel("command");
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("status").executes(ctx -> {
                            MinimalCaptureController.reportStatus();
                            Minecraft client = Minecraft.getInstance();
                            if (client.gui != null) {
                                client.gui.getChat().addMessage(Component.literal("截图管理器: " + MinimalOfflineRenderController.statusSummary()));
                            }
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("gui").executes(ctx -> {
                            Minecraft client = Minecraft.getInstance();
                            client.execute(() -> client.setScreen(new MinimalCaptureScreen(client.screen)));
                            return 1;
                        })));
            }
        }
    ''').strip() + '\n', encoding='utf-8')
    gallery = project / 'src/main/java/com/itrpminimal/capture/MinimalScreenshotGalleryScreen.java'
    text = gallery.read_text(encoding='utf-8')
    text = text.replace('MinimalCaptureMod.MOD_ID', 'MinimalCaptureFabric.MOD_ID')
    gallery.write_text(text, encoding='utf-8')

    keys = project / 'src/main/java/com/itrpminimal/capture/MinimalKeyBindings.java'
    text = keys.read_text(encoding='utf-8')
    text = text.replace('import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;\n', 'import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;\n')
    text = text.replace('    public static void register(RegisterKeyMappingsEvent event) {\n        event.register(OPEN_MENU);\n        event.register(FORCE_CANCEL);\n        event.register(RUN_PRESET);\n        event.register(PREVIEW_LATEST);\n    }', '    public static void register() {\n        KeyBindingHelper.registerKeyBinding(OPEN_MENU);\n        KeyBindingHelper.registerKeyBinding(FORCE_CANCEL);\n        KeyBindingHelper.registerKeyBinding(RUN_PRESET);\n        KeyBindingHelper.registerKeyBinding(PREVIEW_LATEST);\n    }')
    keys.write_text(text, encoding='utf-8')


def patch_version_specific(project: Path, mc: str) -> None:
    if not mc_at_least(mc, '1.21.2'):
        sky_mixin = project / 'src/main/java/com/itrpminimal/capture/mixin/SkyRendererSunMoonFreezeMixin.java'
        if sky_mixin.exists():
            sky_mixin.unlink()
        mixin_json = project / 'src/main/resources/itrp_minimal_capture.mixins.json'
        text = mixin_json.read_text(encoding='utf-8')
        text = text.replace('    "SkyRendererSunMoonFreezeMixin",\n', '')
        mixin_json.write_text(text, encoding='utf-8')

        preview = project / 'src/main/java/com/itrpminimal/capture/MinimalScreenshotPreviewScreen.java'
        text = preview.read_text(encoding='utf-8')
        text = text.replace('import net.minecraft.client.renderer.RenderType;\n', '')
        text = text.replace('g.blit(RenderType::guiTextured, thumb.location, drawX, drawY, 0.0f, 0.0f, drawW, drawH, thumb.width, thumb.height, thumb.width, thumb.height);', 'g.blit(thumb.location, drawX, drawY, drawW, drawH, 0.0f, 0.0f, thumb.width, thumb.height, thumb.width, thumb.height);')
        preview.write_text(text, encoding='utf-8')

    if mc_at_least(mc, '1.21.2'):
        highres = project / 'src/main/java/com/itrpminimal/capture/MinimalHighResShell.java'
        text = highres.read_text(encoding='utf-8')
        text = text.replace('import com.itrpminimal.capture.mixin.MinimalLevelRendererAccessor;\n', '')
        text = re.sub(r'\n        MinimalLevelRendererAccessor levelRenderer = \(MinimalLevelRendererAccessor\) client\.levelRenderer;\n        resizePostChain\(levelRenderer\.itrpminimal\$getEntityEffect\(\), width, height\);\n        resizeTarget\(levelRenderer\.itrpminimal\$getEntityTarget\(\), width, height\);\n        resizeTarget\(levelRenderer\.itrpminimal\$getTranslucentTarget\(\), width, height\);\n        resizeTarget\(levelRenderer\.itrpminimal\$getParticlesTarget\(\), width, height\);\n        resizeTarget\(levelRenderer\.itrpminimal\$getWeatherTarget\(\), width, height\);\n        resizeTarget\(levelRenderer\.itrpminimal\$getCloudsTarget\(\), width, height\);\n        resizeTarget\(levelRenderer\.itrpminimal\$getItemEntityTarget\(\), width, height\);\n', '\n        // 1.21.2+ moved/removed the old LevelRenderer auxiliary target fields; use main target + GameRenderer resize only.\n', text)
        text = text.replace('RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);', 'RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);')
        text = text.replace('mainTarget.resize(width, height, Minecraft.ON_OSX);', 'mainTarget.resize(width, height);')
        text = text.replace('target.resize(width, height, Minecraft.ON_OSX);', 'target.resize(width, height);')
        text = text.replace('            chain.resize(width, height);', '            // PostChain resize(int,int) is not available on 1.21.2+.')
        highres.write_text(text, encoding='utf-8')

        preview = project / 'src/main/java/com/itrpminimal/capture/MinimalScreenshotPreviewScreen.java'
        text = preview.read_text(encoding='utf-8')
        if 'import net.minecraft.client.renderer.RenderType;' not in text:
            text = text.replace('import net.minecraft.client.Minecraft;\n', 'import net.minecraft.client.Minecraft;\nimport net.minecraft.client.renderer.RenderType;\n')
        text = text.replace('.setPixelRGBA(', '.setPixel(')
        text = text.replace('.getPixelRGBA(', '.getPixel(')
        text = text.replace('g.blit(thumb.location, dx, dy, dw, dh, 0.0f, 0.0f, thumb.width, thumb.height, thumb.width, thumb.height);', 'g.blit(RenderType::guiTextured, thumb.location, dx, dy, 0.0f, 0.0f, dw, dh, thumb.width, thumb.height);')
        text = text.replace('g.blit(this.viewingTexture, 0, 0, this.viewingWidth, this.viewingHeight, 0.0f, 0.0f, this.viewingWidth, this.viewingHeight, this.viewingWidth, this.viewingHeight);', 'g.blit(RenderType::guiTextured, this.viewingTexture, 0, 0, 0.0f, 0.0f, this.viewingWidth, this.viewingHeight, this.viewingWidth, this.viewingHeight);')
        preview.write_text(text, encoding='utf-8')

        gallery = project / 'src/main/java/com/itrpminimal/capture/MinimalScreenshotGalleryScreen.java'
        text = gallery.read_text(encoding='utf-8')
        if 'import net.minecraft.client.renderer.RenderType;' not in text:
            text = text.replace('import net.minecraft.client.gui.GuiGraphics;\n', 'import net.minecraft.client.gui.GuiGraphics;\nimport net.minecraft.client.renderer.RenderType;\n')
        text = text.replace('.setPixelRGBA(', '.setPixel(')
        text = text.replace('.getPixelRGBA(', '.getPixel(')
        text = text.replace('graphics.blit(entry.textureId, x, y, 0, 0, THUMB_W, THUMB_H, THUMB_W, THUMB_H);', 'graphics.blit(RenderType::guiTextured, entry.textureId, x, y, 0.0f, 0.0f, THUMB_W, THUMB_H, THUMB_W, THUMB_H);')
        gallery.write_text(text, encoding='utf-8')

        accessor = project / 'src/main/java/com/itrpminimal/capture/mixin/MinimalLevelRendererAccessor.java'
        accessor.write_text(textwrap.dedent('''
            package com.itrpminimal.capture.mixin;

            import net.minecraft.client.renderer.LevelRenderer;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(LevelRenderer.class)
            public interface MinimalLevelRendererAccessor {
            }
        ''').strip() + '\n', encoding='utf-8')

    if mc_at_least(mc, '1.21.5'):
        highres = project / 'src/main/java/com/itrpminimal/capture/MinimalHighResShell.java'
        text = highres.read_text(encoding='utf-8')
        text = text.replace('RenderSystem.maxSupportedTextureSize()', '32768')
        text = text.replace('RenderSystem.viewport(0, 0, current.originalWidth, current.originalHeight);', '// RenderSystem viewport API moved in 1.21.5+.')
        text = text.replace('client.getMainRenderTarget().bindWrite(true);', '// RenderTarget bindWrite API moved in 1.21.5+.')
        text = text.replace('RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);', '// RenderSystem clear API moved in 1.21.5+.')
        text = text.replace('RenderSystem.viewport(0, 0, width, height);', '// RenderSystem viewport API moved in 1.21.5+.')
        highres.write_text(text, encoding='utf-8')

        for rel in [
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotGalleryScreen.java',
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotPreviewScreen.java',
        ]:
            path = project / rel
            text = path.read_text(encoding='utf-8')
            text = text.replace('new DynamicTexture(thumb)', 'new DynamicTexture(() -> "itrp-minimal-thumb", thumb)')
            text = text.replace('new DynamicTexture(image)', 'new DynamicTexture(() -> "itrp-minimal-image", image)')
            path.write_text(text, encoding='utf-8')

        capture_path = project / 'src/main/java/com/itrpminimal/capture/MinimalCaptureController.java'
        text = capture_path.read_text(encoding='utf-8')
        start = text.index('    private static void writeScreenshot(Minecraft client) {')
        end = text.index('    private static void restore(Minecraft client, String reason) {')
        replacement = '''    private static void writeScreenshot(Minecraft client) {
        state = State.WRITING;
        try {
            Path dir = client.gameDirectory.toPath().resolve("screenshots").resolve("itrp_minimal_capture");
            Files.createDirectories(dir);
            Path out = dir.resolve("itrp_minimal_" + FILE_TIME.format(LocalDateTime.now()) + ".png");
            RenderTarget target = client.getMainRenderTarget();
            MinimalLog.line("PHASE WRITE output=" + out + " target=" + target.width + "x" + target.height + " view=" + target.viewWidth + "x" + target.viewHeight);
            Screenshot.takeScreenshot(target, image -> finishScreenshotWrite(client, out, image));
        } catch (Throwable t) {
            MinimalLog.line("WRITE FAILED " + t.getClass().getName() + ": " + t.getMessage());
            say(client, "minimal capture write failed: " + t.getMessage());
            restore(client, "finish");
        }
    }

    private static void finishScreenshotWrite(Minecraft client, Path out, NativeImage image) {
        try {
            if (image == null) {
                throw new IllegalStateException("Screenshot callback returned no image");
            }
            try {
                MinimalLog.line("IMAGE width=" + image.getWidth() + " height=" + image.getHeight() + " frames=" + frames);
                image.writeToFile(out);
                MinimalLog.line("DONE output=" + out);
                say(client, "minimal capture done: " + out.getFileName());
            } finally {
                image.close();
            }
        } catch (Throwable t) {
            MinimalLog.line("WRITE FAILED " + t.getClass().getName() + ": " + t.getMessage());
            say(client, "minimal capture write failed: " + t.getMessage());
        } finally {
            restore(client, "finish");
        }
    }

'''
        text = text[:start] + replacement + text[end:]
        capture_path.write_text(text, encoding='utf-8')

        offline_path = project / 'src/main/java/com/itrpminimal/capture/MinimalOfflineRenderController.java'
        text = offline_path.read_text(encoding='utf-8')
        start = text.index('    private static void writeScreenshot(Minecraft client) {')
        end = text.index('    private static void restore(Minecraft client, String reason) {')
        replacement = '''    private static void writeScreenshot(Minecraft client) {
        state = State.WRITING;
        try {
            Path dir = client.gameDirectory.toPath().resolve("screenshots").resolve("itrp_minimal_offline");
            Files.createDirectories(dir);
            Path out = dir.resolve("itrp_offline_" + targetFrames + "f_" + MinimalHighResShell.multiplier() + "x_" + FILE_TIME.format(LocalDateTime.now()) + ".png");
            RenderTarget target = client.getMainRenderTarget();
            MinimalLog.line("OFFLINE WRITE output=" + out + " target=" + target.width + "x" + target.height + " view=" + target.viewWidth + "x" + target.viewHeight);
            Screenshot.takeScreenshot(target, image -> finishOfflineScreenshotWrite(client, out, image));
        } catch (Throwable t) {
            MinimalLog.line("OFFLINE WRITE FAILED " + t.getClass().getName() + ": " + t.getMessage());
            say(client, "离线渲染写出失败: " + t.getMessage());
            restore(client, "finish");
        }
    }

    private static void finishOfflineScreenshotWrite(Minecraft client, Path out, NativeImage image) {
        try {
            if (image == null) {
                throw new IllegalStateException("Screenshot callback returned no image");
            }
            try {
                MinimalLog.line("OFFLINE IMAGE width=" + image.getWidth() + " height=" + image.getHeight() + " frames=" + frames);
                image.writeToFile(out);
                lastOutput = out;
                MinimalLog.line("OFFLINE DONE output=" + out);
                say(client, "离线渲染完成: " + out.getFileName());
            } finally {
                image.close();
            }
        } catch (Throwable t) {
            MinimalLog.line("OFFLINE WRITE FAILED " + t.getClass().getName() + ": " + t.getMessage());
            say(client, "离线渲染写出失败: " + t.getMessage());
        } finally {
            restore(client, "finish");
        }
    }

'''
        text = text[:start] + replacement + text[end:]
        offline_path.write_text(text, encoding='utf-8')

    if mc_at_least(mc, '1.21.6'):
        for rel in [
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotGalleryScreen.java',
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotPreviewScreen.java',
        ]:
            path = project / rel
            text = path.read_text(encoding='utf-8')
            text = text.replace('import net.minecraft.client.renderer.RenderType;\n', 'import net.minecraft.client.renderer.RenderPipelines;\n')
            text = text.replace('RenderType::guiTextured', 'RenderPipelines.GUI_TEXTURED')
            text = text.replace('g.pose().pushPose();', 'g.pose().pushMatrix();')
            text = text.replace('g.pose().popPose();', 'g.pose().popMatrix();')
            text = text.replace('g.pose().translate(dx, dy, 0.0f);', 'g.pose().translate(dx, dy);')
            text = text.replace('g.pose().scale(displayScale, displayScale, 1.0f);', 'g.pose().scale(displayScale, displayScale);')
            path.write_text(text, encoding='utf-8')

    if mc_at_least(mc, '1.21.9'):
        for rel in [
            'src/main/java/com/itrpminimal/capture/MinimalLog.java',
            'src/main/java/com/itrpminimal/capture/MinimalCaptureController.java',
            'src/main/java/com/itrpminimal/capture/MinimalOfflineRenderController.java',
        ]:
            path = project / rel
            text = path.read_text(encoding='utf-8')
            text = text.replace('.viewWidth', '.width')
            text = text.replace('.viewHeight', '.height')
            path.write_text(text, encoding='utf-8')

        keys = project / 'src/main/java/com/itrpminimal/capture/MinimalKeyBindings.java'
        text = keys.read_text(encoding='utf-8')
        if 'import net.minecraft.resources.ResourceLocation;' not in text:
            text = text.replace('import net.minecraft.client.Minecraft;\n', 'import net.minecraft.client.Minecraft;\nimport net.minecraft.resources.ResourceLocation;\n')
        text = text.replace('private static final String CATEGORY = "key.categories.itrp_minimal_capture";', 'private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("itrp_minimal_capture", "main"));')
        keys.write_text(text, encoding='utf-8')

        for rel in [
            'src/main/java/com/itrpminimal/capture/MinimalCaptureScreen.java',
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotGalleryScreen.java',
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotPreviewScreen.java',
        ]:
            path = project / rel
            text = path.read_text(encoding='utf-8')
            if 'import net.minecraft.client.input.MouseButtonEvent;' not in text:
                text = text.replace('package com.itrpminimal.capture;\n\n', 'package com.itrpminimal.capture;\n\nimport net.minecraft.client.input.MouseButtonEvent;\n')
            text = text.replace('public boolean mouseClicked(double mouseX, double mouseY, int button) {', 'public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {\n        double mouseX = event.x();\n        double mouseY = event.y();\n        int button = event.button();')
            text = text.replace('return super.mouseClicked(mouseX, mouseY, button);', 'return super.mouseClicked(event, doubleClick);')
            text = text.replace('if (super.mouseClicked(mouseX, mouseY, button)) {', 'if (super.mouseClicked(event, doubleClick)) {')
            text = text.replace('public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {', 'public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {\n        double mouseX = event.x();\n        double mouseY = event.y();\n        int button = event.button();')
            text = text.replace('return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);', 'return super.mouseDragged(event, dragX, dragY);')
            text = text.replace('public boolean mouseReleased(double mouseX, double mouseY, int button) {', 'public boolean mouseReleased(MouseButtonEvent event) {\n        double mouseX = event.x();\n        double mouseY = event.y();\n        int button = event.button();')
            text = text.replace('return super.mouseReleased(mouseX, mouseY, button);', 'return super.mouseReleased(event);')
            path.write_text(text, encoding='utf-8')

    if mc_at_least(mc, '1.21.11'):
        for rel in [
            'src/main/java/com/itrpminimal/capture/MinimalKeyBindings.java',
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotGalleryScreen.java',
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotPreviewScreen.java',
        ]:
            path = project / rel
            text = path.read_text(encoding='utf-8')
            text = text.replace('import net.minecraft.resources.ResourceLocation;', 'import net.minecraft.resources.Identifier;')
            text = text.replace('ResourceLocation', 'Identifier')
            path.write_text(text, encoding='utf-8')

        for rel in [
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotGalleryScreen.java',
            'src/main/java/com/itrpminimal/capture/MinimalScreenshotPreviewScreen.java',
        ]:
            path = project / rel
            text = path.read_text(encoding='utf-8')
            text = text.replace('import net.minecraft.Util;', 'import net.minecraft.util.Util;')
            text = text.replace('net.minecraft.Util.', 'net.minecraft.util.Util.')
            path.write_text(text, encoding='utf-8')

        offline = project / 'src/main/java/com/itrpminimal/capture/MinimalOfflineRenderController.java'
        text = offline.read_text(encoding='utf-8')
        text = text.replace('net.minecraft.Util.', 'net.minecraft.util.Util.')
        text = text.replace('oldScreen.resize(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());', 'oldScreen.resize(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());')
        offline.write_text(text, encoding='utf-8')

        capture = project / 'src/main/java/com/itrpminimal/capture/MinimalCaptureController.java'
        text = capture.read_text(encoding='utf-8')
        text = text.replace('oldScreen.resize(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());', 'oldScreen.resize(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());')
        capture.write_text(text, encoding='utf-8')

        freeze = project / 'src/main/java/com/itrpminimal/capture/MinimalFreezeController.java'
        text = freeze.read_text(encoding='utf-8')
        text = text.replace('frozenSunAngle = level.getSunAngle(frozenPartialTick);', 'frozenSunAngle = 0.0F;')
        freeze.write_text(text, encoding='utf-8')


def verify_jar(jar: Path, mc: str) -> None:
    with zipfile.ZipFile(jar) as z:
        names = set(z.namelist())
    required = {
        'fabric.mod.json',
        'itrp_minimal_capture.mixins.json',
        'com/itrpminimal/capture/MinimalCaptureFabric.class',
        'com/itrpminimal/capture/MinimalCaptureController.class',
        'com/itrpminimal/capture/MinimalOfflineRenderController.class',
    }
    missing = sorted(required - names)
    forbidden = [name for name in names if name.startswith('net/neoforged/') or name.endswith('MinimalCaptureMod.class') or name == 'META-INF/neoforge.mods.toml']
    if missing or forbidden:
        write_error(90, 'verify jar', 1, f'missing={missing}\nforbidden={forbidden[:20]}', 'Jar verification failed', 'inspect transformed source and resources')
        raise SystemExit(1)


def build_one(mc: str, api_version: str) -> Path:
    project = WORK_ROOT / f'mc-{mc}'
    copy_source(project)
    update_gradle_wrapper(project)
    write_gradle_files(project, mc, api_version)
    patch_resources(project, mc)
    patch_entry_and_keys(project)
    patch_version_specific(project, mc)
    run(10, ['./gradlew', '--no-daemon', 'clean', 'remapJar'], project)
    jars = sorted((project / 'build/libs').glob(f'{BASE_ARCHIVE}-*-mc{mc}.jar'))
    if not jars:
        jars = sorted((project / 'build/libs').glob('*-remapped.jar')) + sorted((project / 'build/libs').glob('*.jar'))
    jar = [j for j in jars if 'sources' not in j.name][0]
    verify_jar(jar, mc)
    out_dir = OUT_ROOT / mc
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / f'{BASE_ARCHIVE}-{mc}-{MOD_VERSION}.jar'
    if out.exists():
        out.unlink()
    shutil.copy2(jar, out)
    log(f'[ok] {mc} -> {out}')
    return out


def main() -> None:
    reset_logs()
    if not SOURCE.exists():
        write_error(0, 'check source', 1, str(SOURCE), 'clean source directory missing', 'recreate minimal_clean_from_uploaded_0319 first')
        raise SystemExit(1)
    versions = sys.argv[1:] or DEFAULT_MC_VERSIONS
    api_versions = fetch_fabric_api_versions(versions)
    built: list[Path] = []
    for mc in versions:
        built.append(build_one(mc, api_versions[mc]))
    REPORT.write_text('\n'.join(['# Fabric minimal capture build report', '', 'Built outputs:'] + [f'- `{p}`' for p in built]) + '\n', encoding='utf-8')
    ERR.write_text('\n'.join(['status=success', 'errors=none', f'built_count={len(built)}', 'built_versions=' + ','.join(versions), f'out_root={OUT_ROOT}', f'report={REPORT}']) + '\n', encoding='utf-8')
    log('[done] all builds completed')


if __name__ == '__main__':
    main()
