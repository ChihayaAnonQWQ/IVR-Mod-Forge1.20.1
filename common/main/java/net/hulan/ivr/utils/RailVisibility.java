package net.hulan.ivr.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Visibility policy shared by MTR and ANTE rail bounds and MSD segments. */
public final class RailVisibility {//轨道工具类

    public static final double RENDER_DISTANCE = 300.0D;
    private static final Map<Long, OcclusionState> OCCLUSION_CACHE = new ConcurrentHashMap<>();
    private static long budgetWindowStarted;
    private static int snapshotsInWindow;
    private static Level cacheWorld;
    private static long lastCleanup;
    private static long budgetMisses;
    private static long occlusionHits;
    private static final Object STATE_LOCK = new Object();

    public static boolean shouldRenderBox(AABB box) {
        Level world = Minecraft.getInstance().level;
        if (box == null || world == null) {
            return true;
        }
        RenderVisibilityContext.Snapshot view = RenderVisibilityContext.capture(world);
        Vec3 camera = view.cameraPosition();
        double dx = Math.max(box.minX - camera.x, Math.max(0.0D, camera.x - box.maxX));
        double dy = Math.max(box.minY - camera.y, Math.max(0.0D, camera.y - box.maxY));
        double dz = Math.max(box.minZ - camera.z, Math.max(0.0D, camera.z - box.maxZ));
        if (dx * dx + dy * dy + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) {
            return false;
        }
        double radius = Math.sqrt(box.getXsize() * box.getXsize()
                + box.getYsize() * box.getYsize() + box.getZsize() * box.getZsize()) * 0.5D;
        return !view.outsideFrustum(box.getCenter().subtract(camera), radius)
                && shouldRenderOcclusionBox(box);
    }

    public static boolean shouldRenderOcclusionBox(AABB box) {
        Level world = Minecraft.getInstance().level;
        if (box == null || world == null) {
            return true;
        }
        long now = System.nanoTime();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        synchronized (STATE_LOCK) {
            if (cacheWorld != world) {
                OCCLUSION_CACHE.clear();
                cacheWorld = world;
            }
            if (now - lastCleanup > 5_000_000_000L) {
                OCCLUSION_CACHE.entrySet().removeIf(entry -> now - entry.getValue().completedAt > 1_000_000_000L);
                lastCleanup = now;
            }
        }
        if (box.contains(camera) || box.getXsize() > 3.0D || box.getYsize() > 3.0D || box.getZsize() > 3.0D
                || box.getCenter().distanceToSqr(camera) < 64.0D * 64.0D) {
            return true;
        }
        long key = boxKey(box);
        OcclusionState state = OCCLUSION_CACHE.get(key);
        if (state != null && now - state.completedAt < 250_000_000L
                && state.box.equals(box) && state.camera.equals(camera)) {
            synchronized (STATE_LOCK) {
                occlusionHits++;
            }
            return !state.occluded;
        }
        synchronized (STATE_LOCK) {
            if (now - budgetWindowStarted >= 16_000_000L) {
                budgetWindowStarted = now;
                snapshotsInWindow = 0;
            }
            if (snapshotsInWindow >= 1) {
                budgetMisses++;
                return true;
            }
            snapshotsInWindow++;
        }
        Vec3 center = box.getCenter();
        Vec3[] samples = {
                center,
                new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.maxX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ)
        };
        for (Vec3 sample : samples) {
            if (!OcclusionRaycaster.isBlocked(world, camera, sample, null)) {
                OCCLUSION_CACHE.put(key, new OcclusionState(box, camera, false, now));
                return true;
            }
        }
        OCCLUSION_CACHE.put(key, new OcclusionState(box, camera, true, now));
        return false;
    }

    public static long[] statistics() {
        synchronized (STATE_LOCK) {
            return new long[]{occlusionHits, budgetMisses};
        }
    }

    private static long boxKey(AABB box) {
        long key = 17L;
        key = key * 31L + Math.round(box.minX * 2.0D);
        key = key * 31L + Math.round(box.minY * 2.0D);
        key = key * 31L + Math.round(box.minZ * 2.0D);
        key = key * 31L + Math.round(box.maxX * 2.0D);
        key = key * 31L + Math.round(box.maxY * 2.0D);
        return key * 31L + Math.round(box.maxZ * 2.0D);
    }

    private record OcclusionState(AABB box, Vec3 camera, boolean occluded, long completedAt) {
    }
}
