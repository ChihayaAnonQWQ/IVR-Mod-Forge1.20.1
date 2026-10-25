package net.hulan.ksd.utils;

/**
 * Tracks whether MTR is currently loading a {@code RailwayData} instance on this thread.
 *
 * <p>MTR's {@code RailwayData.load(tag)} calls {@code dataCache.sync()}, which calls
 * {@code DataCache.mapSavedRailIdToStation(...)}. KSD mixes into that method and, on the
 * server side, calls {@code RailwayData.getInstance(level)} / {@code KSDRailwayData.getInstance(level)}
 * to locate the KSD data of the dimension being synced. When this happens while MTR is
 * still constructing that very {@code RailwayData} (it is not registered in the level's
 * {@code DimensionDataStorage} yet), {@code getInstance} re-enters {@code load} and the
 * server hangs in an endless load loop.</p>
 *
 * <p>KSD therefore skips its custom station mapping while MTR is loading and re-runs
 * {@code dataCache.sync()} on the next server ticks, when the instance is registered.
 * This is required for both official MTR 3.2.2 and the Yomi's MTR 3.6.x fork.</p>
 */
public final class KSDLoadState {

    private static final ThreadLocal<Boolean> RAILWAY_DATA_LOADING = new ThreadLocal<>();

    private KSDLoadState() {
    }

    public static void beginRailwayDataLoad() {
        RAILWAY_DATA_LOADING.set(Boolean.TRUE);
    }

    public static void endRailwayDataLoad() {
        RAILWAY_DATA_LOADING.remove();
    }

    public static boolean isRailwayDataLoading() {
        return Boolean.TRUE.equals(RAILWAY_DATA_LOADING.get());
    }
}
