package net.hulan.ivr.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.hulan.ivr.utils.BlockEntityVisibility;
import net.hulan.ivr.utils.BlockEntityCullDebug;
import net.hulan.ivr.utils.RenderTimingDebug;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 方块实体渲染优化 Mixin。
 * 目的：降低城市密集区所有方块实体的渲染开销。
 * 规则：
 *   - 距离 < BLOCK_ENTITY_RENDER_DISTANCE（50 格）：完全渲染；
 *   - 距离 ≥ 50 格：完全不渲染。
 *   - 仅缓存当前视角下距离内且位于视锥内的方块实体；其余对象不调用具体渲染器。
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderOptimizeMixin {

    @Unique
    private static final double BLOCK_ENTITY_BOUNDING_RADIUS = 8.0D; // 覆盖站牌等可能跨出方块的方块实体模型。

    /**
     * 注入到 Minecraft 通用 BlockEntityRenderDispatcher.render 方法开头。
     * 这里不检查 MTR、IVR 或任何特定接口，因此其他模组的方块实体也会经过同一判定。
     *
     * @param blockEntity       当前要渲染的块实体
     * @param tickDelta         渲染插值（游戏 tick 与渲染帧之间的小数偏移）
     * @param matrices          当前矩阵栈
     * @param multiBufferSource 顶点缓冲源
     * @param ci                回调，可调用 cancel() 取消原方法执行
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void ivr$cullAnyBlockEntity(BlockEntity blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        long started = RenderTimingDebug.begin(RenderTimingDebug.BE);
        BlockEntityCullDebug.Result result;
        try {
            result = BlockEntityVisibility.classify(blockEntity, BLOCK_ENTITY_BOUNDING_RADIUS);
        } finally {
            RenderTimingDebug.end(RenderTimingDebug.BE, started);
        }
        BlockEntityCullDebug.record(blockEntity, result);
        if (result == BlockEntityCullDebug.Result.VIEW_CULLED || result == BlockEntityCullDebug.Result.OCCLUDED) {
            ci.cancel();
        }
    }

    @Redirect(method = "setupAndRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"))
    private static void ivr$timeActualBlockEntityRender(BlockEntityRenderer<BlockEntity> renderer,
            BlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource buffers,
            int light, int overlay) {
        long started = RenderTimingDebug.begin(RenderTimingDebug.BE_RENDER);
        try {
            renderer.render(entity, tickDelta, matrices, buffers, light, overlay);
        } finally {
            RenderTimingDebug.end(RenderTimingDebug.BE_RENDER, started);
        }
    }
}
