package net.hulan.ivr.mixin;

import mtr.data.TrainClient;
import net.hulan.ivr.utils.TrainVisibility;
import net.hulan.ivr.utils.TrainRenderSkipState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 保留 MTR 的整列模拟，只在得到实时车厢位置后跳过远处或完全离开视锥的逐车厢工作。
 * 乘客所在列车始终保留，车厢级剔除继续负责视锥和遮挡判断。
 */
@Mixin(TrainClient.class)
public abstract class TrainSimulateOptimizeMixin implements TrainRenderSkipState {

    @Unique
    private boolean ivr$skipDistantCars;

    @Override
    public boolean ivr$skipDistantCars() {
        return ivr$skipDistantCars;
    }

    @Inject(method = "simulateTrain", at = @At("HEAD"), remap = false)
    private void ivr$resetDistantCarFlag(Level world, float tickDelta,
                                          TrainClient.SpeedCallback speedCallback,
                                          TrainClient.AnnouncementCallback announcementCallback,
                                          TrainClient.AnnouncementCallback lightRailAnnouncementCallback, CallbackInfo ci) {
        ivr$skipDistantCars = false;
    }

    @Inject(method = "handlePositions", at = @At("HEAD"), remap = false)
    private void ivr$cullDistantTrain(Level world, Vec3[] positions, float tickDelta, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        TrainClient train = (TrainClient) (Object) this;
        ivr$skipDistantCars = (minecraft.player == null || !train.isPlayerRiding(minecraft.player))
                && TrainVisibility.isWholeTrainBeyondDistance(world, positions, train.spacing);
    }

}
