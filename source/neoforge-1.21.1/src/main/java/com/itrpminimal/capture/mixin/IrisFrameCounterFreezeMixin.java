package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalFreezeController;
import com.itrpminimal.capture.MinimalLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.SystemTimeUniforms$FrameCounter", remap = false)
public abstract class IrisFrameCounterFreezeMixin {
    private static boolean itrpMinimal$logged;

    @Inject(method = "getAsInt()I", at = @At("HEAD"), cancellable = true)
    private void itrpMinimal$freezeFrameCounter(CallbackInfoReturnable<Integer> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            if (!itrpMinimal$logged) {
                itrpMinimal$logged = true;
                MinimalLog.line("IRIS FREEZE applied target=SystemTimeUniforms.FrameCounter " + MinimalFreezeController.summary());
            }
            cir.setReturnValue(1);
        }
    }
}