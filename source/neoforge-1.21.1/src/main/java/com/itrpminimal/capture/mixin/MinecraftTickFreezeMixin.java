package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalFreezeController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BooleanSupplier;

@Mixin(Minecraft.class)
public abstract class MinecraftTickFreezeMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;tick()V"))
    private void itrpMinimal$freezeGameModeTick(MultiPlayerGameMode gameMode) {
        if (!MinimalFreezeController.isFrozen()) {
            gameMode.tick();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickEntities()V"))
    private void itrpMinimal$freezeEntityTick(ClientLevel level) {
        if (!MinimalFreezeController.isFrozen()) {
            level.tickEntities();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void itrpMinimal$freezeLevelTick(ClientLevel level, BooleanSupplier hasTimeLeft) {
        if (!MinimalFreezeController.isFrozen()) {
            level.tick(hasTimeLeft);
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;animateTick(III)V"))
    private void itrpMinimal$freezeAnimateTick(ClientLevel level, int x, int y, int z) {
        if (!MinimalFreezeController.isFrozen()) {
            level.animateTick(x, y, z);
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;tick()V"))
    private void itrpMinimal$freezeParticles(ParticleEngine particleEngine) {
        if (!MinimalFreezeController.isFrozen()) {
            particleEngine.tick();
        }
    }
}