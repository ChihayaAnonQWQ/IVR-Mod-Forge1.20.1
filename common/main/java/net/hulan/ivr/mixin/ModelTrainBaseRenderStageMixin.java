package net.hulan.ivr.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.model.ModelTrainBase;
import net.hulan.ivr.utils.TrainVisibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Skips model stages that are not visible at mid distance. */
@Mixin(ModelTrainBase.class)
public abstract class ModelTrainBaseRenderStageMixin {

    @Shadow(remap = false)
    protected abstract void render(PoseStack matrices, VertexConsumer consumer,
                                          ModelTrainBase.RenderStage stage, int light,
                                          float leftDoorX, float rightDoorX, float leftDoorZ, float rightDoorZ,
                                          int carIndex, int trainCars, boolean reversed, boolean isOnRoute);

    @Redirect(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lmtr/data/NameColorDataBase;Lnet/minecraft/resources/ResourceLocation;IFFZIIZZZZZ)V",
            at = @At(value = "INVOKE", target = "Lmtr/model/ModelTrainBase;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lmtr/model/ModelTrainBase$RenderStage;IFFFFIIZZ)V"),
            remap = false)
    private void ivr$renderSelectedStage(ModelTrainBase model, PoseStack matrices, VertexConsumer consumer,
                                         ModelTrainBase.RenderStage stage, int light,
                                         float leftDoorX, float rightDoorX, float leftDoorZ, float rightDoorZ,
                                         int carIndex, int trainCars, boolean reversed, boolean isOnRoute) {
        if (TrainVisibility.shouldRenderStage(stage)) {
            render(matrices, consumer, stage, light, leftDoorX, rightDoorX,
                    leftDoorZ, rightDoorZ, carIndex, trainCars, reversed, isOnRoute);
        }
    }
}
