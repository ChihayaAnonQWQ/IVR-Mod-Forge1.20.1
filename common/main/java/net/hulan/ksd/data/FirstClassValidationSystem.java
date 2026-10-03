package net.hulan.ksd.data;

import mtr.SoundEvents;
import mtr.mappings.Text;
import net.hulan.ksd.item.ItemOctopus;
import net.hulan.ksd.item.ItemSingleTicket;
import net.hulan.ksd.utils.RailDataUtilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.Set;

public final class FirstClassValidationSystem {

    public static final int FC_EVASION_FINE = 1000;
    private static final String FIRST_CLASS_STATE = "fc_state";
    /** Called once when a tracked train passenger crosses into first class. */
    public static void validateEnteringFirstClass(Level world, KSDRailwayData railwayData, Player player) {
        ItemStack holdingItem = player.getMainHandItem();
        if (holdingItem.getItem() instanceof ItemSingleTicket || holdingItem.getItem() instanceof ItemOctopus) {
            validate(world, railwayData, player, holdingItem, holdingItem.getItem() instanceof ItemOctopus);
        } else {
            setIllegal(world, player);
        }
    }

    public static void setIllegal(Level world, Player player) {
        if (isValidated(world, player) || isAdjusted(world, player)) return;
        addObjectivesIfMissing(world);
        Score phpScore = getPlayerHasIllegalScore(world, player);
        phpScore.setScore(1);
    }

    public static boolean isIllegal(Level world, Player player) {
        addObjectivesIfMissing(world);
        Score phpScore = getPlayerHasIllegalScore(world, player);
        return phpScore.getScore() == 1;
    }

    public static void setValidated(Level world, Player player) {
        addObjectivesIfMissing(world);
        Score phpScore = getPlayerHasIllegalScore(world, player);
        phpScore.setScore(2);
    }

    public static boolean isValidated(Level world, Player player) {
        addObjectivesIfMissing(world);
        Score phpScore = getPlayerHasIllegalScore(world, player);
        return phpScore.getScore() == 2;
    }

    public static void setAdjusted(Level world, Player player) {
        addObjectivesIfMissing(world);
        Score phpScore = getPlayerHasIllegalScore(world, player);
        phpScore.setScore(3);
    }

    public static boolean isAdjusted(Level world, Player player) {
        addObjectivesIfMissing(world);
        Score phpScore = getPlayerHasIllegalScore(world, player);
        return phpScore.getScore() == 3;
    }

    public static void clearState(Level world, Player player) {
        addObjectivesIfMissing(world);
        Score phpScore = getPlayerHasIllegalScore(world, player);
        phpScore.setScore(0);
    }

    public static FirstClassState validateOnMachine(Level world,
                                                    KSDRailwayData railwayData,
                                                    Player player,
                                                    KSDStation currentStation,
                                                    ItemStack item,
                                                    boolean isOctopus) {
        Set<KSDRoute> routesInStation = railwayData.dataCache.stationIdToRoutes.get(currentStation.id);
        for (KSDRoute route : routesInStation) {
            if (RailDataUtilities.hasFirstClassService(route)) {
                return validate(world, railwayData, player, item, isOctopus);
            }
        }
        playSoundAndSendMessage(
                world,
                player.blockPosition(),
                player,
                "gui.ksd.fc_denied_no_matched_route");
        return FirstClassState.DENIED;
    }

    public static FirstClassState validate(Level world,
                                           KSDRailwayData railwayData,
                                           Player player,
                                           ItemStack item,
                                           boolean isOctopus) {
        CompoundTag itemTag = item.getOrCreateTag();
        if (!KCRTicketSystem.isEntered(itemTag, railwayData.stations, isOctopus)) {
            playSoundAndSendMessage(
                    world,
                    player.blockPosition(),
                    player,
                    "gui.ksd.fc_denied_no_entry");
            return FirstClassState.DENIED;
        }
        boolean isConcessionary = itemTag.getBoolean("is_concessionary");
        boolean fcAvailable = isOctopus || itemTag.getBoolean("fc_available");
        if (!fcAvailable) {
            playSoundAndSendMessage(
                    world,
                    player.blockPosition(),
                    player,
                    "gui.ksd.fc_denied_fc_unavailable");
            return FirstClassState.DENIED;
        }
        if (!isValidated(itemTag)) {
            validate(itemTag);
            if (!isValidated(world, player)) {
                setValidated(world, player);
            }
            playSoundAndSendMessage(
                    world,
                    player.blockPosition(),
                    player,
                    isConcessionary ? "gui.ksd.fc_validated_concessionary" : "gui.ksd.fc_validated");
            return isConcessionary ? FirstClassState.VALIDATED_CONCESSIONARY : FirstClassState.VALIDATED;
        } else {
            playSoundAndSendMessage(
                    world,
                    player.blockPosition(),
                    player,
                    "gui.ksd.fc_already_validated");
            return FirstClassState.DENIED;
        }
    }

    public static void validate(CompoundTag itemTag) {
        itemTag.putBoolean("fc_validated", true);
    }

    public static boolean isValidated(CompoundTag itemTag) {
        return itemTag.getBoolean("fc_validated");
    }

    public static void devalidate(CompoundTag itemTag) {
        itemTag.remove("fc_validated");
    }

    private static Score getPlayerHasIllegalScore(Level world, Player player) {
        return world.getScoreboard().getOrCreatePlayerScore(player.getGameProfile().getName(), player.getScoreboard().getObjective(FIRST_CLASS_STATE));
    }

    private static void addObjectivesIfMissing(Level world) {
        if (world.getScoreboard().getObjective(FIRST_CLASS_STATE) == null) {
            world.getScoreboard().addObjective(FIRST_CLASS_STATE, ObjectiveCriteria.DUMMY, Text.literal("IS ILLEGAL"), ObjectiveCriteria.RenderType.INTEGER);
        }
    }

    private static void playSoundAndSendMessage(Level world, BlockPos pos, Player player, String message) {
        world.playSound(null, pos, SoundEvents.TICKET_BARRIER, SoundSource.PLAYERS, 1, 1);
        player.displayClientMessage(Text.translatable(message), false);
    }

    public enum FirstClassState {

        MTR("mtr"),
        ILLEGALLY("illegally"),
        VALIDATED("validated"),
        VALIDATED_CONCESSIONARY("validated_concessionary"),
        DENIED("denied");

        private final String name;

        FirstClassState(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
