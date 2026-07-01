package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalFreezeController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.client.DeltaTracker$DefaultValue", "net.minecraft.client.DeltaTracker$Timer"})
public abstract class DeltaTrackerPartialTickFreezeMixin {
    @Inject(method = "getGameTimeDeltaPartialTick(Z)F", at = @At("RETURN"), cancellable = true)
    private void itrpMinimal$freezePartialTick(boolean runsNormally, CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.isFrozen()) {
            float replacement = MinimalFreezeController.frozenPartialTick();
            cir.setReturnValue(replacement);
        } else {
            MinimalFreezeController.rememberPartialTick(cir.getReturnValueF());
        }
    }
}