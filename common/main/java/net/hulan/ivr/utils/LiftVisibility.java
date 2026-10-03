package net.hulan.ivr.utils;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Lift visibility policy and compatibility entry point. */
public final class LiftVisibility {//电梯工具类

    private static final double RENDER_DISTANCE = 50.0D;
    private static final double BOUNDING_RADIUS = 8.0D;
    private static final Map<Long, State> STATES = new ConcurrentHashMap<>();
    private static final Map<Long, OcclusionState> OCCLUSION = new ConcurrentHashMap<>();
    private static Level cacheWorld;
    private static long budgetStarted;
    private static int snapshotsInWindow;

    public static boolean shouldRender(Level world, long liftId, Vec3 center, boolean relative) {
        if (world == null || center == null) {
            return true;
        }
        RenderVisibilityContext.Snapshot view = RenderVisibilityContext.capture(world);
        if (cacheWorld != world) {
            STATES.clear();
            OCCLUSION.clear();
            cacheWorld = world;
        }
        State cached = STATES.get(liftId);
        double fov = RenderVisibilityContext.fovTanHalf();
        if (cached != null && cached.view.equals(view) && cached.fov == fov
                && cached.relative == relative && cached.center.distanceToSqr(center) < 0.0001D) {
            return cached.visible;
        }
        Vec3 relativeCenter = relative ? center : view.relative(center);
        boolean visible = relativeCenter.lengthSqr() <= (RENDER_DISTANCE + BOUNDING_RADIUS) * (RENDER_DISTANCE + BOUNDING_RADIUS)
                && !view.outsideFrustum(relativeCenter, BOUNDING_RADIUS);
        STATES.put(liftId, new State(view, fov, center, relative, visible));
        return visible;
    }

    public static boolean shouldCullOccluded(Level world, long liftId, Vec3 center) {
        if (world == null || center == null) {
            return false;
        }
        RenderVisibilityContext.Snapshot view = RenderVisibilityContext.capture(world);
        long now = System.nanoTime();
        OcclusionState state = OCCLUSION.get(liftId);
        boolean matching = state != null && state.world == world
                && state.center.distanceToSqr(center) < 0.25D
                && state.camera.distanceToSqr(view.cameraPosition()) < 0.25D
                && angleDifference(state.yaw, view.yaw()) < 2.0F
                && angleDifference(state.pitch, view.pitch()) < 2.0F;
        if (matching && now - state.completedAt < 150_000_000L) {
            return state.occluded;
        }
        if (now - budgetStarted >= 16_000_000L) {
            budgetStarted = now;
            snapshotsInWindow = 0;
        }
        if (snapshotsInWindow >= 1) {
            return matching && state.occluded && now - state.completedAt < 200_000_000L;
        }
        snapshotsInWindow++;
        Vec3 camera = view.cameraPosition();
        net.minecraft.core.BlockPos ignored = new net.minecraft.core.BlockPos(
                (int) Math.floor(center.x), (int) Math.floor(center.y), (int) Math.floor(center.z));
        boolean allBlocked = true;
        for (int x = -1; x <= 1 && allBlocked; x++) {
            for (int y = -1; y <= 1 && allBlocked; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (!OcclusionRaycaster.isBlocked(world, camera, center.add(x, y, z), ignored)) {
                        allBlocked = false;
                        break;
                    }
                }
            }
        }
        OCCLUSION.put(liftId, new OcclusionState(world, center, camera,
                view.yaw(), view.pitch(), allBlocked, now));
        return allBlocked;
    }

    private static float angleDifference(float a, float b) {
        float difference = Math.abs((a - b) % 360.0F);
        return Math.min(difference, 360.0F - difference);
    }

    private record State(RenderVisibilityContext.Snapshot view, double fov, Vec3 center,
                         boolean relative, boolean visible) {
    }

    private record OcclusionState(Level world, Vec3 center, Vec3 camera,
                                  float yaw, float pitch, boolean occluded, long completedAt) {
    }
}
