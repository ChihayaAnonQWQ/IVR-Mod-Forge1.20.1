package net.hulan.ivr.utils;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Block entity visibility policy. Keeps all BE selection at the dispatcher boundary. */
public final class BlockEntityVisibility {//BE工具类

    private static final Map<BlockEntity, State> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Class<? extends BlockEntity>, BoundsProvider> BOUNDS_PROVIDERS = new ConcurrentHashMap<>();
    private static final double RENDER_DISTANCE = 50.0D;
    private static Level cacheWorld;
    private static final Map<Long, OcclusionState> OCCLUSION = new ConcurrentHashMap<>();
    private static final Map<Long, Probe> QUEUE = new LinkedHashMap<>();
    private static long budgetStarted;
    private static int raysInWindow;
    private static long pauseUntil;
    private static float lastYaw;
    private static float lastPitch;
    private static Vec3 lastCamera;
    private static boolean cameraInitialized;

    /** Providers return a current world-space box covering the complete rendered model. */
    public static <T extends BlockEntity> void registerBoundsProvider(Class<T> type, Function<T, AABB> provider) {
        registerBoundsProvider(type, provider, false);
    }

    public static <T extends BlockEntity> void registerStaticBoundsProvider(Class<T> type, Function<T, AABB> provider) {
        registerBoundsProvider(type, provider, true);
    }

    private static <T extends BlockEntity> void registerBoundsProvider(Class<T> type,
                                                                       Function<T, AABB> provider,
                                                                       boolean staticBounds) {
        BOUNDS_PROVIDERS.put(type, new BoundsProvider(entity -> provider.apply(type.cast(entity)), staticBounds));
        CACHE.clear();
        OCCLUSION.clear();
        QUEUE.clear();
    }

    public static boolean shouldRender(BlockEntity entity, double radius) {
        if (entity == null || entity.getLevel() == null) {
            return true;
        }
        return viewState(entity, radius).visible;
    }

    private static State viewState(BlockEntity entity, double radius) {
        Level world = entity.getLevel();
        if (cacheWorld != world) {
            CACHE.clear();
            OCCLUSION.clear();
            QUEUE.clear();
            cameraInitialized = false;
            lastCamera = null;
            cacheWorld = world;
        }
        RenderVisibilityContext.Snapshot view = RenderVisibilityContext.capture(world);
        State cached = CACHE.get(entity);
        BoundsProvider provider = BOUNDS_PROVIDERS.get(entity.getClass());
        BlockPos pos = entity.getBlockPos();
        BlockState blockState = provider != null && provider.staticBounds ? entity.getBlockState() : null;
        boolean reusableBounds = provider == null || (provider.staticBounds && cached != null
                && cached.pos == pos.asLong() && cached.blockState == blockState);
        if (cached != null && cached.view == view && cached.radius == radius && reusableBounds) {
            return cached;
        }
        AABB bounds = reusableBounds && cached != null ? cached.bounds : boundsFor(entity, provider);
        if (cached != null && cached.view == view && cached.radius == radius
                && (provider == null || !provider.staticBounds || reusableBounds)
                && java.util.Objects.equals(cached.bounds, bounds)) {
            return cached;
        }
        if (cached == null) {
            cached = new State();
            CACHE.put(entity, cached);
        }
        Vec3 camera = view.cameraPosition();
        double x = (bounds == null ? pos.getX() + 0.5D : (bounds.minX + bounds.maxX) * 0.5D) - camera.x;
        double y = (bounds == null ? pos.getY() + 0.5D : (bounds.minY + bounds.maxY) * 0.5D) - camera.y;
        double z = (bounds == null ? pos.getZ() + 0.5D : (bounds.minZ + bounds.maxZ) * 0.5D) - camera.z;
        double effectiveRadius = bounds == null ? radius : Math.sqrt(
                bounds.getXsize() * bounds.getXsize() + bounds.getYsize() * bounds.getYsize()
                        + bounds.getZsize() * bounds.getZsize()) * 0.5D;
        double dx = bounds == null ? 0.0D : Math.max(bounds.minX - camera.x,
                Math.max(0.0D, camera.x - bounds.maxX));
        double dy = bounds == null ? 0.0D : Math.max(bounds.minY - camera.y,
                Math.max(0.0D, camera.y - bounds.maxY));
        double dz = bounds == null ? 0.0D : Math.max(bounds.minZ - camera.z,
                Math.max(0.0D, camera.z - bounds.maxZ));
        double reach = RENDER_DISTANCE + radius;
        cached.visible = (bounds == null ? x * x + y * y + z * z < reach * reach
                : dx * dx + dy * dy + dz * dz < RENDER_DISTANCE * RENDER_DISTANCE)
                && !view.outsideFrustum(x, y, z, effectiveRadius);
        cached.view = view;
        cached.radius = radius;
        cached.bounds = bounds;
        cached.pos = pos.asLong();
        cached.blockState = blockState;
        return cached;
    }

