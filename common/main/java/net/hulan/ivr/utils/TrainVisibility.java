package net.hulan.ivr.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** MTR train visibility policy and compatibility entry point. */
public final class TrainVisibility {
    private static final double RENDER_DISTANCE = 200.0D;
    private static final double BOUNDING_RADIUS = 32.0D;
    private static final double OCCLUSION_MIN_DISTANCE = 80.0D;
    private static final Map<String, OcclusionState> OCCLUSION = new ConcurrentHashMap<>();
    private static final Map<String, Probe> PROBES = new ConcurrentHashMap<>();
    private static long budgetStarted;
    private static int raysInWindow;
    private static Level cacheWorld;
    private static long lastCleanup;
    private static final ThreadLocal<RenderMode> RENDER_MODE = ThreadLocal.withInitial(() -> RenderMode.FULL);
    private TrainVisibility() {
    }

    public static void prepareRenderMode(Level world, mtr.data.TrainClient train, Vec3 center, boolean relative) {
        if (world == null || train == null || center == null) {
            RENDER_MODE.set(RenderMode.FULL);
            return;
        }
        Vec3 rel = relative ? center : RenderVisibilityContext.capture(world).relative(center);
        double distance = rel.lengthSqr();
        if (distance <= 75.0D * 75.0D || distance > RENDER_DISTANCE * RENDER_DISTANCE) {
            RENDER_MODE.set(RenderMode.FULL);
        } else {
            RENDER_MODE.set(Minecraft.getInstance().player != null
                    && train.isPlayerRiding(Minecraft.getInstance().player)
                    ? RenderMode.INTERIOR_ONLY : RenderMode.EXTERIOR_ONLY);
        }
    }

    public static boolean shouldRenderStage(Object stage) {
        if (!(stage instanceof mtr.model.ModelTrainBase.RenderStage)) {
            return true;
        }
        return switch (RENDER_MODE.get()) {
            case FULL -> true;
            case INTERIOR_ONLY -> stage == mtr.model.ModelTrainBase.RenderStage.INTERIOR
                    || stage == mtr.model.ModelTrainBase.RenderStage.INTERIOR_TRANSLUCENT
                    || stage == mtr.model.ModelTrainBase.RenderStage.LIGHTS
                    || stage == mtr.model.ModelTrainBase.RenderStage.ALWAYS_ON_LIGHTS;
            case EXTERIOR_ONLY -> stage != mtr.model.ModelTrainBase.RenderStage.INTERIOR
                    && stage != mtr.model.ModelTrainBase.RenderStage.INTERIOR_TRANSLUCENT;
        };
    }

    public static void clearRenderMode() {
        RENDER_MODE.set(RenderMode.FULL);
    }

    private enum RenderMode { FULL, INTERIOR_ONLY, EXTERIOR_ONLY }

    public static boolean shouldRenderCar(Level world, long trainId, int carIndex, Vec3 center,
                                          float yaw, float pitch, boolean relative) {
        if (world == null || center == null) {
            return true;
        }
        RenderVisibilityContext.Snapshot view = RenderVisibilityContext.capture(world);
        Vec3 rel = relative ? center : view.relative(center);
        double distanceSqr = rel.lengthSqr();
        boolean nearRidingCar = relative && distanceSqr <= OCCLUSION_MIN_DISTANCE * OCCLUSION_MIN_DISTANCE;
        double reach = RENDER_DISTANCE + BOUNDING_RADIUS;
        if (distanceSqr > reach * reach || (!nearRidingCar && view.outsideFrustum(rel, BOUNDING_RADIUS))) {
            return false;
        }
        return relative || distanceSqr <= OCCLUSION_MIN_DISTANCE * OCCLUSION_MIN_DISTANCE
                || !shouldCullOccludedCar(world, Long.toString(trainId), carIndex, center, yaw, pitch);
    }

    public static boolean shouldCullOccludedCar(Level world, String trainId, int carIndex, Vec3 center,
                                                float yaw, float pitch) {
        if (world == null || trainId == null || center == null) {
            return false;
        }
        if (cacheWorld != world) {
            OCCLUSION.clear();
            PROBES.clear();
            cacheWorld = world;
        }
        long now = System.nanoTime();
        if (now - lastCleanup > 5_000_000_000L) {
            OCCLUSION.entrySet().removeIf(entry -> now - entry.getValue().completedAt > 1_000_000_000L);
            PROBES.entrySet().removeIf(entry -> now - entry.getValue().startedAt > 1_000_000_000L);
            lastCleanup = now;
        }
        String key = trainId + ":" + carIndex;
        RenderVisibilityContext.Snapshot view = RenderVisibilityContext.capture(world);
        OcclusionState state = OCCLUSION.get(key);
        boolean matching = state != null && state.center.equals(center)
                && state.camera.equals(view.cameraPosition())
                && angleDifference(state.yaw, yaw) < 2.0F
                && angleDifference(state.pitch, pitch) < 2.0F
                && angleDifferenceDegrees(state.cameraYaw, view.yaw()) < 2.0F
                && angleDifferenceDegrees(state.cameraPitch, view.pitch()) < 2.0F;
        if (now - budgetStarted >= 16_000_000L) {
            budgetStarted = now;
            raysInWindow = 0;
        }
        if (state == null || now - state.completedAt >= 150_000_000L) {
            Probe probe = PROBES.get(key);
            if (probe == null || !probe.matches(center, view.cameraPosition(), yaw, pitch,
                    view.yaw(), view.pitch())) {
                probe = new Probe(center, view.cameraPosition(), yaw, pitch,
                        view.yaw(), view.pitch(), now);
                PROBES.put(key, probe);
            }
            Boolean blocked = advance(world, probe);
            if (blocked != null) {
                PROBES.remove(key);
                int streak = blocked && matching && state.rawOccluded ? state.streak + 1 : blocked ? 1 : 0;
                OCCLUSION.put(key, new OcclusionState(center, view.cameraPosition(), yaw, pitch,
                        view.yaw(), view.pitch(), blocked, streak >= 3, streak, now));
            }
        }
        return matching && state.occluded && now - state.completedAt < 200_000_000L;
    }

