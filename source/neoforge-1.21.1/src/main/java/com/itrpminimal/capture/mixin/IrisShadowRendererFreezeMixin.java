package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalFreezeController;
import com.itrpminimal.capture.MinimalLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.shadows.ShadowRenderer", remap = false)
public abstract class IrisShadowRendererFreezeMixin {
    private static boolean itrpMinimal$logged;

    @Inject(method = "getSkyAngle()F", at = @At("HEAD"), cancellable = true)
    private static void itrpMinimal$freezeShadowSkyAngle(CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce();
            cir.setReturnValue(itrpMinimal$skyAngle());
        }
    }

    @Inject(method = "getSunAngle()F", at = @At("HEAD"), cancellable = true)
    private static void itrpMinimal$freezeShadowSunAngle(CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce();
            cir.setReturnValue(itrpMinimal$sunAngle());
        }
    }

    @Inject(method = "getShadowAngle()F", at = @At("HEAD"), cancellable = true)
    private static void itrpMinimal$freezeShadowAngle(CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce();
            float sunAngle = itrpMinimal$sunAngle();
            cir.setReturnValue(sunAngle <= 0.5F ? sunAngle : sunAngle - 0.5F);
        }
    }

    private static float itrpMinimal$skyAngle() {
        return MinimalFreezeController.frozenTimeOfDay(MinimalFreezeController.frozenPartialTick());
    }

    private static float itrpMinimal$sunAngle() {
        float skyAngle = itrpMinimal$skyAngle();
        return skyAngle < 0.75F ? skyAngle + 0.25F : skyAngle - 0.75F;
    }

    private static void itrpMinimal$logOnce() {
        if (!itrpMinimal$logged) {
            itrpMinimal$logged = true;
            MinimalLog.line("IRIS FREEZE applied target=ShadowRenderer " + MinimalFreezeController.summary());
        }
    }
}