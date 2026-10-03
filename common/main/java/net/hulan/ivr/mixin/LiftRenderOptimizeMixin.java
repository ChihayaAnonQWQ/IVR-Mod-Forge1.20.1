package net.hulan.ivr.mixin;

import mtr.data.LiftClient;
import net.hulan.ivr.utils.LiftVisibility;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 电梯渲染优化 Mixin。
 * 目的：缓存电梯可见性，并在渲染距离外跳过电梯顶点生成。
 * 原版 MTR 的电梯（LiftClient.tickClient）每帧都会绘制所有电梯（电梯箱体 + 门 + 面板），
 * 远处/屏幕外的电梯同样全量提交，增加渲染线程顶点生成负担。
 * 做法：重定向 LiftClient.tickClient 中的 renderLift（电梯绘制回调）调用，
 * 读取电梯当前位置并判断到相机距离：
 *   - 距离 ≤ LIFT_RENDER_DISTANCE（50 格）且在视锥内：渲染；
 *   - 其他情况：跳过电梯绘制，但保留 tickClient 后续逻辑。
 * 可见性结果按视角和电梯位置缓存。
 * 注：LiftClient.tickClient 的 tick()（电梯移动/开门逻辑）在渲染之前执行，
 * 因此只跳过绘制，不影响电梯行为。
 * 位置通过 Lift 的公开 getter 读取。
 */
@Mixin(LiftClient.class)
public abstract class LiftRenderOptimizeMixin {

    /** 只拦截绘制回调，保留 tickClient 后面的乘客和菜单逻辑。 */
    @Redirect(method = "tickClient",
            at = @At(value = "INVOKE",
                    target = "Lmtr/data/LiftClient$RenderLift;renderLift(DDDFF)V"),
            remap = false)
    private void ivr$renderLiftIfVisible(LiftClient.RenderLift renderLift,
                                         double x,
                                         double y,
                                         double z,
                                         float frontOpen,
                                         float backOpen) {
        LiftClient lift = (LiftClient) (Object) this;
        boolean relative = lift.getViewOffset() != null;
        if (LiftVisibility.shouldRender(net.minecraft.client.Minecraft.getInstance().level, lift.id, new Vec3(x, y, z), relative)
                && (relative || !LiftVisibility.shouldCullOccluded(net.minecraft.client.Minecraft.getInstance().level, lift.id, new Vec3(x, y, z)))) {
            renderLift.renderLift(x, y, z, frontOpen, backOpen);
        }
    }
}
