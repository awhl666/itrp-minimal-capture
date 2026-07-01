package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalFreezeController;
import com.itrpminimal.capture.MinimalLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.WorldTimeUniforms", remap = false)
public abstract class IrisWorldTimeUniformsFreezeMixin {
    private static boolean itrpMinimal$logged;

    @Inject(method = "getWorldDayTime()I", at = @At("HEAD"), cancellable = true)
    private static void itrpMinimal$freezeWorldDayTime(CallbackInfoReturnable<Integer> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce();
            cir.setReturnValue((int)Math.floorMod(MinimalFreezeController.frozenDayTime(), 24000L));
        }
    }

    @Inject(method = "getWorldDay()I", at = @At("HEAD"), cancellable = true)
    private static void itrpMinimal$freezeWorldDay(CallbackInfoReturnable<Integer> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce();
            cir.setReturnValue((int)(MinimalFreezeController.frozenDayTime() / 24000L));
        }
    }

    @Inject(method = "lambda$addWorldTimeUniforms$0()I", at = @At("HEAD"), cancellable = true)
    private static void itrpMinimal$freezeMoonPhase(CallbackInfoReturnable<Integer> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce();
            cir.setReturnValue((int)Math.floorMod(MinimalFreezeController.frozenDayTime() / 24000L, 8L));
        }
    }

    private static void itrpMinimal$logOnce() {
        if (!itrpMinimal$logged) {
            itrpMinimal$logged = true;
            MinimalLog.line("IRIS FREEZE applied target=WorldTimeUniforms " + MinimalFreezeController.summary());
        }
    }
}