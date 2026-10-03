package net.hulan.ivr.mixin;

import mtr.data.TrainClient;
import mtr.render.RenderTrains;
import net.hulan.ivr.utils.TrainRenderSkipState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import java.util.function.Consumer;

@Mixin(RenderTrains.class)
public abstract class RenderTrainsLoopOptimizeForgeMixin {

    @Redirect(method = "render(Lmtr/entity/EntitySeat;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;forEach(Ljava/util/function/Consumer;)V", ordinal = 1), remap = false)
    private static void ivr$renderVisibleTranslucentTrainPass(Set<TrainClient> trains, Consumer<TrainClient> action) {
        for (TrainClient train : trains) {
            if (!(train instanceof TrainRenderSkipState) || !((TrainRenderSkipState) train).ivr$skipDistantCars()) {
                action.accept(train);
            }
        }
    }
}
