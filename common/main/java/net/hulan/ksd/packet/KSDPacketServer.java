package net.hulan.ksd.packet;

import io.netty.buffer.Unpooled;
import mtr.Registry;
import mtr.data.*;
import mtr.mappings.Utilities;
import mtr.packet.PacketTrainDataBase;
import net.hulan.ivr.block.StorableBlockEntity;
import net.hulan.ivr.item.ItemNPCSpawnEgg;
import net.hulan.ivr.entity.NPC;
import net.hulan.ksd.data.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Score;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class KSDPacketServer extends PacketTrainDataBase implements KSDPacket {

    public static final String NPC_ROLE_TAG = "ivr_role";

    public static String getNPCRole(NPC npc) {
        CompoundTag tag = new CompoundTag();
        npc.saveWithoutId(tag);
        if (tag.contains(NPC_ROLE_TAG)) {
            return tag.getString(NPC_ROLE_TAG);
        }
        return npc.getRole();
    }

    public static void receiveNPCInteraction(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet) {
        final int entityId = packet.readVarInt();
        server.execute(() -> {
            if (!(player.level.getEntity(entityId) instanceof NPC npc)
                    || npc.distanceToSqr(player) > 36.0D) {
                return;
            }
            final String role = getNPCRole(npc);
            if (ItemNPCSpawnEgg.TICKETS_ROLE.equals(role)) {
                openTicketsScreenS2C(player, npc.blockPosition());
            } else if (ItemNPCSpawnEgg.FA_ROLE.equals(role)) {
                openFareAdjustmentsScreenS2C(player, npc.blockPosition());
            }
        });
    }
    
    private static final int PACKET_CHUNK_SIZE = (int) Math.pow(2.0F, 14.0F);

    public static void openKSDDashboardScreenS2C(ServerPlayer player, TransportMode transportMode, boolean useTimeAndWindSync) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeUtf(transportMode.toString());
        packet.writeBoolean(useTimeAndWindSync);
        Registry.sendToPlayer(player, KSD_PACKET_OPEN_KSD_DASHBOARD_SCREEN, packet);
    }

    public static void openTicketsScreenS2C(ServerPlayer player, BlockPos pos) {
        int balance = TicketSystem.getPlayerScore(player.getLevel(), player, "mtr_balance").getScore();
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(pos);
        packet.writeInt(balance);
        Registry.sendToPlayer(player, KSD_PACKET_OPEN_TICKETS_SCREEN, packet);
    }

    public static void openSTMachineScreenS2C(ServerPlayer player, SingleTicketSystem.TicketType ticketType, BlockPos pos) {
        int balance = TicketSystem.getPlayerScore(player.getLevel(), player, "mtr_balance").getScore();
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeUtf(ticketType.name());
        packet.writeBlockPos(pos);
        packet.writeInt(balance);
        Registry.sendToPlayer(player, KSD_PACKET_OPEN_KCR_ST_MACHINE_SCREEN, packet);
    }

    public static void openFareAdjustmentsScreenS2C(ServerPlayer player, BlockPos pos) {
        int balance = TicketSystem.getPlayerScore(player.getLevel(), player, "mtr_balance").getScore();
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(pos);
        packet.writeInt(balance);
        Registry.sendToPlayer(player, KSD_PACKET_OPEN_FARE_ADJUSTMENT_SCREEN, packet);
    }

    public static void openPurchaseOctopusScreenS2C(ServerPlayer player) {
        int balance = TicketSystem.getPlayerScore(player.getLevel(), player, "mtr_balance").getScore();
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeInt(balance);
        Registry.sendToPlayer(player, KSD_PACKET_OPEN_APPLY_OCTOPUS_SCREEN, packet);
    }

    public static void openAddValueMachine(ServerPlayer player, BlockPos pos) {
        int balance = TicketSystem.getPlayerScore(player.getLevel(), player, "mtr_balance").getScore();
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeBlockPos(pos);
        packet.writeInt(balance);
        Registry.sendToPlayer(player, KSD_PACKET_OPEN_ADD_VALUE_SCREEN, packet);
    }

    public static void sendAllInChunks(ServerPlayer player, Set<KSDStation> stations, Set<KSDPlatform> platforms, Set<KSDRoute> routes) {
        final long tempPacketId = new Random().nextLong();
        final FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        serializeData(packet, stations);
        serializeData(packet, platforms);
        serializeData(packet, routes);
        int i = 0;
        while (!sendChunk(player, packet, tempPacketId, i)) {
            i++;
        }
    }

    private static boolean sendChunk(ServerPlayer player, FriendlyByteBuf packet, long tempPacketId, int chunk) {
        FriendlyByteBuf packetChunk = new FriendlyByteBuf(Unpooled.buffer());
        packetChunk.writeLong(tempPacketId);
        packetChunk.writeInt(chunk);
        boolean success = chunk * PACKET_CHUNK_SIZE > packet.readableBytes();
        packetChunk.writeBoolean(success);
        if (!success) {
            packetChunk.writeBytes(packet.copy(chunk * PACKET_CHUNK_SIZE, Math.min(PACKET_CHUNK_SIZE, packet.readableBytes() - chunk * PACKET_CHUNK_SIZE)));
        }
        try {
            Registry.sendToPlayer(player, KSD_PACKET_CHUNK_S2C, packetChunk);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public static <T extends NameColorDataBase> void receiveUpdateOrDeleteC2S(MinecraftServer minecraftServer,
                                                                              ServerPlayer player,
                                                                              FriendlyByteBuf packet,
                                                                              ResourceLocation packetId,
                                                                              Function<KSDRailwayData, Set<T>> dataSet,
                                                                              Function<KSDRailwayData, Map<Long, T>> cacheMap,
                                                                              BiFunction<Long, TransportMode, T> createDataWithId,
                                                                              boolean isDelete) {
        if (!KSDRailwayData.hasNoPermission(player)) {
            Level world = player.level;
            KSDRailwayData railwayData = KSDRailwayData.getInstance(world);
            if (railwayData != null) {
                PacketCallback packetCallback = (updatePacket, fullPacket) -> {
                    world.players().forEach(worldPlayer -> {
                        if (!worldPlayer.getUUID().equals(player.getUUID())) {
                            Registry.sendToPlayer((ServerPlayer) worldPlayer, packetId, fullPacket);
                        }
                    });
                    railwayData.dataCache.sync();
                    railwayData.setDirty();
                };
                if (isDelete) {
                    deleteData(
                            dataSet.apply(railwayData),
                            cacheMap.apply(railwayData),
                            minecraftServer,
                            packet,
                            packetCallback,
                            (data) -> railwayData.railwayDataLoggingModule.addEvent(player, data.getClass(), data.id, data.name, KSDRailwayDataLoggingModule.getData(data), new ArrayList<>()));
                } else {
                    updateData(
                            dataSet.apply(railwayData),
                            cacheMap.apply(railwayData),
                            minecraftServer,
                            packet,
                            packetCallback,
                            createDataWithId,
                        (data, oldData) -> railwayData.railwayDataLoggingModule.addEvent(player, data.getClass(), data.id, data.name, oldData, KSDRailwayDataLoggingModule.getData(data)));
                }
            }
        }
    }

    public static void receivePurchaseSTC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        int fare = packet.readInt();
        int amount = packet.readInt();
        SingleTicketSystem.TicketType ticketType = EnumHelper.valueOf(SingleTicketSystem.TicketType.MTR, packet.readUtf());
        boolean isConcessionary = packet.readBoolean();
        boolean fcAvailable = packet.readBoolean();
        BlockPos storeBlockPos = packet.readBlockPos();
        minecraftServer.execute(() -> {
            Level world = player.level;
            List<ItemStack> items = new ArrayList<>(amount);
            for (int i = 1; i <= amount; i++) {
                items.add(SingleTicketSystem.purchaseSingleTicketItem(fare, ticketType, isConcessionary, fcAvailable));
            }
            storeOrReleaseItems(world, storeBlockPos, Utilities.getInventory(player), items);
        });
    }

    public static void receiveAdjustSTFareC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        long id = packet.readLong();
        int addValue = packet.readInt();
        minecraftServer.execute(() -> {
            Inventory inventory = Utilities.getInventory(player);
            ItemStack singleTicketItem = SingleTicketSystem.findSingleTicketItem(id, inventory);
            if (!singleTicketItem.isEmpty()) {
                inventory.removeItem(singleTicketItem);
                SingleTicketSystem.adjustSingleTicketFare(singleTicketItem, addValue);
                inventory.add(singleTicketItem);
            }
        });
    }

    public static void receiveAdjustOctopusFareC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        UUID uuid = packet.readUUID();
        minecraftServer.execute(() -> {
            Inventory inventory = Utilities.getInventory(player);
            ItemStack octopusItem = OctopusSystem.findOctopusItem(uuid, inventory);
            if (!octopusItem.isEmpty()) {
                KCRTicketSystem.resetEntryTime(octopusItem.getOrCreateTag());
            }
        });
    }

    public static void receiveAdjustFCFareC2S(MinecraftServer minecraftServer, ServerPlayer player) {
        minecraftServer.execute(() -> FirstClassValidationSystem.setAdjusted(player.level, player));
    }

    public static void receivePayment(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        PaymentMethod paymentMethod = EnumHelper.valueOf(PaymentMethod.MTR_BALANCE, packet.readUtf());
        int needToPay = packet.readInt();
        int actualPayment = packet.readInt();
        BlockPos storeBlockPos = packet.readBlockPos();
        minecraftServer.execute(() -> {
            Level world = player.level;
            Inventory inventory = Utilities.getInventory(player);
            switch (paymentMethod) {
                case EMERALDS -> {
                    List<ItemStack> items = new ArrayList<>();
                    int change = actualPayment - needToPay;
                    ContainerHelper.clearOrCountMatchingItems(inventory, (itemStack) -> itemStack.getItem() == Items.EMERALD, actualPayment, false);
                    if (change > 0) {
                        ItemStack emeraldsStack = new ItemStack(Items.EMERALD, change);
                        items.add(emeraldsStack);
                    }
                    storeOrReleaseItems(world, storeBlockPos, inventory, items);
                    world.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                case MTR_BALANCE -> {
                    TicketSystem.addObjectivesIfMissing(world);
                    Score balanceScore = TicketSystem.getPlayerScore(world, player, "mtr_balance");
                    balanceScore.setScore(balanceScore.getScore() - actualPayment);
                }
            }
        });
    }

    public static void receivePurchaseOctopusC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        int addValue = packet.readInt();
        boolean isConcessionary = packet.readBoolean();
        int amount = packet.readInt();
        minecraftServer.execute(() -> {
            Level world = player.level;
            KSDRailwayData railwayData = KSDRailwayData.getInstance(world);
            Inventory inventory = Utilities.getInventory(player);
            if (railwayData != null) {
                for (int i = 1; i <= amount; i++) {
                    inventory.add(OctopusSystem.createOctopusItem(addValue, isConcessionary, railwayData.jsonDataManager));
                }
            }
        });
    }

    public static void receiveOctopusAddValueC2S(MinecraftServer minecraftServer, ServerPlayer player, FriendlyByteBuf packet) {
        UUID uuid = packet.readUUID();
        int addValue = packet.readInt();
        Octopus.History.Source source = EnumHelper.valueOf(Octopus.History.Source.NONE, packet.readUtf());
        minecraftServer.execute(() -> {
            Level world = player.level;
            KSDRailwayData railwayData = KSDRailwayData.getInstance(world);
            if (railwayData != null) {
                OctopusSystem.addValue(uuid, addValue, source, railwayData.jsonDataManager, Utilities.getInventory(player));
            }
        });
    }

    private static <T extends SerializedDataBase> void serializeData(FriendlyByteBuf packet, Collection<T> objects) {
        packet.writeInt(objects.size());
        objects.forEach((object) -> object.writePacket(packet));
    }

    private static void storeOrReleaseItems(Level world, BlockPos storeBlockPos, Inventory inventory, List<ItemStack> items) {
        if (world.getBlockEntity(storeBlockPos) instanceof StorableBlockEntity entity) {
            entity.storeItems(items);
        } else {
            items.forEach(inventory::add);
        }
    }
}
