package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.hulan.ksd.KSDItems;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.SingleTicketSystem;
import net.hulan.ksd.data.KSDRailwayData;
import net.hulan.ksd.data.KSDStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public class TicketsScreen extends ScreenMapper implements KSDGui {

    private final TextureButton buttonOpenMTRSTMScreen;
    private final TextureButton buttonOpenKCRSTMScreen;
    private final TextureButton buttonOpenLRTSTMScreen;
    private final TextureButton buttonOpenITTTMScreen;
    private final TextureButton buttonOpenPurchaseOctopusScreen;
    private final TextureButton buttonOpenAddValueScreen;
    private final int panelWidth;
    private final int panelHeight;
    private static final int MIN_PANEL_WIDTH = BUTTON_WIDTH * 2 + BUTTON_GAP + 8;
    private static final int MIN_PANEL_HEIGHT = BUTTON_HEIGHT * 3 + BUTTON_GAP * 2 + 40;

    public TicketsScreen(BlockPos storeBlockPos, int mtrBalance) {
        super(Text.literal("Tickets"));
        this.panelWidth = Math.max(MIN_PANEL_WIDTH, 440);
        this.panelHeight = Math.max(MIN_PANEL_HEIGHT, 290);
        buttonOpenMTRSTMScreen = new TextureButton(Text.translatable("gui.ksd.open_stm_screen_mtr"), button ->
                openSTMScreen(SingleTicketSystem.TicketType.MTR, storeBlockPos, mtrBalance));
        buttonOpenKCRSTMScreen = new TextureButton(Text.translatable("gui.ksd.open_stm_screen_kcr"), button ->
                openSTMScreen(SingleTicketSystem.TicketType.KCR, storeBlockPos, mtrBalance));
        buttonOpenLRTSTMScreen = new TextureButton(Text.translatable("gui.ksd.open_stm_screen_lrt"), button ->
                openSTMScreen(SingleTicketSystem.TicketType.LRT, storeBlockPos, mtrBalance));
        buttonOpenITTTMScreen = new TextureButton(Text.translatable("gui.ksd.open_stm_screen_itt"), button -> {
        });
        buttonOpenPurchaseOctopusScreen = new TextureButton(Text.translatable("gui.ksd.open_purchase_octopus_screen"), button ->
                openPurchaseOctopusScreen(mtrBalance, this));
        buttonOpenAddValueScreen = new TextureButton(Text.translatable("gui.ksd.open_add_value_screen"), button ->
                openAddValueScreen(mtrBalance, this, storeBlockPos));
    }

    protected void init() {
        super.init();
        final int panelLeft = width / 2 - panelWidth / 2;
        final int panelTop = height / 2 - panelHeight / 2;
        final int left = panelLeft + (panelWidth - BUTTON_WIDTH * 2 - BUTTON_GAP) / 2;
        final int top = panelTop + (panelHeight - BUTTON_HEIGHT * 3 - BUTTON_GAP * 2) / 2;
        IDrawing.setPositionAndWidth(buttonOpenMTRSTMScreen, left, top, BUTTON_WIDTH);
        buttonOpenMTRSTMScreen.setLayoutHeight(BUTTON_HEIGHT);
        IDrawing.setPositionAndWidth(buttonOpenKCRSTMScreen, left + BUTTON_WIDTH + BUTTON_GAP, top, BUTTON_WIDTH);
        buttonOpenKCRSTMScreen.setLayoutHeight(BUTTON_HEIGHT);
        IDrawing.setPositionAndWidth(buttonOpenLRTSTMScreen, left, top + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH);
        buttonOpenLRTSTMScreen.setLayoutHeight(BUTTON_HEIGHT);
        IDrawing.setPositionAndWidth(buttonOpenITTTMScreen, left + BUTTON_WIDTH + BUTTON_GAP, top + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH);
        buttonOpenITTTMScreen.setLayoutHeight(BUTTON_HEIGHT);
        IDrawing.setPositionAndWidth(buttonOpenPurchaseOctopusScreen, left, top + (BUTTON_HEIGHT + BUTTON_GAP) * 2, BUTTON_WIDTH);
        buttonOpenPurchaseOctopusScreen.setLayoutHeight(BUTTON_HEIGHT);
        IDrawing.setPositionAndWidth(buttonOpenAddValueScreen, left + BUTTON_WIDTH + BUTTON_GAP, top + (BUTTON_HEIGHT + BUTTON_GAP) * 2, BUTTON_WIDTH);
        buttonOpenAddValueScreen.setLayoutHeight(BUTTON_HEIGHT);
        addDrawableChild(buttonOpenMTRSTMScreen);
        addDrawableChild(buttonOpenKCRSTMScreen);
        addDrawableChild(buttonOpenLRTSTMScreen);
        addDrawableChild(buttonOpenITTTMScreen);
        addDrawableChild(buttonOpenPurchaseOctopusScreen);
        addDrawableChild(buttonOpenAddValueScreen);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        KSDGui.renderBg(guiGraphics, width, height, panelWidth, panelHeight);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, title, width / 2, height / 2 - panelHeight / 2 + 7, IGui.ARGB_WHITE);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    public boolean isPauseScreen() {
        return false;
    }

    public static void openSTMScreen(SingleTicketSystem.TicketType ticketType, BlockPos storeBlockPos, int mtrBalance) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            KSDStation current = KSDRailwayData.getStation(KSDClientData.STATIONS, storeBlockPos);
            if (!(minecraft.screen instanceof KCRSTMachineScreen) && current != null) {
                UtilitiesClient.setScreen(minecraft, new KCRSTMachineScreen(ticketType, current, storeBlockPos, mtrBalance));
            }
        }
    }

    public static void openITTTMScreen(int mtrBalance) {
        //TODO 直通车售票界面
    }

    public static void openPurchaseOctopusScreen(int mtrBalance, ScreenMapper parent) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player =  minecraft.player;
        if (player != null) {
            BlockPos storeBlockPos = player.blockPosition();
            if (!(minecraft.screen instanceof PurchaseOctopusScreen)) {
                UtilitiesClient.setScreen(minecraft, new PurchaseOctopusScreen(mtrBalance, parent, storeBlockPos));
            }
        }
    }

    public static void openAddValueScreen(int mtrBalance, ScreenMapper parent, BlockPos storeBlockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof KCRSTMachineScreen)) {
            UtilitiesClient.setScreen(minecraft, new PutItemScreen(
                    "item.ksd.octopus",
                    KSDItems.OCTOPUS.get(),
                    PutItemScreen.PutMethod.INSERT,
                    true,
                    (octopusItem, amount) -> {
                        if (!(minecraft.screen instanceof AddValueMachineScreen)) {
                            UtilitiesClient.setScreen(minecraft, new AddValueMachineScreen(
                                    mtrBalance,
                                    parent,
                                    octopusItem,
                                    storeBlockPos));
                        }
                    }));
        }
    }

}
