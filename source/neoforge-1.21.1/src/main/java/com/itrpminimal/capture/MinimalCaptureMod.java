package com.itrpminimal.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MinimalCaptureMod.MOD_ID, dist = Dist.CLIENT)
public final class MinimalCaptureMod {
    public static final String MOD_ID = "itrp_minimal_capture";

    public MinimalCaptureMod(IEventBus modBus) {
        MinimalLog.line("Screenshot manager loaded");
        modBus.addListener(MinimalCaptureMod::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(MinimalCaptureMod::onClientTick);
        NeoForge.EVENT_BUS.addListener(MinimalCaptureMod::registerCommands);
        Minecraft.getInstance().execute(() -> {
            MinimalCapturePreset.load();
            MinimalShaderManager.cleanupResidualOfflineState(Minecraft.getInstance());
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        MinimalKeyBindings.tick(client);
        MinimalCaptureController.tick(client);
        MinimalOfflineRenderController.tick(client);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        MinimalKeyBindings.register(event);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("itrpminimal")
                .then(Commands.literal("start").executes(ctx -> {
                    MinimalCaptureController.start();
                    return 1;
                }))
                .then(Commands.literal("offline").executes(ctx -> {
                    MinimalOfflineRenderController.start();
                    return 1;
                }))
                .then(Commands.literal("cancel").executes(ctx -> {
                    MinimalCaptureController.cancel("command");
                    MinimalOfflineRenderController.cancel("command");
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    MinimalCaptureController.reportStatus();
                    Minecraft client = Minecraft.getInstance();
                    if (client.gui != null) {
                        client.gui.getChat().addMessage(Component.literal("截图管理器: " + MinimalOfflineRenderController.statusSummary()));
                    }
                    return 1;
                }))
                .then(Commands.literal("gui").executes(ctx -> {
                    Minecraft client = Minecraft.getInstance();
                    client.execute(() -> client.setScreen(new MinimalCaptureScreen(client.screen)));
                    return 1;
                })));
    }
}