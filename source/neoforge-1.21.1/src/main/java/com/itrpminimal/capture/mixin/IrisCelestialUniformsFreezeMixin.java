package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalFreezeController;
import com.itrpminimal.capture.MinimalLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CelestialUniforms", remap = false)
public abstract class IrisCelestialUniformsFreezeMixin {
    private static boolean itrpMinimal$logged;

    @Inject(method = "getSkyAngle()F", at = @At("HEAD"), cancellable = true)
    private static void itrpMinimal$freezeSkyAngle(CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce("CelestialUniforms");
            cir.setReturnValue(itrpMinimal$skyAngle());
        }
    }

    @Inject(method = "getSunAngle()F", at = @At("HEAD"), cancellable = true)
    private static void itrpMinimal$freezeSunAngle(CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce("CelestialUniforms");
            cir.setReturnValue(itrpMinimal$sunAngle());
        }
    }

    private static float itrpMinimal$skyAngle() {
        return MinimalFreezeController.frozenTimeOfDay(MinimalFreezeController.frozenPartialTick());
    }

    private static float itrpMinimal$sunAngle() {
        float skyAngle = itrpMinimal$skyAngle();
        return skyAngle < 0.75F ? skyAngle + 0.25F : skyAngle - 0.75F;
    }

    private static void itrpMinimal$logOnce(String target) {
        if (!itrpMinimal$logged) {
            itrpMinimal$logged = true;
            MinimalLog.line("IRIS FREEZE applied target=" + target + " " + MinimalFreezeController.summary());
        }
    }
}