    public static BlockEntityCullDebug.Result classify(BlockEntity entity, double radius) {
        if (entity == null || entity.getLevel() == null) {
            return BlockEntityCullDebug.Result.ALLOWED;
        }
        State state = viewState(entity, radius);
        if (!state.visible) {
            return BlockEntityCullDebug.Result.VIEW_CULLED;
        }
        Level world = entity.getLevel();
        RenderVisibilityContext.Snapshot view = state.view;
        BlockPos pos = entity.getBlockPos();
        AABB bounds = state.bounds;
        if ((bounds != null && bounds.contains(view.cameraPosition()))
                || (bounds == null && view.cameraPosition().x >= pos.getX() && view.cameraPosition().x < pos.getX() + 1
                && view.cameraPosition().y >= pos.getY() && view.cameraPosition().y < pos.getY() + 1
                && view.cameraPosition().z >= pos.getZ() && view.cameraPosition().z < pos.getZ() + 1)) {
            return BlockEntityCullDebug.Result.NEAR;
        }
        long now = System.nanoTime();
        if (cameraInitialized && (!view.cameraPosition().equals(lastCamera)
                || view.yaw() != lastYaw || view.pitch() != lastPitch)) {
            pauseUntil = now + 180_000_000L;
        }
        lastYaw = view.yaw();
        lastPitch = view.pitch();
        lastCamera = view.cameraPosition();
        cameraInitialized = true;
        if (now < pauseUntil) {
            return BlockEntityCullDebug.Result.PENDING;
        }
        drain(world, view, now);
        OcclusionState cached = OCCLUSION.get(pos.asLong());
        if (cached != null && cached.camera.equals(view.cameraPosition())
                && java.util.Objects.equals(cached.bounds, bounds)
                && now - cached.completedAt < (cached.occluded ? 500_000_000L : 250_000_000L)) {
            return cached.occluded ? BlockEntityCullDebug.Result.OCCLUDED : BlockEntityCullDebug.Result.RAY_CLEAR;
        }
        if (QUEUE.size() < 2048) {
            Probe existing = QUEUE.get(pos.asLong());
            if (existing == null || !java.util.Objects.equals(existing.bounds, bounds)) {
                QUEUE.put(pos.asLong(), new Probe(radius, view.cameraPosition(), bounds));
            }
        }
        return BlockEntityCullDebug.Result.PENDING;
    }

    private static void drain(Level world, RenderVisibilityContext.Snapshot view, long now) {
        if (now - budgetStarted >= 16_000_000L) {
            budgetStarted = now;
            raysInWindow = 0;
        }
        if (raysInWindow >= 8 || QUEUE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Long, Probe>> iterator = QUEUE.entrySet().iterator();
        while (iterator.hasNext() && raysInWindow < 8) {
            Map.Entry<Long, Probe> request = iterator.next();
            BlockPos pos = BlockPos.of(request.getKey());
            Probe probe = request.getValue();
            if (!probe.camera.equals(view.cameraPosition())) {
                probe = new Probe(probe.radius, view.cameraPosition(), probe.bounds);
                request.setValue(probe);
            }
            AABB bounds = probe.bounds;
            Vec3 center = bounds == null ?
                    new Vec3(
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D)
                    : bounds.getCenter();
            Vec3 relative = view.relative(center);
            double effectiveRadius = bounds == null ? probe.radius : Math.sqrt(
                    bounds.getXsize() * bounds.getXsize() + bounds.getYsize() * bounds.getYsize()
                            + bounds.getZsize() * bounds.getZsize()) * 0.5D;
            if ((bounds == null && relative.lengthSqr() > Math.pow(RENDER_DISTANCE + probe.radius, 2.0D))
                    || view.outsideFrustum(relative, effectiveRadius)) {
                iterator.remove();
                continue;
            }
            boolean visible = false;
            double sampleRadius = Math.min(probe.radius, 8.0D);
            while (probe.nextSample < (bounds == null ? 27 : 9) && raysInWindow < 8) {
                int index = probe.nextSample++;
                raysInWindow++;
                Vec3 sample = bounds == null ? center.add(((double) index / 9 - 1) * sampleRadius,
                        (((double) index / 3) % 3 - 1) * sampleRadius,
                        (index % 3 - 1) * sampleRadius) : boxSample(bounds, index);
                if (!OcclusionRaycaster.isBlocked(world, view.cameraPosition(), sample, pos)) {
                    visible = true;
                    break;
                }
            }
            if (visible || probe.nextSample == (bounds == null ? 27 : 9)) {
                iterator.remove();
                OCCLUSION.put(
                        request.getKey(),
                        new OcclusionState(
                                view.cameraPosition(),
                                bounds,
                                !visible,
                                System.nanoTime()));
            }
        }
    }

    private static Vec3 boxSample(AABB bounds, int index) {
        if (index == 0) {
            return bounds.getCenter();
        }
        int corner = index - 1;
        return new Vec3(
                (corner & 1) == 0 ? bounds.minX : bounds.maxX,
                (corner & 2) == 0 ? bounds.minY : bounds.maxY,
                (corner & 4) == 0 ? bounds.minZ : bounds.maxZ);
    }

    private static AABB boundsFor(BlockEntity entity, BoundsProvider provider) {
        if (provider == null) return null;
        try {
            AABB bounds = provider.function.apply(entity);
            if (bounds == null || !Double.isFinite(bounds.minX) || !Double.isFinite(bounds.minY)
                    || !Double.isFinite(bounds.minZ) || !Double.isFinite(bounds.maxX)
                    || !Double.isFinite(bounds.maxY) || !Double.isFinite(bounds.maxZ)) {
                return null;
            }
            return bounds;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static final class Probe {

        private final double radius;
        private final Vec3 camera;
        private final AABB bounds;
        private int nextSample;

        private Probe(double radius, Vec3 camera, AABB bounds) {
            this.radius = radius;
            this.camera = camera;
            this.bounds = bounds;
        }
    }

    private record OcclusionState(Vec3 camera, AABB bounds, boolean occluded, long completedAt) {
    }

    private static final class State {

        private RenderVisibilityContext.Snapshot view;
        private double radius;
        private AABB bounds;
        private long pos;
        private BlockState blockState;
        private boolean visible;
    }

    private record BoundsProvider(Function<BlockEntity, AABB> function, boolean staticBounds) {
    }
}
