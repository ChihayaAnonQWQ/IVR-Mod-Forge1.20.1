package net.hulan.ivr.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Conservative view test for already-generated rail and catenary segments. */
public final class SegmentVisibility {

    private static final Map<SectionKey, Boolean> SECTION_FRUSTUM_CACHE = new java.util.HashMap<>();
    private static Object cacheWorld;
    private static Vec3 cacheCamera;
    private static float cacheYaw;
    private static float cachePitch;
    private static long cacheHits;
    private static long cacheMisses;
    private static long occlusionChecks;
    private static long occlusionCulled;

    public static boolean shouldRenderRailQuad(
            double x1, double z1, double x2, double z2, double x3, double z3, double x4, double z4,
            double y1, double y2) {
        return shouldRenderBounds(
                Math.min(Math.min(x1, x2), Math.min(x3, x4)),
                Math.max(Math.max(x1, x2), Math.max(x3, x4)),
                Math.min(y1, y2), Math.max(y1, y2),
                Math.min(Math.min(z1, z2), Math.min(z3, z4)),
                Math.max(Math.max(z1, z2), Math.max(z3, z4)));
    }

    public static boolean shouldRenderCatenarySegment(
            double x1, double z1, double x2, double z2, double x3, double z3, double x4, double z4,
            double xs1, double zs1, double xs2, double zs2, double xs3, double zs3, double xs4, double zs4,
            double y1, double y2) {
        return shouldRenderBounds(
                Math.min(Math.min(Math.min(x1, x2), Math.min(x3, x4)),
                        Math.min(Math.min(xs1, xs2), Math.min(xs3, xs4))),
                Math.max(Math.max(Math.max(x1, x2), Math.max(x3, x4)),
                        Math.max(Math.max(xs1, xs2), Math.max(xs3, xs4))),
                Math.min(y1, y2) - 0.25D, Math.max(y1, y2) + 0.25D,
                Math.min(Math.min(Math.min(z1, z2), Math.min(z3, z4)),
                        Math.min(Math.min(zs1, zs2), Math.min(zs3, zs4))),
                Math.max(Math.max(Math.max(z1, z2), Math.max(z3, z4)),
                        Math.max(Math.max(zs1, zs2), Math.max(zs3, zs4))));
    }

    private static boolean shouldRenderBounds(double minX, double maxX, double minY, double maxY,
                                              double minZ, double maxZ) {
        if (Minecraft.getInstance().level == null) {
            return true;
        }
        if (!Double.isFinite(minX) || !Double.isFinite(maxX)
                || !Double.isFinite(minZ) || !Double.isFinite(maxZ)
                || !Double.isFinite(minY) || !Double.isFinite(maxY)) {
            return true;
        }
        RenderVisibilityContext.Snapshot view = RenderVisibilityContext.capture(Minecraft.getInstance().level);
        Vec3 camera = view.cameraPosition();
        float yaw = view.yaw();
        float pitch = view.pitch();
        synchronized (SECTION_FRUSTUM_CACHE) {
            if (cacheWorld != Minecraft.getInstance().level || cacheCamera == null
                    || !cacheCamera.equals(camera) || cacheYaw != yaw || cachePitch != pitch) {
                SECTION_FRUSTUM_CACHE.clear();
                cacheWorld = Minecraft.getInstance().level;
                cacheCamera = camera;
                cacheYaw = yaw;
                cachePitch = pitch;
            }
        }
        int sectionX = (int) Math.floor(minX / 16.0D);
        int sectionY = (int) Math.floor(minY / 16.0D);
        int sectionZ = (int) Math.floor(minZ / 16.0D);
        if (sectionX == (int) Math.floor(maxX / 16.0D)
                && sectionY == (int) Math.floor(maxY / 16.0D)
                && sectionZ == (int) Math.floor(maxZ / 16.0D)) {
            SectionKey key = new SectionKey(sectionX, sectionY, sectionZ);
            Boolean outside;
            synchronized (SECTION_FRUSTUM_CACHE) {
                outside = SECTION_FRUSTUM_CACHE.get(key);
                if (outside != null) {
                    cacheHits++;
                } else {
                    cacheMisses++;
                    Vec3 sectionCenter = new Vec3(sectionX * 16.0D + 8.0D,
                            sectionY * 16.0D + 8.0D, sectionZ * 16.0D + 8.0D).subtract(camera);
                    outside = view.outsideFrustum(sectionCenter, 14.8565D);
                    SECTION_FRUSTUM_CACHE.put(key, outside);
                }
            }
            if (outside) {
                return false;
            }
        }
        double dx = Math.max(minX - camera.x, Math.max(0.0D, camera.x - maxX));
        double dy = Math.max(minY - camera.y, Math.max(0.0D, camera.y - maxY));
        double dz = Math.max(minZ - camera.z, Math.max(0.0D, camera.z - maxZ));
        double distance = RailVisibility.RENDER_DISTANCE;
        if (dx * dx + dy * dy + dz * dz > distance * distance) {
            return false;
        }
        Vec3 center = new Vec3((minX + maxX) * 0.5D, (minY + maxY) * 0.5D,
                (minZ + maxZ) * 0.5D).subtract(camera);
        double radius = Math.sqrt((maxX - minX) * (maxX - minX)
                + (maxY - minY) * (maxY - minY)
                + (maxZ - minZ) * (maxZ - minZ)) * 0.5D;
        boolean visible = !view.outsideFrustum(center, radius + 1.0D);
        if (visible) {
            occlusionChecks++;
            visible = RailVisibility.shouldRenderOcclusionBox(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
            if (!visible) {
                occlusionCulled++;
            }
        }
        return visible;
    }

    /** Snapshot counters for comparing chunk/segment culling with occlusion disabled. */
    public static synchronized long[] getStatistics() {
        return new long[]{cacheHits, cacheMisses, occlusionChecks, occlusionCulled};
    }

    private record SectionKey(int x, int y, int z) {}
}
