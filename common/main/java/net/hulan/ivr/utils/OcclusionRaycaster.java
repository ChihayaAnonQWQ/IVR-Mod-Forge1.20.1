package net.hulan.ivr.utils;

import mtr.block.BlockPSDAPGBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared conservative world ray walk used by all visibility policies. */
public final class OcclusionRaycaster {
    // A 50-block diagonal sight line can cross far more than 96 voxel boundaries.
    private static final int MAX_STEPS = 256;

    public static boolean isBlocked(net.minecraft.world.level.Level world, Vec3 start, Vec3 end, BlockPos ignoredPos) {
        long started = RenderTimingDebug.begin(RenderTimingDebug.RAY);
        try {
            return trace(world, start, end, ignoredPos);
        } finally {
            RenderTimingDebug.end(RenderTimingDebug.RAY, started);
        }
    }

    private static boolean trace(Level world, Vec3 start, Vec3 end, BlockPos ignoredPos) {
        Vec3 direction = end.subtract(start); // 计算射线方向和长度向量。
        double distance = direction.length(); // 计算射线总长度。
        if (!(distance > 0.001D)) {
            return false; // 起点和终点重合时视为没有遮挡。
        }
        int x = (int) Math.floor(start.x); // 获取射线起点所在方块的 X 坐标。
        int y = (int) Math.floor(start.y); // 获取射线起点所在方块的 Y 坐标。
        int z = (int) Math.floor(start.z); // 获取射线起点所在方块的 Z 坐标。
        int stepX = direction.x >= 0.0D ? 1 : -1; // 设置 X 方向步进。
        int stepY = direction.y >= 0.0D ? 1 : -1; // 设置 Y 方向步进。
        int stepZ = direction.z >= 0.0D ? 1 : -1; // 设置 Z 方向步进。
        double deltaX = direction.x == 0.0D ? Double.MAX_VALUE : Math.abs(1.0D / direction.x); // 计算穿过一个 X 方块的归一化步长。
        double deltaY = direction.y == 0.0D ? Double.MAX_VALUE : Math.abs(1.0D / direction.y); // 计算穿过一个 Y 方块的归一化步长。
        double deltaZ = direction.z == 0.0D ? Double.MAX_VALUE : Math.abs(1.0D / direction.z); // 计算穿过一个 Z 方块的归一化步长。
        double nextX = direction.x == 0.0D ? Double.MAX_VALUE : deltaX * (direction.x >= 0.0D ? Math.floor(start.x) + 1.0D - start.x : start.x - Math.floor(start.x)); // 计算下一次穿过 X 边界的参数。
        double nextY = direction.y == 0.0D ? Double.MAX_VALUE : deltaY * (direction.y >= 0.0D ? Math.floor(start.y) + 1.0D - start.y : start.y - Math.floor(start.y)); // 计算下一次穿过 Y 边界的参数。
        double nextZ = direction.z == 0.0D ? Double.MAX_VALUE : deltaZ * (direction.z >= 0.0D ? Math.floor(start.z) + 1.0D - start.z : start.z - Math.floor(start.z)); // 计算下一次穿过 Z 边界的参数。
        // nextX/Y/Z 是归一化射线参数，射线终点对应 t=1，而不是世界距离值。
        int steps = 0;
        while (Math.min(nextX, Math.min(nextY, nextZ)) < 1.0D) {
            if (++steps > MAX_STEPS) {
                return false;
            }
            if (nextX <= nextY && nextX <= nextZ) {
                x += stepX; // 沿 X 方向进入下一个方块。
                nextX += deltaX; // 推进 X 方向边界参数。
            } else if (nextY <= nextZ) {
                y += stepY; // 沿 Y 方向进入下一个方块。
                nextY += deltaY; // 推进 Y 方向边界参数。
            } else {
                z += stepZ; // 沿 Z 方向进入下一个方块。
                nextZ += deltaZ; // 推进 Z 方向边界参数。
            }
            BlockPos currentPos = new BlockPos(x, y, z); // 构造射线当前经过的方块坐标。
            if (currentPos.equals(ignoredPos)) { // 跳过方块实体自身所在的方块。
                continue; // 避免把目标方块自身误判为遮挡物。
            }
            BlockState state = world.getBlockState(currentPos); // 读取射线经过的方块状态。
            // APG/PSD 面板不遮挡视线，但其后仍可能有实墙。
            if (state.getBlock() instanceof BlockPSDAPGBase) {
                continue;
            }
            // noOcclusion() 只影响相邻方块面合并，并不表示视觉透明；许多装饰墙
            // 正是这种状态。只根据材质不透明性与实际 VoxelShape 射线命中判断。
            if (!state.getMaterial().isSolidBlocking() && state.getLightBlock(world, currentPos) < 15) {
                continue; // 不满足不透明条件时继续检查射线后方。
            }
            VoxelShape shape = state.getShape(world, currentPos, CollisionContext.empty()); // 获取方块的实际碰撞形状。
            if (!shape.isEmpty() && shape.clip(start, end, currentPos) != null) { // 只有射线真正穿过方块形状时才算遮挡。
                return true; // 发现不透明遮挡方块。
            }
        }
        return false; // 射线到达车厢前没有发现遮挡方块。
    }
}

