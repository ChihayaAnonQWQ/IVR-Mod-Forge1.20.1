package net.hulan.ivr.utils;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared per-frame camera data used by block, train, rail and catenary policies. */
public final class RenderVisibilityContext {

    private static final double FALLBACK_FOV_TAN_HALF = Math.tan(Math.toRadians(35.0D));
    private static volatile double cachedFovTanHalf = FALLBACK_FOV_TAN_HALF;
    private static volatile long fovCacheExpiresAt;
    private static Snapshot lastSnapshot;

    private RenderVisibilityContext() {
    }

    public static Snapshot capture(Level world) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 position = camera.getPosition();
        float yaw = camera.getYRot();
        float pitch = camera.getXRot();
        int width = minecraft.getWindow().getWidth();
        int height = Math.max(1, minecraft.getWindow().getHeight());
        double aspect = (double) width / height;
        double tanVertical = fovTanHalf();
        Snapshot previous = lastSnapshot;
        if (previous != null && previous.world() == world && previous.cameraPosition().equals(position)
                && previous.yaw() == yaw && previous.pitch() == pitch && previous.aspect() == aspect
                && previous.tanVertical() == tanVertical) {
            return previous;
        }
        Snapshot snapshot = new Snapshot(world, position, yaw, pitch, aspect, tanVertical,
                frustumPlanes(yaw, pitch, tanVertical, aspect));
        lastSnapshot = snapshot;
        return snapshot;
    }

    public static double fovTanHalf() {
        long now = System.nanoTime();
        if (now < fovCacheExpiresAt) {
            return cachedFovTanHalf;
        }
        try {
            java.lang.reflect.Field field = Minecraft.getInstance().options.getClass().getDeclaredField("fov");
            field.setAccessible(true);
            Object value = field.get(Minecraft.getInstance().options);
            if (!(value instanceof Number)) {
                value = value.getClass().getMethod("get").invoke(value);
            }
            if (value instanceof Number) {
                cachedFovTanHalf = Math.tan(Math.toRadians(((Number) value).doubleValue() * 0.5D));
            }
        } catch (Exception ignored) {
        }
        fovCacheExpiresAt = now + 500_000_000L;
        return cachedFovTanHalf;
    }

    public static boolean outsideFrustum(Vec3 relative,
                                         double radius,
                                         float yaw,
                                         float pitch,
                                         double tanVertical,
                                         double aspect) {
        if (relative == null) {
            return true;
        }
        Vec3[] planes = frustumPlanes(yaw, pitch, tanVertical, aspect);
        return outsideFrustum(relative, radius, planes);
    }

    public static boolean outsideFrustum(Vec3 relative, double radius, Vec3[] planes) {
        if (relative == null) {
            return true;
        }
        return outsideFrustum(relative.x, relative.y, relative.z, radius, planes);
    }

    public static boolean outsideFrustum(double x, double y, double z, double radius, Vec3[] planes) {
        for (int index = 0; index < planes.length; index++) {
            Vec3 plane = planes[index];
            if (plane.x * x + plane.y * y + plane.z * z - (index == 4 ? 0.05D : 0.0D) < -radius) {
                return true;
            }
        }
        return false;
    }

    public static Vec3[] frustumPlanes(double yawDegrees,
                                       double pitchDegrees,
                                       double tanVertical,
                                       double aspect) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        Vec3 forward = new Vec3(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        Vec3 right = new Vec3(-cosYaw, 0.0D, -sinYaw);
        Vec3 up = right.cross(forward);
        double tanHorizontal = tanVertical * aspect;
        return new Vec3[] {
                right.add(forward.scale(tanHorizontal)).normalize(),
                forward.scale(tanHorizontal).subtract(right).normalize(),
                up.add(forward.scale(tanVertical)).normalize(),
                forward.scale(tanVertical).subtract(up).normalize(),
                forward};
    }

    public record Snapshot(
            Level world,
            Vec3 cameraPosition,
            float yaw,
            float pitch,
            double aspect,
            double tanVertical,
            Vec3[] planes) {

        public Vec3 relative(Vec3 worldPosition) {
            return worldPosition.subtract(cameraPosition);
        }

        public boolean outsideFrustum(Vec3 relative, double radius) {
            return RenderVisibilityContext.outsideFrustum(relative, radius, planes);
        }

        public boolean outsideFrustum(double x, double y, double z, double radius) {
            return RenderVisibilityContext.outsideFrustum(x, y, z, radius, planes);
        }

        public double distanceSquared(Vec3 worldPosition) {
            return worldPosition.distanceToSqr(cameraPosition);
        }

        public boolean withinDistance(Vec3 worldPosition, double maxDistance) {
            return distanceSquared(worldPosition) <= maxDistance * maxDistance;
        }
    }
}
