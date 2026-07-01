package com.itrpminimal.capture.mixin;

import com.itrpminimal.capture.MinimalFreezeController;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelTimeOfDayFreezeMixin {
    @Inject(method = "method_30274(F)F", at = @At("HEAD"), cancellable = true, remap = false)
    private void itrpMinimal$freezeClientTimeOfDay(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            cir.setReturnValue(MinimalFreezeController.frozenTimeOfDay(partialTick));
        }
    }

    @Inject(method = "method_30273()I", at = @At("HEAD"), cancellable = true, remap = false)
    private void itrpMinimal$freezeMoonPhase(CallbackInfoReturnable<Integer> cir) {
        if (MinimalFreezeController.shouldFreezeCelestial()) {
            cir.setReturnValue((int)Math.floorMod(MinimalFreezeController.frozenDayTime() / 24000L, 8L));
        }
    }
}