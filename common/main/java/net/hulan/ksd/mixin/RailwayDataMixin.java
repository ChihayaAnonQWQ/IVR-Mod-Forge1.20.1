package net.hulan.ksd.mixin;

import io.netty.buffer.Unpooled;
import mtr.Registry;
import mtr.data.*;
import mtr.packet.IPacket;
import net.hulan.ksd.data.*;
import net.hulan.ksd.packet.KSDPacket;
import net.hulan.ksd.utils.DataUtilities;
import net.hulan.ksd.utils.KSDLoadState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.hulan.ksd.KSDMain.overworld;
import static net.hulan.ksd.KSDMain.the_nether;
import static net.hulan.ksd.KSDMain.the_end;

@Mixin(RailwayData.class)
public class RailwayDataMixin {

    @Shadow(remap = false)
    private Level world;

    /** Platform ids present before a removeSavedRailS2C() pass, see ivr$captureRemovedPlatforms. */
    private static final ThreadLocal<List<Long>> IVR$PLATFORM_IDS_BEFORE_REMOVE = new ThreadLocal<>();

    /**
     * Guards the KSD station mapping in DataCacheMixin while MTR is loading the
     * RailwayData of this dimension: calling RailwayData.getInstance() from inside
     * load() would re-enter load() and hang the server.
     */
    @Inject(method = "load", at = @At("HEAD"), remap = false)
    private void ivr$beginRailwayDataLoad(CompoundTag tag, CallbackInfo ci) {
        KSDLoadState.beginRailwayDataLoad();
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void ivr$endRailwayDataLoad(CompoundTag tag, CallbackInfo ci) {
        KSDLoadState.endRailwayDataLoad();

        // Once load() returns, MTR registers this RailwayData in the level's
        // DimensionDataStorage. Re-run the cache sync two ticks later (the instance
        // is registered by then) so the KSD-aware station mapping still applies.
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        final MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return;
        }
        final RailwayData self = (RailwayData) (Object) this;
        final Level level = world;
        server.tell(new TickTask(server.getTickCount() + 2, () -> {
            final RailwayData current = RailwayData.getInstance(level);
            if (current == self) {
                current.dataCache.sync();
            }
        }));
    }

    @Unique
    private static Station ivr$toMtrStation(KSDStation station) {
        // Never hide an MTR station from other addons (Shanghai/Tianjin sign GUIs call
        // this method). If KSD has no copy, or its copy has not received a name yet,
        // return null so the original MTR lookup runs instead.
        if (station == null || station.name == null || station.name.isEmpty()) {
            return null;
        }
        return station.toMTRStation();
    }

