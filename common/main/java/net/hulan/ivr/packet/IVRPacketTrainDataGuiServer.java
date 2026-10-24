package net.hulan.ivr.packet;

import io.netty.buffer.Unpooled;
import mtr.Registry;
import mtr.data.*;
import mtr.mappings.BlockEntityMapper;
import mtr.packet.PacketTrainDataBase;
import net.hulan.ivr.block.BlockClassicalSign;
import net.hulan.ivr.block.BlockKCRRouteSignBase;
import net.hulan.ivr.block.BlockModernSign;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;
import java.util.function.Consumer;

public class IVRPacketTrainDataGuiServer extends PacketTrainDataBase implements IVRPacket {

    public static void openClassicalSignScreenS2C(ServerPlayer player, BlockPos signPos) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer(player, PACKET_OPEN_CLASSICAL_SIGN_SCREEN, packet);
    }

    public static void openClassicalSign1OddScreenS2C(ServerPlayer player, BlockPos signPos) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer(player, PACKET_OPEN_CLASSICAL_1ODD_SIGN_SCREEN, packet);
    }

    public static void openModernSignScreenS2C(ServerPlayer player, BlockPos signPos) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer(player, IVRPacket.PACKET_OPEN_MODERN_SIGN_SCREEN, packet);
    }

    public static void openModernSign1OddScreenS2C(ServerPlayer player, BlockPos signPos) {
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(signPos);
        Registry.sendToPlayer(player, PACKET_OPEN_MODERN_1ODD_SIGN_SCREEN, packet);
    }

    public static void receiveClassicalSignIdsC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        final BlockPos signPos = packet.readBlockPos();
        final int selectedIdsLength = packet.readInt();
        final Set<Long> selectedIds = new HashSet<>();
        for (int i = 0; i < selectedIdsLength; i++) {
            selectedIds.add(packet.readLong());
        }
        final int signLength = packet.readInt();
        final String[] signIds = new String[signLength];
        for (int i = 0; i < signLength; i++) {
            final String signId = packet.readUtf(SerializedDataBase.PACKET_STRING_READ_LENGTH);
            signIds[i] = signId.isEmpty() ? null : signId;
        }
        final boolean luminance = packet.readBoolean();
        minecraftServer.execute(() -> {
            final BlockEntity entity = player.level().getBlockEntity(signPos);
            if (entity instanceof BlockClassicalSign.TileEntityClassicalSign) {
                setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setData(selectedIds, signIds, luminance), (BlockClassicalSign.TileEntityClassicalSign) entity);
            } else if (entity instanceof BlockKCRRouteSignBase.TileEntityKCRRouteSignBase) {
                final long platformId = selectedIds.isEmpty() ? 0 : (long) selectedIds.toArray()[0];
                final BlockEntity entityAbove = player.level().getBlockEntity(signPos.above());
                if (entityAbove instanceof BlockKCRRouteSignBase.TileEntityKCRRouteSignBase) {
                    setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setPlatformId(platformId), ((BlockKCRRouteSignBase.TileEntityKCRRouteSignBase) entityAbove), (BlockKCRRouteSignBase.TileEntityKCRRouteSignBase) entity);
                } else {
                    setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setPlatformId(platformId), (BlockKCRRouteSignBase.TileEntityKCRRouteSignBase) entity);
                }
            }
        });
    }

    public static void receiveClassicalSign1OddIdsC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        final BlockPos signPos = packet.readBlockPos();
        final int selectedIds1Length = packet.readInt();
        final Set<Long> selectedIds1 = new HashSet<>();
        for (int i = 0; i < selectedIds1Length; i++) {
            selectedIds1.add(packet.readLong());
        }
        packet.readInt();
        final String[] signId1 = new String[]{packet.readUtf(SerializedDataBase.PACKET_STRING_READ_LENGTH)};
        final int selectedIds2Length = packet.readInt();
        final Set<Long> selectedIds2 = new HashSet<>();
        for (int i = 0; i < selectedIds2Length; i++) {
            selectedIds2.add(packet.readLong());
        }
        packet.readInt();
        final String[] signId2 = new String[]{packet.readUtf(SerializedDataBase.PACKET_STRING_READ_LENGTH)};
        final boolean luminance = packet.readBoolean();
        minecraftServer.execute(() -> {
            final BlockEntity entity = player.level().getBlockEntity(signPos);
            if (entity instanceof BlockClassicalSign.TileEntityClassicalSign1Odd) {
                setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setData(selectedIds1, signId1, selectedIds2, signId2, luminance), (BlockClassicalSign.TileEntityClassicalSign1Odd) entity);
            }
        });
    }

    public static void receiveModernSignIdsC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        BlockPos signPos = packet.readBlockPos();
        int selectedIdsLength = packet.readInt();
        Set<Long> selectedIds = new HashSet<>();
        int signLength;
        for(signLength = 0; signLength < selectedIdsLength; ++signLength) {
            selectedIds.add(packet.readLong());
        }
        signLength = packet.readInt();
        String[] signIds = new String[signLength];
        for(int i = 0; i < signLength; ++i) {
            String signId = packet.readUtf(32767);
            signIds[i] = signId.isEmpty() ? null : signId;
        }
        final boolean luminance = packet.readBoolean();
        minecraftServer.execute(() -> {
            BlockEntity entity = player.level().getBlockEntity(signPos);
            if (entity instanceof BlockModernSign.TileEntityModernSign) {
                setTileEntityDataAndWriteUpdate(player, (entity2) -> entity2.setData(selectedIds, signIds, luminance), (BlockModernSign.TileEntityModernSign)entity);
            } else if (entity instanceof BlockKCRRouteSignBase.TileEntityKCRRouteSignBase) {
                long platformId = selectedIds.isEmpty() ? 0L : (Long)selectedIds.toArray()[0];
                BlockEntity entityAbove = player.level().getBlockEntity(signPos.above());
                if (entityAbove instanceof BlockKCRRouteSignBase.TileEntityKCRRouteSignBase) {
                    setTileEntityDataAndWriteUpdate(player, (entity2) -> entity2.setPlatformId(platformId), (BlockKCRRouteSignBase.TileEntityKCRRouteSignBase)entityAbove, (BlockKCRRouteSignBase.TileEntityKCRRouteSignBase)entity);
                } else {
                    setTileEntityDataAndWriteUpdate(player, (entity2) -> entity2.setPlatformId(platformId), (BlockKCRRouteSignBase.TileEntityKCRRouteSignBase)entity);
                }
            }
        });
    }

    public static void receiveModernSign1OddIdsC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        final BlockPos signPos = packet.readBlockPos();
        final int selectedIds1Length = packet.readInt();
        final Set<Long> selectedIds1 = new HashSet<>();
        for (int i = 0; i < selectedIds1Length; i++) {
            selectedIds1.add(packet.readLong());
        }
        packet.readInt();
        final String[] signId1 = new String[]{packet.readUtf(SerializedDataBase.PACKET_STRING_READ_LENGTH)};
        final int selectedIds2Length = packet.readInt();
        final Set<Long> selectedIds2 = new HashSet<>();
        for (int i = 0; i < selectedIds2Length; i++) {
            selectedIds2.add(packet.readLong());
        }
        packet.readInt();
        final String[] signId2 = new String[]{packet.readUtf(SerializedDataBase.PACKET_STRING_READ_LENGTH)};
        final boolean luminance = packet.readBoolean();
        minecraftServer.execute(() -> {
            final BlockEntity entity = player.level().getBlockEntity(signPos);
            if (entity instanceof BlockModernSign.TileEntityModernSign1Odd) {
                setTileEntityDataAndWriteUpdate(player, entity2 -> entity2.setData(selectedIds1, signId1, selectedIds2, signId2, luminance), (BlockModernSign.TileEntityModernSign1Odd) entity);
            }
        });
    }

    @SafeVarargs
    private static <T extends BlockEntityMapper> void setTileEntityDataAndWriteUpdate(ServerPlayer player, Consumer<T> setData, T... entities) {
        final RailwayData railwayData = RailwayData.getInstance(player.level());
        if (railwayData != null && entities.length > 0) {
            final CompoundTag compoundTagOld = new CompoundTag();
            entities[0].writeCompoundTag(compoundTagOld);
            BlockPos blockPos = null;
            long posLong = 0;
            for (final T entity : entities) {
                setData.accept(entity);
                final BlockPos entityPos = entity.getBlockPos();
                if (blockPos == null || entityPos.asLong() > posLong) {
                    blockPos = entityPos;
                    posLong = entityPos.asLong();
                }
            }
            final CompoundTag compoundTagNew = new CompoundTag();
            entities[0].writeCompoundTag(compoundTagNew);
            railwayData.railwayDataLoggingModule.addEvent(player, entities[0].getClass(), RailwayDataLoggingModule.getData(compoundTagOld), RailwayDataLoggingModule.getData(compoundTagNew), blockPos);
        }
    }
}
