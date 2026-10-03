package net.hulan.ivr.mixin;

import mtr.data.TrainClient;
import mtr.render.JonModelTrainRenderer;
import net.hulan.ivr.utils.TrainRenderOptimize;
import net.hulan.ivr.utils.TrainVisibility;
import net.hulan.ivr.utils.RenderTimingDebug;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 列车渲染优化 Mixin。
 * 目的：降低列车渲染开销。
 * 规则：
 *   - 距离 ≤ 75 格：完整渲染；75～200 格：按乘车状态渲染模型阶段；
 *   - 距离 > 200 格：完全不渲染。
 *   - 只有整节车厢所有采样点都被实心方块遮挡时才剔除。
 * 所有列车（含玩家乘坐的）都按此距离规则渲染。
 */
@Mixin(value = JonModelTrainRenderer.class)
public abstract class TrainRenderOptimizeMixin {

    /** 当前列车（MTR 客户端数据），用于读取视角偏移。 */
    @Shadow(remap = false)
    @Final
    private TrainClient train;

    /**
     * 注入到 renderCar（每节车厢渲染）开头。
     * 75 格内保持完整渲染；超出距离、视锥或整节车厢完全被遮挡时跳过。
     */
    @Inject(method = "renderCar", at = @At("HEAD"), cancellable = true, remap = false)
    private void ivr$optimizeRenderCar(int carIndex,
                                       double x,
                                       double y,
                                       double z,
                                       float yaw,
                                       float pitch,
                                       boolean doorLeftOpen,
                                       boolean doorRightOpen,
                                       CallbackInfo ci) {
        long started = RenderTimingDebug.begin(RenderTimingDebug.TRAIN_CULL);
        boolean visible;
        try {
            boolean relative = this.train.getViewOffset() != null;
            TrainVisibility.prepareRenderMode(net.minecraft.client.Minecraft.getInstance().level, this.train,
                    new Vec3(x, y, z), relative);
            visible = TrainVisibility.shouldRenderCar(net.minecraft.client.Minecraft.getInstance().level,
                    this.train.id, carIndex, new Vec3(x, y, z), yaw, pitch, relative);
        } finally {
            RenderTimingDebug.end(RenderTimingDebug.TRAIN_CULL, started);
        }
        if (!visible) {
            TrainVisibility.clearRenderMode();
            ci.cancel();
        }
    }

    @Inject(method = "renderCar", at = @At("RETURN"), remap = false)
    private void ivr$finishRenderCar(int carIndex,
                                     double x,
                                     double y,
                                     double z,
                                     float yaw,
                                     float pitch,
                                     boolean doorLeftOpen,
                                     boolean doorRightOpen,
                                     CallbackInfo ci) {
        TrainVisibility.clearRenderMode();
    }

    /**
     * 注入到 renderConnection（车厢连接件渲染）开头。
     * 与车厢使用相同的 200 格距离上限。
     */
    @Inject(method = "renderConnection", at = @At("HEAD"), cancellable = true, remap = false)
    private void ivr$optimizeRenderConnection(Vec3 prevPos1,
                                              Vec3 prevPos2,
                                              Vec3 prevPos3,
                                              Vec3 prevPos4,
                                              Vec3 thisPos1,
                                              Vec3 thisPos2,
                                              Vec3 thisPos3,
                                              Vec3 thisPos4,
                                              double x,
                                              double y,
                                              double z,
                                              float yaw,
                                              float pitch,
                                              CallbackInfo ci) {
        ivr$cullAuxiliary(
                new Vec3[] {
                        prevPos1,
                        prevPos2,
                        prevPos3,
                        prevPos4,
                        thisPos1,
                        thisPos2,
                        thisPos3,
                        thisPos4},
                ci); // 使用连接件的全部角点进行距离判断。
    }

    /**
     * 注入到 renderBarrier（屏蔽门/屏障渲染）开头。
     * 与连接件同样处理。
     */
    @Inject(method = "renderBarrier", at = @At("HEAD"), cancellable = true, remap = false)
    private void ivr$optimizeRenderBarrier(Vec3 prevPos1,
                                           Vec3 prevPos2,
                                           Vec3 prevPos3,
                                           Vec3 prevPos4,
                                           Vec3 thisPos1,
                                           Vec3 thisPos2,
                                           Vec3 thisPos3,
                                           Vec3 thisPos4,
                                           double x,
                                           double y,
                                           double z,
                                           float yaw,
                                           float pitch,
                                           CallbackInfo ci) {
        ivr$cullAuxiliary(
                new Vec3[] {
                        prevPos1,
                        prevPos2,
                        prevPos3,
                        prevPos4,
                        thisPos1,
                        thisPos2,
                        thisPos4},
                ci); // 使用屏障的全部角点进行距离判断。
    }

    /** 辅助部件保持完整显示，直到超出列车渲染距离或当前视锥。 */
    @Unique
    private void ivr$cullAuxiliary(Vec3[] corners, CallbackInfo ci) {
        if (corners == null || corners.length == 0) {
            return;
        }
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        int count = 0;
        for (Vec3 corner : corners) {
            if (corner != null) {
                x += corner.x;
                y += corner.y;
                z += corner.z;
                count++;
            }
        }
        if (count == 0) {
            return;
        }
        boolean relative = train.getViewOffset() != null;
        Vec3 center = new Vec3(x / count, y / count, z / count);
        Vec3 rel = TrainRenderOptimize.toCameraRelative(center, relative);
        double renderReach = TrainRenderOptimize.getTrainRenderDistance() + 12.0D;
        if (rel != null && (rel.lengthSqr() > renderReach * renderReach
                || TrainRenderOptimize.isOutsideFrustum(rel, 12.0D))) {
            ci.cancel();
        }
    }
}
