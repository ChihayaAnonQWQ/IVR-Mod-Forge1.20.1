package net.hulan.ksd;

import mtr.MTR;
import mtr.RegistryObject;
import net.hulan.ksd.data.*;
import net.hulan.ksd.packet.KSDPacket;
import net.hulan.ksd.packet.KSDPacketServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class KSDMain implements KSDBlocks, KSDItems, KSDCreativeModTabs, KSDPacket {

    public static final String MOD_ID = "ksd";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void init(BiConsumer<String, RegistryObject<Item>> registerItem,
                            BiConsumer<String, RegistryObject<Block>> registerBlock,
                            MTR.RegisterBlockItem registerBlockItem) {
        registerBlock.accept("kp_cell_side", KP_CELL_SIDE);
        registerBlock.accept("kp_cell_side_with_light", KP_CELL_SIDE_WITH_LIGHT);
        registerItem.accept("kp_cell_side_is", KP_CELL_SIDE_IS);
        registerItem.accept("kp_cell_side_os", KP_CELL_SIDE_OS);
        registerItem.accept("kp_cell_side_os_with_light", KP_CELL_SIDE_OS_WITH_LIGHT);
        registerItem.accept("ksd_dashboard", KSD_DASHBOARD);
        registerItem.accept("octopus", OCTOPUS);
        registerItem.accept("single_ticket", SINGLE_TICKET);
        registerBlockItem.accept("kp_pole_normal", KP_POLE_NORMAL, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_pole_single_side", KP_POLE_1_SIDE, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_pole_double_side", KP_POLE_2_SIDE, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_horizontal_crossbar", KP_HORIZONTAL_CROSSBAR, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_horizontal_lower_crossbar", KP_HORIZONTAL_LOWER_CROSSBAR, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_horizontal_ending_crossbar", KP_HORIZONTAL_ENDING_CROSSBAR, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_longitudinal_crossbar", KP_LONGITUDINAL_CROSSBAR, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_hb_with_lb", KP_HB_WITH_LB, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_hb_with_hb", KP_HB_WITH_HB, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_cell_top", KP_CELL_TOP, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("kp_cell_light", KP_LIGHT, KCR_PLATFORM_BLOCKS);
        registerBlockItem.accept("first_class_processor", FIRST_CLASS_PROCESSOR, KCR_PLATFORM_BLOCKS);
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_PURCHASE_ST, KSDPacketServer::receivePurchaseSTC2S);
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_ADJUST_ST_FARE, KSDPacketServer::receiveAdjustSTFareC2S);
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_ADJUST_OCTOPUS_FARE, KSDPacketServer::receiveAdjustOctopusFareC2S);
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_ADJUST_FC_FARE, (minecraftServer, player, packet) ->
                KSDPacketServer.receiveAdjustFCFareC2S(minecraftServer, player));
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_PAYMENT, KSDPacketServer::receivePayment);
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_PURCHASE_OCTOPUS, KSDPacketServer::receivePurchaseOctopusC2S);
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_ADD_VALUE, KSDPacketServer::receiveOctopusAddValueC2S);
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_NPC_INTERACTION, KSDPacketServer::receiveNPCInteraction);
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_UPDATE_STATION,
                (minecraftServer, player, packet) -> KSDPacketServer.receiveUpdateOrDeleteC2S(
                        minecraftServer,
                        player,
                        packet,
                        KSD_PACKET_UPDATE_STATION,
                        (railwayData) -> railwayData.stations,
                        (railwayData) -> railwayData.dataCache.stationIdMap,
                        KSDStation::new,
                        false));
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_UPDATE_PLATFORM,
                (minecraftServer, player, packet) -> KSDPacketServer.receiveUpdateOrDeleteC2S(
                        minecraftServer,
                        player,
                        packet,
                        KSD_PACKET_UPDATE_PLATFORM,
                        railwayData -> railwayData.platforms,
                        railwayData -> railwayData.dataCache.platformIdMap,
                        null,
                        false));
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_UPDATE_ROUTE,
                (minecraftServer, player, packet) -> KSDPacketServer.receiveUpdateOrDeleteC2S(
                        minecraftServer,
                        player,
                        packet,
                        KSD_PACKET_UPDATE_ROUTE,
                        railwayData -> railwayData.routes,
                        railwayData -> railwayData.dataCache.routeIdMap,
                        KSDRoute::new,
                        false));
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_DELETE_STATION,
                (minecraftServer, player, packet) -> KSDPacketServer.receiveUpdateOrDeleteC2S(
                        minecraftServer,
                        player,
                        packet,
                        KSD_PACKET_DELETE_STATION,
                        (railwayData) -> railwayData.stations,
                        (railwayData) -> railwayData.dataCache.stationIdMap,
                        null,
                        true));
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_DELETE_PLATFORM,
                (minecraftServer, player, packet) -> KSDPacketServer.receiveUpdateOrDeleteC2S(
                        minecraftServer,
                        player,
                        packet,
                        KSD_PACKET_DELETE_PLATFORM,
                        railwayData -> railwayData.platforms,
                        railwayData -> railwayData.dataCache.platformIdMap,
                        null,
                        true));
        mtr.Registry.registerNetworkReceiver(KSD_PACKET_DELETE_ROUTE,
                (minecraftServer, player, packet) -> KSDPacketServer.receiveUpdateOrDeleteC2S(
                        minecraftServer,
                        player,
                        packet,
                        KSD_PACKET_DELETE_ROUTE,
                        railwayData -> railwayData.routes,
                        railwayData -> railwayData.dataCache.routeIdMap,
                        null,
                        true));
        mtr.Registry.registerTickEvent(playerTick -> pendingInitialSync.entrySet().removeIf(entry -> {
            ServerPlayer player = playerTick.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                // The join callback can run before the player is visible in the list.
                return false;
            }
            if (entry.getValue() <= 0) {
                KSDRailwayData railwayData = KSDRailwayData.getInstance(player.level());
                if (railwayData != null) {
                    boolean synced = railwayData.onPlayerJoin(player);
                    // Large saves can take several seconds before MTR's RailwayData is
                    // ready. Sending an empty snapshot once would leave the KSD client
                    // (and therefore IVR's own signs/dashboard) empty for the whole
                    // session, while other addons read MTR's data normally. Retry for a
                    // while until MTR data is available.
                    int retries = pendingInitialSyncRetries.getOrDefault(entry.getKey(), 0);
                    if (!synced && retries < 120) {
                        pendingInitialSyncRetries.put(entry.getKey(), retries + 1);
                        entry.setValue(10);
                        return false;
                    }
                }
                pendingInitialSyncRetries.remove(entry.getKey());
                return true;
            }
            entry.setValue(entry.getValue() - 1);
            return false;
        }));
        mtr.Registry.registerPlayerJoinEvent((player) -> {
            KSDRailwayData ksdRailwayData = KSDRailwayData.getInstance(player.level());
            if (ksdRailwayData != null) {
                // Wait until the client has finished entering the world, then send one snapshot.
                pendingInitialSync.put(player.getUUID(), 5);
                pendingInitialSyncRetries.put(player.getUUID(), 0);
            }
        });
        mtr.Registry.registerServerStartingEvent(server -> {
            overworld = server.overworld();
            the_nether = server.getLevel(Level.NETHER);
            the_end = server.getLevel(Level.END);
        });
    }

    public static ServerLevel overworld;
    public static ServerLevel the_nether;
    public static ServerLevel the_end;
    private static final Map<UUID, Integer> pendingInitialSync = new HashMap<>();
    private static final Map<UUID, Integer> pendingInitialSyncRetries = new HashMap<>();
}
