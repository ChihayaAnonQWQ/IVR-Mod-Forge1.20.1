package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.SingleTicketSystem;
import net.hulan.ksd.data.KSDRoute;
import net.hulan.ksd.data.KSDStation;
import net.hulan.ksd.utils.RailDataUtilities;
import net.hulan.ksd.utils.RenderUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class KCRSTMachineScreen extends ScreenMapper implements KSDGui {

    public final SingleTicketSystem.TicketType ticketType;
    private final KSDStation current;
    public final BlockPos machinePos;
    private final int balance;
    final Set<KSDRoute> mainRoutes = new HashSet<>();
    final Set<KSDRoute> otherRoutes = new HashSet<>();
    final Set<KSDRoute> lightRailRoutes = new HashSet<>();
    final Set<KSDStation> mainStations = new HashSet<>();
    final Set<KSDStation> otherStations = new HashSet<>();
    final Set<KSDStation> lightRailStations = new HashSet<>();
    private final KCRSTMachineRailMap railMap;
    private final KCRSTMMachineLegend legend;
    private static final String RMH_CHI = "路線圖";
    private static final String RMH_ENG = "System Map";
    private static final float RMH_CHI_WIDTH;
    private static final float RMH_ENG_WIDTH;

    public KCRSTMachineScreen(SingleTicketSystem.TicketType ticketType, @NotNull KSDStation current, BlockPos machinePos, int balance) {
        super(Text.literal(""));
        this.ticketType = ticketType;
        this.current = current;
        this.machinePos = machinePos;
        this.balance = balance;
        railMap = new KCRSTMachineRailMap(this::onClickedOnDestination, ticketType, current);
        legend = new KCRSTMMachineLegend(this);
    }

    protected void init() {
        loadRoutes();
        loadStations();
        int componentHeight = height - RMH_HEADER_HEIGHT;
        legend.setPositionAndSize(0, RMH_HEADER_HEIGHT, LEGEND_WIDTH, componentHeight);
        railMap.setPositionAndSize(LEGEND_WIDTH, RMH_HEADER_HEIGHT, width - LEGEND_WIDTH, componentHeight);
        addWidget(legend);
        addWidget(railMap);
        legend.load(componentHeight);
        railMap.load();
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderMapComponent(guiGraphics, mouseX, mouseY, delta);
        renderHeader(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    public boolean isPauseScreen() {
        return false;
    }

    public void onClose() {
        railMap.updateCursor(false);
        super.onClose();
    }

    public void onClickedOnDestination(KSDStation destination) {
        if (minecraft != null) {
            UtilitiesClient.setScreen(minecraft, new STProcessingScreen(current, destination, balance, this));
        }
    }

    private void loadRoutes() {
        mainRoutes.clear();
        otherRoutes.clear();
        lightRailRoutes.clear();
        Set<KSDRoute> routesInNetwork = KSDClientData.DATA_CACHE.wayFinder.getNetwork(current.id);
        Set<KSDRoute> mtr = RailDataUtilities.getMTRRoutes(routesInNetwork);
        Set<KSDRoute> kcr = RailDataUtilities.getKCRRoutes(routesInNetwork);
        Set<KSDRoute> lightRails = RailDataUtilities.getLightRailRoutes(routesInNetwork);
        switch (ticketType) {
            case MTR -> {
                mainRoutes.addAll(mtr);
                otherRoutes.addAll(kcr);
                lightRailRoutes.addAll(lightRails);
            }
            case KCR -> {
                mainRoutes.addAll(kcr);
                otherRoutes.addAll(mtr);
                lightRailRoutes.addAll(lightRails);
            }
            case LRT -> {
                mainRoutes.addAll(lightRails);
                otherRoutes.addAll(kcr);
                otherRoutes.addAll(mtr);
            }
        }
    }

    private void loadStations() {
        mainStations.clear();
        otherStations.clear();
        lightRailStations.clear();
        for (KSDRoute route : mainRoutes) {
            mainStations.addAll(KSDClientData.DATA_CACHE.routeIdToStationsWithIndex.get(route.id));
        }
        for (KSDRoute route : otherRoutes) {
            otherStations.addAll(KSDClientData.DATA_CACHE.routeIdToStationsWithIndex.get(route.id));
        }
        for (KSDRoute route : lightRailRoutes) {
            lightRailStations.addAll(KSDClientData.DATA_CACHE.routeIdToStationsWithIndex.get(route.id));
        }
    }

    private void renderHeader(GuiGraphics guiGraphics) {
        PoseStack matrices = guiGraphics.pose();
        guiGraphics.fill(0, 0, width, RMH_HEADER_HEIGHT, RGB_HEADER_BLUE | ARGB_BLACK);
        // Flush the batched header fill before the raw texture/text drawing below.
        guiGraphics.flush();
        RenderUtilities renderUtilities = RenderUtilities.getInstance();
        renderUtilities.drawTexture(matrices, new ResourceLocation("ivr", "textures/block/sign/rmh.png"), 0, 0, RMH_HEADER_HEIGHT, RMH_HEADER_HEIGHT);
        renderUtilities.drawText(matrices, RMH_CHI, RMH_HEADER_HEIGHT + RMH_PADDING, 10, RMH_CHI_WIDTH, RMH_CHI_HEIGHT, ARGB_WHITE);
        renderUtilities.drawText(matrices, RMH_ENG, 170, 25, RMH_ENG_WIDTH, RMH_ENG_HEIGHT, ARGB_WHITE);
    }

    private void renderMapComponent(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.fill(0, RMH_HEADER_HEIGHT, width, height, ARGB_WHITE);
        // Flush the batched map background before the map/legend draw through raw vertex buffers.
        guiGraphics.flush();
        railMap.render(guiGraphics, mouseX, mouseY, delta);
        legend.render(guiGraphics, mouseX, mouseY, delta);
    }

    static {
        RMH_CHI_WIDTH = RenderUtilities.getInstance().getTextWidth(RMH_CHI, RMH_CHI_HEIGHT / Minecraft.getInstance().font.lineHeight);
        RMH_ENG_WIDTH = RenderUtilities.getInstance().getTextWidth(RMH_ENG, RMH_ENG_HEIGHT / Minecraft.getInstance().font.lineHeight);
    }
}
