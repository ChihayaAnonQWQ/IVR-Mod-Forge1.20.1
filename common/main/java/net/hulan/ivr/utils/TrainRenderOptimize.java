package net.hulan.ivr.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Compatibility facade for existing rendering call sites. */
public final class TrainRenderOptimize {
    public static final double TRAIN_RENDER_DISTANCE = 200.0D;
    public static final double BLOCK_ENTITY_RENDER_DISTANCE = 50.0D;
    public static final double LIFT_RENDER_DISTANCE = 50.0D;
    public static final double RAIL_RENDER_DISTANCE = RailVisibility.RENDER_DISTANCE;
    public static final double TRAIN_FULL_RENDER_DISTANCE = 75.0D;

    private TrainRenderOptimize() {
    }

    public static double getTrainRenderDistance() {
        return TRAIN_RENDER_DISTANCE;
    }

    public static Vec3 toCameraRelative(double x, double y, double z, boolean relative) {
        return toCameraRelative(new Vec3(x, y, z), relative);
    }

    public static Vec3 toCameraRelative(Vec3 position, boolean relative) {
        if (position == null || relative) {
            return position;
        }
        return position.subtract(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }

    public static Vec3 toCameraRelative(BlockPos pos) {
        return pos == null ? null : toCameraRelative(
                new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D), false);
    }

    public static boolean isOutsideFrustum(Vec3 relative, double radius) {
        if (relative == null) {
            return true;
        }
        RenderVisibilityContext.Snapshot view =
                RenderVisibilityContext.capture(Minecraft.getInstance().level);
        return view.outsideFrustum(relative, radius);
    }

    public static boolean isOutsideCurrentFrustum(Vec3 relative, double radius) {
        return Minecraft.getInstance().level != null && isOutsideFrustum(relative, radius);
    }

    public static boolean shouldRenderBlockEntity(BlockEntity entity, double radius) {
        return BlockEntityVisibility.shouldRender(entity, radius);
    }

    public static BlockEntityCullDebug.Result classifyBlockEntityForRender(BlockEntity entity, double radius) {
        return BlockEntityVisibility.classify(entity, radius);
    }

    public static BlockEntityCullDebug.Result classifyBlockEntity(BlockPos pos, Vec3 relative, double radius) {
        Level world = Minecraft.getInstance().level;
        if (world == null || pos == null || relative == null) {
            return BlockEntityCullDebug.Result.ALLOWED;
        }
        BlockEntity entity = world.getBlockEntity(pos);
        return entity == null ? BlockEntityCullDebug.Result.ALLOWED
                : BlockEntityVisibility.classify(entity, radius);
    }

    public static boolean shouldCullBlockEntity(BlockPos pos, Vec3 relative, double radius) {
        BlockEntityCullDebug.Result result = classifyBlockEntity(pos, relative, radius);
        return result == BlockEntityCullDebug.Result.VIEW_CULLED
                || result == BlockEntityCullDebug.Result.OCCLUDED;
    }

    public static boolean shouldRenderCustomRailBox(AABB box) {
        return RailVisibility.shouldRenderBox(box);
    }

    public static boolean shouldRenderSegmentBox(AABB box) {
        return RailVisibility.shouldRenderOcclusionBox(box);
    }

    public static boolean shouldRenderTrainCar(Level world, long trainId, int carIndex, Vec3 center,
                                               float yaw, float pitch, boolean relative) {
        return TrainVisibility.shouldRenderCar(world, trainId, carIndex, center, yaw, pitch, relative);
    }

    public static void prepareTrainRenderMode(Level world, mtr.data.TrainClient train,
                                              Vec3 center, boolean relative) {
        TrainVisibility.prepareRenderMode(world, train, center, relative);
    }

    public static boolean shouldRenderTrainStage(Object stage) {
        return TrainVisibility.shouldRenderStage(stage);
    }

    public static void clearTrainRenderMode() {
        TrainVisibility.clearRenderMode();
    }

    public static boolean isWholeTrainBeyondRenderDistance(Level world, Vec3[] positions, int spacing) {
        return TrainVisibility.isWholeTrainBeyondDistance(world, positions, spacing);
    }

    public static boolean shouldRenderLift(Level world, long liftId, Vec3 center, boolean relative) {
        return LiftVisibility.shouldRender(world, liftId, center, relative);
    }

    public static boolean shouldCullOccludedCar(Level world, String trainId, int carIndex,
                                                Vec3 center, float yaw, float pitch) {
        return TrainVisibility.shouldCullOccludedCar(world, trainId, carIndex, center, yaw, pitch);
    }

    public static boolean shouldCullOccludedLift(Level world, long liftId, Vec3 center) {
        return LiftVisibility.shouldCullOccluded(world, liftId, center);
    }

    public static double[] getLiftPosition(mtr.data.LiftClient lift) {
        try {
            return new double[]{lift.getPositionX(), lift.getPositionY(), lift.getPositionZ()};
        } catch (Exception exception) {
            return null;
        }
    }
}
