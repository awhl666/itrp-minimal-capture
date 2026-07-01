package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalFreezeController;
import com.itrpminimal.capture.MinimalLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.SystemTimeUniforms$Timer", remap = false)
public abstract class IrisSystemTimeFreezeMixin {
    private static boolean itrpMinimal$logged;

    @Inject(method = "getFrameTimeCounter()F", at = @At("HEAD"), cancellable = true)
    private void itrpMinimal$freezeFrameTimeCounter(CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce();
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getLastFrameTime()F", at = @At("HEAD"), cancellable = true)
    private void itrpMinimal$freezeLastFrameTime(CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            itrpMinimal$logOnce();
            cir.setReturnValue(0.0F);
        }
    }

    private static void itrpMinimal$logOnce() {
        if (!itrpMinimal$logged) {
            itrpMinimal$logged = true;
            MinimalLog.line("IRIS FREEZE applied target=SystemTimeUniforms.Timer " + MinimalFreezeController.summary());
        }
    }
}