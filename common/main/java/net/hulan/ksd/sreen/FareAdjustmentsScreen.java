package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.hulan.ksd.KSDItems;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.KCRTicketSystem;
import net.hulan.ksd.data.KSDRailwayData;
import net.hulan.ksd.data.KSDStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class FareAdjustmentsScreen extends ScreenMapper implements KSDGui {

    private static final int PANEL_WIDTH = 440;
    private static final int PANEL_HEIGHT = BUTTON_HEIGHT * 2 + BUTTON_GAP + 40;
    private final TextureButton buttonOpenSTFareAdjustmentScreen;
    private final TextureButton buttonOpenOctopusFareAdjustmentScreen;
    private final TextureButton buttonOpenFCFareAdjustmentScreen;

    public FareAdjustmentsScreen(int mtrBalance, BlockPos storeBlockPos) {
        super(Text.literal(""));
        buttonOpenSTFareAdjustmentScreen = new TextureButton(Text.translatable("gui.ksd.st_fare_adjustment"), button ->
                openSTFAScreen(mtrBalance, this, storeBlockPos));
        buttonOpenOctopusFareAdjustmentScreen = new TextureButton(Text.translatable("gui.ksd.octopus_fare_adjustment"), button ->
                openOctopusFareAdjustmentScreen(mtrBalance, this, storeBlockPos));
        buttonOpenFCFareAdjustmentScreen = new TextureButton(Text.translatable("gui.ksd.fc_fare_adjustment"), button ->
                openFCFareAdjustmentScreen(mtrBalance, this, storeBlockPos));
    }

    protected void init() {
        super.init();
        final int panelLeft = width / 2 - PANEL_WIDTH / 2;
        final int panelTop = height / 2 - PANEL_HEIGHT / 2;
        final int left = panelLeft + (PANEL_WIDTH - BUTTON_WIDTH * 2 - BUTTON_GAP) / 2;
        final int top = panelTop + (PANEL_HEIGHT - BUTTON_HEIGHT * 2 - BUTTON_GAP) / 2;
        IDrawing.setPositionAndWidth(buttonOpenSTFareAdjustmentScreen, left, top, BUTTON_WIDTH);
        buttonOpenSTFareAdjustmentScreen.setLayoutHeight(BUTTON_HEIGHT);
        IDrawing.setPositionAndWidth(buttonOpenOctopusFareAdjustmentScreen, left + BUTTON_WIDTH + BUTTON_GAP, top, BUTTON_WIDTH);
        buttonOpenOctopusFareAdjustmentScreen.setLayoutHeight(BUTTON_HEIGHT);
        IDrawing.setPositionAndWidth(buttonOpenFCFareAdjustmentScreen, left, top + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH);
        buttonOpenFCFareAdjustmentScreen.setLayoutHeight(BUTTON_HEIGHT);
        addDrawableChild(buttonOpenSTFareAdjustmentScreen);
        addDrawableChild(buttonOpenOctopusFareAdjustmentScreen);
        addDrawableChild(buttonOpenFCFareAdjustmentScreen);
    }

    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        KSDGui.renderBg(guiGraphics, width, height, PANEL_WIDTH, PANEL_HEIGHT);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, title, width / 2, height / 2 - PANEL_HEIGHT / 2 + 7, IGui.ARGB_WHITE);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    public boolean isPauseScreen() {
        return false;
    }

    public static void openSTFAScreen(int mtrBalance, ScreenMapper parent, BlockPos storeBlockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            if (!(minecraft.screen instanceof STFareAdjustmentScreen)) {
                UtilitiesClient.setScreen(minecraft, new PutItemScreen(
                        "item.ksd.single_ticket",
                        KSDItems.SINGLE_TICKET.get(),
                        PutItemScreen.PutMethod.PUT,
                        true,
                        (stItem, amount) -> {
                            CompoundTag stTag = stItem.getOrCreateTag();
                            KSDStation current = KCRTicketSystem.getEnteredStation(stTag, KSDClientData.STATIONS);
                            KSDStation destination = KSDRailwayData.getStation(KSDClientData.STATIONS, storeBlockPos);
                            if (!(minecraft.screen instanceof STFareAdjustmentScreen) &&
                                    current != null &&
                                    destination != null) {
                                UtilitiesClient.setScreen(minecraft, new STFareAdjustmentScreen(
                                        current,
                                        destination,
                                        mtrBalance,
                                        parent,
                                        stItem,
                                        storeBlockPos));
                            }
                        }));
            }
        }
    }

    public static void openOctopusFareAdjustmentScreen(int mtrBalance, ScreenMapper parent, BlockPos storeBlockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            if (!(minecraft.screen instanceof OctopusFareAdjustmentScreen)) {
                UtilitiesClient.setScreen(minecraft, new PutItemScreen(
                        "item.ksd.octopus",
                        KSDItems.OCTOPUS.get(),
                        PutItemScreen.PutMethod.PUT,
                        true,
                        (stItem, amount) -> {
                            CompoundTag stTag = stItem.getOrCreateTag();
                            KSDStation current = KCRTicketSystem.getEnteredStation(stTag, KSDClientData.STATIONS);
                            KSDStation destination = KSDRailwayData.getStation(KSDClientData.STATIONS, storeBlockPos);
                            if (!(minecraft.screen instanceof OctopusFareAdjustmentScreen) &&
                                    current != null &&
                                    destination != null) {
                                UtilitiesClient.setScreen(minecraft, new OctopusFareAdjustmentScreen(
                                        current,
                                        destination,
                                        mtrBalance,
                                        parent,
                                        stItem,
                                        storeBlockPos));
                            }
                        }));
            }
        }
    }

    public static void openFCFareAdjustmentScreen(int mtrBalance, ScreenMapper parent, BlockPos storeBlockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            if (!(minecraft.screen instanceof OctopusFareAdjustmentScreen)) {
                UtilitiesClient.setScreen(minecraft, new FCFareAdjustmentScreen(
                        mtrBalance,
                        parent,
                        storeBlockPos));
            }
        }
    }
}