    private static Boolean advance(Level world, Probe probe) {
        while (probe.nextSample < 27 && raysInWindow < 8) {
            int index = probe.nextSample++;
            raysInWindow++;
            double localX = index / 9 == 0 ? 0.0D : index / 9 == 1 ? -11.0D : 11.0D;
            double localY = (index / 3) % 3 == 0 ? 1.5D : (index / 3) % 3 == 1 ? 0.0D : 3.0D;
            double localZ = index % 3 == 0 ? 0.0D : index % 3 == 1 ? -2.0D : 2.0D;
            double cos = Math.cos(probe.yaw), sin = Math.sin(probe.yaw);
            Vec3 sample = probe.center.add(localX * cos - localZ * sin, localY,
                    localX * sin + localZ * cos);
            if (!OcclusionRaycaster.isBlocked(world, probe.camera, sample, null)) {
                return false;
            }
        }
        return probe.nextSample == 27 ? true : null;
    }

    private static float angleDifference(float a, float b) {
        float difference = Math.abs((a - b) % (float) (Math.PI * 2.0D));
        return Math.min(difference, (float) (Math.PI * 2.0D) - difference);
    }

    private static float angleDifferenceDegrees(float a, float b) {
        float difference = Math.abs((a - b) % 360.0F);
        return Math.min(difference, 360.0F - difference);
    }

    private static final class Probe {
        private final Vec3 center, camera;
        private final float yaw, pitch, cameraYaw, cameraPitch;
        private final long startedAt;
        private int nextSample;

        private Probe(Vec3 center, Vec3 camera, float yaw, float pitch,
                      float cameraYaw, float cameraPitch, long startedAt) {
            this.center = center;
            this.camera = camera;
            this.yaw = yaw;
            this.pitch = pitch;
            this.cameraYaw = cameraYaw;
            this.cameraPitch = cameraPitch;
            this.startedAt = startedAt;
        }

        private boolean matches(Vec3 center, Vec3 camera, float yaw, float pitch,
                                float cameraYaw, float cameraPitch) {
            return this.center.equals(center) && this.camera.equals(camera)
                    && this.yaw == yaw && this.pitch == pitch
                    && angleDifferenceDegrees(this.cameraYaw, cameraYaw) < 2.0F
                    && angleDifferenceDegrees(this.cameraPitch, cameraPitch) < 2.0F;
        }
    }

    private record OcclusionState(Vec3 center, Vec3 camera, float yaw, float pitch,
                                  float cameraYaw, float cameraPitch, boolean rawOccluded,
                                  boolean occluded, int streak, long completedAt) {
    }

    public static boolean isWholeTrainBeyondDistance(Level world, Vec3[] positions, int spacing) {
        if (world == null || positions == null || positions.length < 2 || Minecraft.getInstance().level != world) {
            return false;
        }
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 position : positions) {
            if (position == null || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
                return false;
            }
            minX = Math.min(minX, position.x);
            minY = Math.min(minY, position.y);
            minZ = Math.min(minZ, position.z);
            maxX = Math.max(maxX, position.x);
            maxY = Math.max(maxY, position.y);
            maxZ = Math.max(maxZ, position.z);
        }
        double margin = Math.max(BOUNDING_RADIUS, spacing);
        Vec3 camera = RenderVisibilityContext.capture(world).cameraPosition();
        double dx = Math.max(0.0D, Math.max(minX - margin - camera.x, camera.x - maxX - margin));
        double dy = Math.max(0.0D, Math.max(minY - margin - camera.y, camera.y - maxY - margin));
        double dz = Math.max(0.0D, Math.max(minZ - margin - camera.z, camera.z - maxZ - margin));
        double distanceSqr = dx * dx + dy * dy + dz * dz;
        if (distanceSqr > RENDER_DISTANCE * RENDER_DISTANCE) {
            return true;
        }
        if (distanceSqr <= OCCLUSION_MIN_DISTANCE * OCCLUSION_MIN_DISTANCE) {
            return false;
        }
        RenderVisibilityContext.Snapshot view = RenderVisibilityContext.capture(world);
        Vec3 boundsCenter = new Vec3((minX + maxX) * 0.5D, (minY + maxY) * 0.5D, (minZ + maxZ) * 0.5D);
        double radius = Math.sqrt(Math.pow(maxX - minX, 2.0D)
                + Math.pow(maxY - minY, 2.0D) + Math.pow(maxZ - minZ, 2.0D)) * 0.5D + margin;
        return view.outsideFrustum(view.relative(boundsCenter), radius);
    }

}
