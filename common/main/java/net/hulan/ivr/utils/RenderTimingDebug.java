package net.hulan.ivr.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** Sampled render-thread CPU timings shown only in the F3 overlay. */
public final class RenderTimingDebug {

    public static final int BE = 0;
    public static final int RAY = 1;
    public static final int TRAIN_CULL = 2;
    public static final int RAIL = 3;
    public static final int MSD = 4;
    public static final int BE_RENDER = 5;
    public static final int MSD_GEOMETRY = 6;
    private static final int COUNT = 7;
    private static final int[] SAMPLE_INTERVAL = {64, 8, 8, 64, 64, 64, 64};
    private static final long[] calls = new long[COUNT];
    private static final long[] samples = new long[COUNT];
    private static final long[] nanos = new long[COUNT];
    private static final long[] previousCalls = new long[COUNT];
    private static final long[] previousSamples = new long[COUNT];
    private static final long[] previousNanos = new long[COUNT];
    private static final long[] controlPoints = new long[COUNT];
    private static final long[] previousControlPoints = new long[COUNT];
    private static long windowStarted = System.nanoTime();
    private static long lastOverlayUpdate;
    private static long previousDuration;
    private static boolean moving;
    private static boolean previousMoving;
    private static Vec3 lastCamera;
    private static float lastYaw;
    private static float lastPitch;

    /** Returns zero for calls outside the category's fixed sample. */
    public static long begin(int category) {
        long count = ++calls[category];
        return (count & (SAMPLE_INTERVAL[category] - 1)) == 0 ? System.nanoTime() : 0L;
    }

    public static void end(int category, long started) {
        if (started != 0L) {
            nanos[category] += System.nanoTime() - started;
            samples[category]++;
        }
    }

    public static void recordControlPoints(int category, long count) {
        controlPoints[category] += count;
    }

    public static String[] getDebugLines() {
        long now = System.nanoTime();
        if (lastOverlayUpdate == 0L || now - lastOverlayUpdate > 2_000_000_000L) {
            for (int i = 0; i < COUNT; i++) {
                calls[i] = 0L;
                samples[i] = 0L;
                nanos[i] = 0L;
                previousCalls[i] = 0L;
                previousSamples[i] = 0L;
                previousNanos[i] = 0L;
                controlPoints[i] = 0L;
                previousControlPoints[i] = 0L;
            }
            previousDuration = 0L;
            windowStarted = now;
            moving = false;
            lastCamera = null;
        }
        lastOverlayUpdate = now;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 position = camera.getPosition();
        if (lastCamera != null && (!lastCamera.equals(position)
                || lastYaw != camera.getYRot() || lastPitch != camera.getXRot())) {
            moving = true;
        }
        lastCamera = position;
        lastYaw = camera.getYRot();
        lastPitch = camera.getXRot();

        if (now - windowStarted >= 1_000_000_000L) {
            previousDuration = now - windowStarted;
            previousMoving = moving;
            moving = false;
            for (int i = 0; i < COUNT; i++) {
                previousCalls[i] = calls[i];
                previousSamples[i] = samples[i];
                previousNanos[i] = nanos[i];
                previousControlPoints[i] = controlPoints[i];
                calls[i] = 0L;
                samples[i] = 0L;
                nanos[i] = 0L;
                controlPoints[i] = 0L;
            }
            windowStarted = now;
        }
        String mode = previousDuration == 0L ? "warming" : previousMoving ? "motion seen" : "still";
        return new String[]{
                "IVR CPU " + mode + " estimated ms/s | BE "
                        + timing(BE) + " | shared-ray " + timing(RAY),
                "IVR CPU train-cull " + timing(TRAIN_CULL) + " | Rail-vis "
                        + timing(RAIL) + " | MSD-vis " + timing(MSD),
                "IVR actual BE-render " + timing(BE_RENDER)
                        + " | MSD-callback " + timingWithControlPoints(MSD_GEOMETRY)
        };
    }

    private static String timing(int category) {
        if (previousDuration == 0L) {
            return "warming";
        }
        double seconds = previousDuration / 1_000_000_000.0D;
        double rate = previousCalls[category] / seconds;
        if (previousCalls[category] == 0L) {
            return "unavailable (0/s)";
        }
        if (previousSamples[category] == 0L) {
            return "- (" + Math.round(rate) + "/s, n=0, 1/" + SAMPLE_INTERVAL[category] + ")";
        }
        double millis = previousNanos[category] / 1_000_000.0D
                * previousCalls[category] / previousSamples[category] / seconds;
        return String.format(Locale.ROOT, "%.2f (%d/s, n=%d, 1/%d)", millis,
                Math.round(rate), previousSamples[category], SAMPLE_INTERVAL[category]);
    }

    private static String timingWithControlPoints(int category) {
        String result = timing(category);
        if (previousDuration == 0L) {
            return result;
        }
        double seconds = previousDuration / 1_000_000_000.0D;
        return result.substring(0, result.length() - 1) + ", control-pts/s="
                + Math.round(previousControlPoints[category] / seconds) + ")";
    }
}
