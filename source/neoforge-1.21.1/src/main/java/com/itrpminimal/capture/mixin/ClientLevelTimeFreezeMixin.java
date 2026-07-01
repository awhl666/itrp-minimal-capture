package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalFreezeController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class ClientLevelTimeFreezeMixin {
    @Inject(method = "getDayTime()J", at = @At("HEAD"), cancellable = true)
    private void itrpMinimal$freezeDayTime(CallbackInfoReturnable<Long> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial() && isClientLevel()) {
            long replacement = MinimalFreezeController.frozenDayTime();
            cir.setReturnValue(replacement);
        }
    }

    @Inject(method = "getGameTime()J", at = @At("HEAD"), cancellable = true)
    private void itrpMinimal$freezeGameTime(CallbackInfoReturnable<Long> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial() && isClientLevel()) {
            long replacement = MinimalFreezeController.frozenGameTime();
            cir.setReturnValue(replacement);
        }
    }

    @Inject(method = "getSunAngle(F)F", at = @At("HEAD"), cancellable = true)
    private void itrpMinimal$freezeSunAngle(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial() && isClientLevel()) {
            cir.setReturnValue(MinimalFreezeController.frozenSunAngle(partialTick));
        }
    }

    /* 1.21.2+ renders sun and moon through SkyRenderer.renderSunMoonAndStars. */

    private boolean isClientLevel() {
        try {
            return Minecraft.getInstance().level == (Object) this;
        } catch (Throwable ignored) {
            return false;
        }
    }
}