package net.hulan.ivr;

import mtr.RegistryClient;
import mtr.packet.IPacket;
import net.hulan.ivr.packet.IVRPacket;
import net.hulan.ivr.packet.IVRPacketTrainDataGuiClient;
import net.hulan.ivr.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.hulan.ivr.platform.NPCPlatform;
import net.hulan.ivr.block.BlockKCRClock;
import net.hulan.ivr.utils.BlockEntityVisibility;
import net.minecraft.world.phys.AABB;

public class IVRClient implements IVRPacket, IVRBlockEntityTypes, IVRBlocks, IPacket {

    public static void init() {
        NPCPlatform.getInstance().registerClientRenderers();
        BlockEntityVisibility.registerStaticBoundsProvider(BlockKCRClock.TileEntityKCRClock.class,
                entity -> new AABB(entity.getBlockPos()));
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_CLOCK.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), IVR_LOGO.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_PLATFORM_INDENTED.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_APG_DOOR.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_APG_GLASS.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_APG_GLASS_END.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_PSD_DOOR_1.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_PSD_GLASS_1.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_PSD_GLASS_END_1.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_PSD_DOOR_2.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_PSD_GLASS_2.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_PSD_GLASS_END_2.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_STATION_NAME_TALL_BLOCK.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_STATION_NAME_TALL_BLOCK_DOUBLE_SIDED.get());
        RegistryClient.registerBlockRenderType(RenderType.cutout(), KCR_STATION_NAME_TALL_WALL.get());
        RegistryClient.registerBlockColors(KCR_STATION_COLOR_STATION_WALL.get());
        RegistryClient.registerBlockColors(KCR_STATION_COLOR_STATION_WALL_SLAB.get());
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_1_EVEN_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_1_ODD_TILE_ENTITY.get(), RenderClassicalSign1Odd::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_2_EVEN_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_2_ODD_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_3_EVEN_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_3_ODD_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_4_EVEN_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_4_ODD_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_5_EVEN_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_5_ODD_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_6_EVEN_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_6_ODD_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_7_EVEN_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(CLASSICAL_SIGN_7_ODD_TILE_ENTITY.get(), RenderClassicalSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_1_EVEN_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_1_ODD_TILE_ENTITY.get(), RenderModernSign1Odd::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_2_EVEN_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_2_ODD_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_3_EVEN_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_3_ODD_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_4_EVEN_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_4_ODD_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_5_EVEN_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_5_ODD_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_6_EVEN_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_6_ODD_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_7_EVEN_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(MODERN_SIGN_7_ODD_TILE_ENTITY.get(), RenderModernSign::new);
        RegistryClient.registerTileEntityRenderer(KCR_CLOCK_TILE_ENTITY.get(), RenderKCRClock::new);
        RegistryClient.registerTileEntityRenderer(KCR_PSD_DOOR_1_TILE_ENTITY.get(), dispatcher -> new RenderKCRPSDAPGDoor<>(dispatcher, 0));
        RegistryClient.registerTileEntityRenderer(KCR_PSD_DOOR_2_TILE_ENTITY.get(), dispatcher -> new RenderKCRPSDAPGDoor<>(dispatcher, 1));
        RegistryClient.registerTileEntityRenderer(KCR_PSD_TOP_TILE_ENTITY.get(), RenderKCRPSDTop::new);
        RegistryClient.registerTileEntityRenderer(KCR_APG_GLASS_TILE_ENTITY.get(), RenderKCRAPGGlass::new);
        RegistryClient.registerTileEntityRenderer(KCR_APG_DOOR_TILE_ENTITY.get(), dispatcher -> new RenderKCRPSDAPGDoor<>(dispatcher, 2));
        RegistryClient.registerTileEntityRenderer(KCR_STATION_NAME_ENTRANCE_TILE_ENTITY.get(), (dispatcher) -> new RenderKCRStationNameTiled<>(dispatcher, true));
        RegistryClient.registerTileEntityRenderer(KCR_STATION_NAME_TALL_BLOCK_TILE_ENTITY.get(), RenderKCRStationNameTall::new);
        RegistryClient.registerTileEntityRenderer(KCR_STATION_NAME_TALL_BLOCK_DOUBLE_SIDED_TILE_ENTITY.get(), RenderKCRStationNameTall::new);
        RegistryClient.registerTileEntityRenderer(KCR_STATION_NAME_TALL_WALL_TILE_ENTITY.get(), RenderKCRStationNameTall::new);
        RegistryClient.registerTileEntityRenderer(KCR_STATION_NAME_WALL_WHITE_TILE_ENTITY.get(), (dispatcher) -> new RenderKCRStationNameTiled<>(dispatcher, false));
        RegistryClient.registerTileEntityRenderer(KCR_STATION_NAME_WALL_GRAY_TILE_ENTITY.get(), (dispatcher) -> new RenderKCRStationNameTiled<>(dispatcher, false));
        RegistryClient.registerTileEntityRenderer(KCR_STATION_NAME_WALL_BLACK_TILE_ENTITY.get(), (dispatcher) -> new RenderKCRStationNameTiled<>(dispatcher, false));
        RegistryClient.registerTileEntityRenderer(MODERN_ROUTE_SIGN_TILE_ENTITY.get(), RenderModernRouteSign::new);
        RegistryClient.registerNetworkReceiver(PACKET_OPEN_CLASSICAL_SIGN_SCREEN, packet -> IVRPacketTrainDataGuiClient.openClassicalSignScreenS2C(Minecraft.getInstance(), packet));
        RegistryClient.registerNetworkReceiver(PACKET_OPEN_CLASSICAL_1ODD_SIGN_SCREEN, packet -> IVRPacketTrainDataGuiClient.openClassicalSign1OddScreenS2C(Minecraft.getInstance(), packet));
        RegistryClient.registerNetworkReceiver(PACKET_OPEN_MODERN_SIGN_SCREEN, packet -> IVRPacketTrainDataGuiClient.openModernSignScreenS2C(Minecraft.getInstance(), packet));
        RegistryClient.registerNetworkReceiver(PACKET_OPEN_MODERN_1ODD_SIGN_SCREEN, packet -> IVRPacketTrainDataGuiClient.openModernSign1OddScreenS2C(Minecraft.getInstance(), packet));
    }
}