    @Inject(method = "getStation",
            remap = false,
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void getStation(Set<Station> stations, DataCache dataCache, BlockPos pos, CallbackInfoReturnable<Station> cir) {
        // Server-side lookup only. The client variant (ClientData.STATIONS /
        // KSDClientData) lives in RailwayDataClientMixin so that dedicated servers
        // never resolve client classes from this common mixin.
        RailwayData mtrOverworld = overworld == null ? null : RailwayData.getInstance(overworld);
        KSDRailwayData ksdOverworld = overworld == null ? null : KSDRailwayData.getInstance(overworld);
        RailwayData mtrTheNether = the_nether == null ? null : RailwayData.getInstance(the_nether);
        KSDRailwayData ksdTheNether = the_nether == null ? null : KSDRailwayData.getInstance(the_nether);
        RailwayData mtrTheEnd = the_end == null ? null : RailwayData.getInstance(the_end);
        KSDRailwayData ksdTheEnd = the_end == null ? null : KSDRailwayData.getInstance(the_end);
        if (ksdOverworld != null && mtrOverworld != null && stations == mtrOverworld.stations) {
            Station mtrStation = ivr$toMtrStation(KSDRailwayData.getStation(ksdOverworld.stations, pos));
            if (mtrStation != null) {
                cir.setReturnValue(mtrStation);
            }
        } else if (ksdTheNether != null && mtrTheNether != null && stations == mtrTheNether.stations) {
            Station mtrStation = ivr$toMtrStation(KSDRailwayData.getStation(ksdTheNether.stations, pos));
            if (mtrStation != null) {
                cir.setReturnValue(mtrStation);
            }
        } else if (ksdTheEnd != null && mtrTheEnd != null && stations == mtrTheEnd.stations) {
            Station mtrStation = ivr$toMtrStation(KSDRailwayData.getStation(ksdTheEnd.stations, pos));
            if (mtrStation != null) {
                cir.setReturnValue(mtrStation);
            }
        }
    }

    @Inject(method = "addRail(Ljava/util/Map;Ljava/util/Set;Ljava/util/Set;Lmtr/data/TransportMode;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lmtr/data/Rail;J)V",
            remap = false,
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Set;add(Ljava/lang/Object;)Z",
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private static void addRail(Map<BlockPos, Map<BlockPos, Rail>> rails,
                                Set<Platform> platforms,
                                Set<Siding> sidings,
                                TransportMode transportMode,
                                BlockPos posStart,
                                BlockPos posEnd,
                                Rail rail,
                                long savedRailId,
                                CallbackInfo ci) {
        // Server-side only; see RailwayDataClientMixin for the client KSD platform mirror.
        RailwayData mtrOverworld = overworld == null ? null : RailwayData.getInstance(overworld);
        KSDRailwayData ksdOverworld = overworld == null ? null : KSDRailwayData.getInstance(overworld);
        RailwayData mtrTheNether = the_nether == null ? null : RailwayData.getInstance(the_nether);
        KSDRailwayData ksdTheNether = the_nether == null ? null : KSDRailwayData.getInstance(the_nether);
        RailwayData mtrTheEnd = the_end == null ? null : RailwayData.getInstance(the_end);
        KSDRailwayData ksdTheEnd = the_end == null ? null : KSDRailwayData.getInstance(the_end);
        if (ksdOverworld != null && mtrOverworld != null && platforms == mtrOverworld.platforms) {
            ksdOverworld.platforms.add(new KSDPlatform(savedRailId, transportMode, posStart, posEnd));
            ksdOverworld.dataCache.sync();
        } else if (ksdTheNether != null && mtrTheNether != null && platforms == mtrTheNether.platforms) {
            ksdTheNether.platforms.add(new KSDPlatform(savedRailId, transportMode, posStart, posEnd));
            ksdTheNether.dataCache.sync();
        } else if (ksdTheEnd != null && mtrTheEnd != null && platforms == mtrTheEnd.platforms) {
            ksdTheEnd.platforms.add(new KSDPlatform(savedRailId, transportMode, posStart, posEnd));
            ksdTheEnd.dataCache.sync();
        }
    }

    // The old injection targeted MTR's compiler-generated lambda method
    // ("lambda$removeSavedRailS2C$39"); its number changes between MTR builds
    // (Yomi's MTR 3.6.x uses $37). Snapshot the platform ids around the real
    // method instead so the hook works on every MTR 3.x build.
    @Inject(method = "removeSavedRailS2C", at = @At("HEAD"), remap = false)
    private static void ivr$captureRemovedPlatforms(Level world,
                                                    Set<? extends SavedRailBase> savedRails,
                                                    Map<BlockPos, Map<BlockPos, Rail>> rails,
                                                    ResourceLocation packetId,
                                                    CallbackInfo ci) {
        if (IPacket.PACKET_DELETE_PLATFORM.equals(packetId)) {
            final List<Long> ids = new ArrayList<>();
            for (final SavedRailBase savedRail : savedRails) {
                if (savedRail instanceof Platform) {
                    ids.add(savedRail.id);
                }
            }
            IVR$PLATFORM_IDS_BEFORE_REMOVE.set(ids);
        } else {
            IVR$PLATFORM_IDS_BEFORE_REMOVE.remove();
        }
    }

    @Inject(method = "removeSavedRailS2C", at = @At("RETURN"), remap = false)
    private static void ivr$removeKSDPlatforms(Level world,
                                               Set<? extends SavedRailBase> savedRails,
                                               Map<BlockPos, Map<BlockPos, Rail>> rails,
                                               ResourceLocation packetId,
                                               CallbackInfo ci) {
        final List<Long> idsBefore = IVR$PLATFORM_IDS_BEFORE_REMOVE.get();
        IVR$PLATFORM_IDS_BEFORE_REMOVE.remove();
        if (idsBefore == null || idsBefore.isEmpty()) {
            return;
        }
        final Set<Long> idsAfter = new HashSet<>();
        for (final SavedRailBase savedRail : savedRails) {
            idsAfter.add(savedRail.id);
        }
        for (final Long removedId : idsBefore) {
            if (idsAfter.contains(removedId)) {
                continue;
            }
            final KSDRailwayData railwayData = KSDRailwayData.getInstance(world);
            if (railwayData == null) {
                continue;
            }
            final KSDPlatform platform = DataUtilities.getPlatform(railwayData.platforms, removedId);
            if (platform == null) {
                continue;
            }
            railwayData.platforms.remove(platform);
            railwayData.dataCache.sync();
            final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
            packet.writeLong(platform.id);
            world.players().forEach((player) -> Registry.sendToPlayer((ServerPlayer) player, KSDPacket.KSD_PACKET_DELETE_PLATFORM, packet));
        }
    }
}
