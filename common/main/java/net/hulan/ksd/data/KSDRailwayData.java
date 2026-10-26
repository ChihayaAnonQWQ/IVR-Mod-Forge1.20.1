package net.hulan.ksd.data;

import mtr.data.RailwayData;
import mtr.mappings.PersistentStateMapper;
import net.hulan.ksd.packet.KSDPacketServer;
import net.hulan.ksd.utils.DataUtilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class KSDRailwayData extends PersistentStateMapper {

    public final Set<KSDStation> stations = new HashSet<>();
    public final Set<KSDPlatform> platforms = new HashSet<>();
    public final Set<KSDRoute> routes = new HashSet<>();
    public final JSONDataManager jsonDataManager;
    public final KSDDataCache dataCache;
    private final Level world;
    private final KSDRailwayDataFileSaveModule railwayDataFileSaveModule;
    public final KSDRailwayDataLoggingModule railwayDataLoggingModule;
    private boolean syncedFromMTR;
    private static final String NAME = "ksd_train_data";
    private static final String KEY_STATIONS = "stations";
    private static final String KEY_PLATFORMS = "platforms";
    private static final String KEY_ROUTES = "routes";

    public KSDRailwayData(Level world) {
        super(NAME);
        this.world = world;
        dataCache = new KSDDataCache(stations, platforms, routes);
        ResourceLocation dimensionLocation = world.dimension().location();
        Path savePath = ((ServerLevel) world).getServer().getWorldPath(LevelResource.ROOT).resolve("ksd").resolve(dimensionLocation.getNamespace()).resolve(dimensionLocation.getPath());
        railwayDataFileSaveModule = new KSDRailwayDataFileSaveModule(this, world, savePath);
        railwayDataLoggingModule = new KSDRailwayDataLoggingModule(savePath);
        jsonDataManager = new JSONDataManager((ServerLevel) world);
    }

    public static KSDRailwayData getInstance(Level world) {
        return getInstance(world, () -> new KSDRailwayData(world), NAME);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        if (compoundTag.contains("raw_message_pack")) {
            try {
                MessageUnpacker messageUnpacker = MessagePack.newDefaultUnpacker(compoundTag.getByteArray("raw_message_pack"));
                int mapSize = messageUnpacker.unpackMapHeader();

                for(int i = 0; i < mapSize; ++i) {
                    String key = messageUnpacker.unpackString();
                    if (key.equals("mtr_data_version")) {
                        if (messageUnpacker.unpackInt() > 1) {
                            throw new IllegalArgumentException("Unsupported data version");
                        }
                    } else {
                        int arraySize = messageUnpacker.unpackArrayHeader();
                        switch (key) {
                            case KEY_STATIONS -> {
                                for (int j = 0; j < arraySize; ++j) {
                                    stations.add(new KSDStation(readMessagePackSKMap(messageUnpacker)));
                                }
                            }
                            case KEY_PLATFORMS -> {
                                for (int j = 0; j < arraySize; ++j) {
                                    platforms.add(new KSDPlatform(readMessagePackSKMap(messageUnpacker)));
                                }
                            }
                            case KEY_ROUTES -> {
                                for (int j = 0; j < arraySize; ++j) {
                                    routes.add(new KSDRoute(readMessagePackSKMap(messageUnpacker)));
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                final CompoundTag tagStations = compoundTag.getCompound(KEY_STATIONS);
                for (final String key : tagStations.getAllKeys()) {
                    stations.add(new KSDStation(tagStations.getCompound(key)));
                }
                final CompoundTag tagNewPlatforms = compoundTag.getCompound(KEY_PLATFORMS);
                for (final String key : tagNewPlatforms.getAllKeys()) {
                    platforms.add(new KSDPlatform(tagNewPlatforms.getCompound(key)));
                }
                final CompoundTag tagNewRoutes = compoundTag.getCompound(KEY_ROUTES);
                for (final String key : tagNewRoutes.getAllKeys()) {
                    routes.add(new KSDRoute(tagNewRoutes.getCompound(key)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        railwayDataFileSaveModule.load();
        jsonDataManager.onLoad();
        syncedFromMTR = compoundTag.getBoolean("synced_from_mtr");
        syncFromMTR();
        dataCache.sync();
    }

    public boolean onPlayerJoin(ServerPlayer serverPlayer) {
        // MTR may finish loading its RailwayData after this persistent state is created.
        // Retry here so the first join does not receive an empty snapshot forever.
        syncFromMTR();
        dataCache.sync();
        KSDPacketServer.sendAllInChunks(serverPlayer, stations, platforms, routes);
        return !stations.isEmpty() || !platforms.isEmpty() || !routes.isEmpty();
    }

    public static boolean hasNoPermission(ServerPlayer serverPlayer) {
        return !hasPermission(serverPlayer.gameMode.getGameModeForPlayer());
    }

    public static boolean hasPermission(GameType gameType) {
        return gameType == GameType.CREATIVE || gameType == GameType.SURVIVAL;
    }

    @Override
    public void save(File file) {
        MinecraftServer minecraftServer = ((ServerLevel) world).getServer();
        if (!minecraftServer.isStopped() && minecraftServer.isRunning()) {
            railwayDataFileSaveModule.autoSave();
        } else {
            railwayDataFileSaveModule.fullSave();
        }
        railwayDataLoggingModule.save();
        jsonDataManager.onSave();
        setDirty();
        super.save(file);
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag compoundTag) {
        compoundTag.putBoolean("synced_from_mtr", syncedFromMTR);
        return compoundTag;
    }

    public void syncFromMTR() {
        try {
            RailwayData railwayData = RailwayData.getInstance(world);
            if (syncedFromMTR && (!stations.isEmpty() || !platforms.isEmpty() || !routes.isEmpty())) {
                return;
            }
            if (railwayData != null && (!railwayData.stations.isEmpty()
                    || !railwayData.platforms.isEmpty()
                    || !railwayData.routes.isEmpty())) {
                stations.clear();
                stations.addAll(KSDStation.fromMTRStations(railwayData.stations));
                platforms.clear();
                platforms.addAll(KSDPlatform.fromMTRPlatforms(railwayData.platforms));
                routes.clear();
                routes.addAll(KSDRoute.fromMTRRoutes(railwayData.routes));
                syncedFromMTR = true;
                setDirty();
            }
        } catch (Exception e) {
            e.printStackTrace();
            syncedFromMTR = false;
        }
    }

    public static KSDStation getStation(Set<KSDStation> stations, BlockPos pos) {
        try {
            return DataUtilities.getOrNull(stations, station -> station.inArea(pos));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static long getClosePlatformId(Set<KSDPlatform> platforms, KSDDataCache dataCache, BlockPos pos) {
        return getClosePlatformId(platforms, dataCache, pos, 5, 0, 4);
    }

    public static long getClosePlatformId(Set<KSDPlatform> platforms, KSDDataCache dataCache, BlockPos pos, int radius, int lower, int upper) {
        try {
            long posLong = pos.asLong();
            if (dataCache.blockPosToPlatformId.containsKey(posLong)) {
                return dataCache.blockPosToPlatformId.get(posLong);
            } else {
                long platformId = 0L;

                for(int i = 1; i <= radius; ++i) {
                    int finalI = i;
                    platformId = platforms.stream().filter((platform) -> platform.isCloseToSavedRail(pos, finalI, lower, upper)).min(Comparator.comparingInt((platform) -> platform.getMidPos().distManhattan(pos))).map((platform) -> platform.id).orElse(0L);
                    if (platformId != 0L) {
                        break;
                    }
                }

                dataCache.blockPosToPlatformId.put(posLong, platformId);
                return platformId;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    private static Map<String, Value> readMessagePackSKMap(MessageUnpacker messageUnpacker) throws IOException {
        int size = messageUnpacker.unpackMapHeader();
        HashMap<String, Value> result = new HashMap<>(size);
        for(int i = 0; i < size; ++i) {
            result.put(messageUnpacker.unpackString(), messageUnpacker.unpackValue());
        }
        return result;
    }
}
