package net.hulan.ivr.utils;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Render-thread counters for IVR's block entity decisions. */
public final class BlockEntityCullDebug {

    public enum Result {
        VIEW_CULLED,
        OCCLUDED,
        ALLOWED,
        NEAR,
        RAY_CLEAR,
        PENDING
    }

    private static long windowStarted = System.nanoTime();
    private static final int[] current = new int[Result.values().length];
    private static final int[] previous = new int[Result.values().length];
    private static int upstreamCurrent;
    private static int upstreamPrevious;
    private static final Set<BlockEntity> uniqueCurrent = Collections.newSetFromMap(new IdentityHashMap<>());
    private static int uniquePrevious;

    private BlockEntityCullDebug() {
    }

    public static void record(BlockEntity entity, Result result) {
        rollWindow();
        current[result.ordinal()]++;
        if (entity != null) {
            uniqueCurrent.add(entity);
        }
    }

    public static String getDebugLine() {
        rollWindow();
        int checked = 0;
        for (int count : previous) {
            checked += count;
        }
        return "IVR BE/s calls: upstream " + upstreamPrevious + " | dispatcher " + checked
                + " | unique " + uniquePrevious
                + " | view " + previous[Result.VIEW_CULLED.ordinal()]
                + " | occluded " + previous[Result.OCCLUDED.ordinal()]
                + " | near " + previous[Result.NEAR.ordinal()]
                + " | ray-clear " + previous[Result.RAY_CLEAR.ordinal()]
                + " | unknown " + previous[Result.ALLOWED.ordinal()]
                + " | pending " + previous[Result.PENDING.ordinal()];
    }

    private static void rollWindow() {
        long now = System.nanoTime();
        if (now - windowStarted >= 1_000_000_000L) {
            upstreamPrevious = upstreamCurrent;
            upstreamCurrent = 0;
            uniquePrevious = uniqueCurrent.size();
            uniqueCurrent.clear();
            for (int i = 0; i < current.length; i++) {
                previous[i] = current[i];
                current[i] = 0;
            }
            windowStarted = now;
        }
    }
}
