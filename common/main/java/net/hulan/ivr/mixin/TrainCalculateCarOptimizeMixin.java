package net.hulan.ivr.mixin;

import mtr.data.Train;
import net.hulan.ivr.utils.TrainRenderSkipState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Avoids the per-car geometry and door scans for a fully culled client train. */
@Mixin(Train.class)
public abstract class TrainCalculateCarOptimizeMixin {

    @Inject(method = "calculateCar", at = @At("HEAD"), cancellable = true, remap = false)
    private void ivr$skipDistantCarCalculation(CallbackInfo ci) {
        if (this instanceof TrainRenderSkipState && ((TrainRenderSkipState) this).ivr$skipDistantCars()) {
            ci.cancel();
        }
    }
}
