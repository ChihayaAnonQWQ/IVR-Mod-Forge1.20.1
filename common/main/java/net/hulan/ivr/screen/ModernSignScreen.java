package net.hulan.ivr.screen;

import mtr.block.BlockRailwaySign;
import mtr.client.CustomResources;
import mtr.client.IDrawing;
import mtr.data.DataConverter;
import mtr.data.IGui;
import mtr.data.NameColorDataBase;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderRailwaySign;
import mtr.screen.DashboardListSelectorScreen;
import net.hulan.ivr.block.BlockModernSign;
import net.hulan.ivr.block.BlockKCRRouteSignBase;
import net.hulan.ivr.packet.IVRPacketTrainDataGuiClient;
import net.hulan.ivr.render.RenderModernSign;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.KSDPlatform;
import net.hulan.ksd.data.KSDRailwayData;
import net.hulan.ksd.data.KSDStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;
import java.util.stream.Collectors;

public class ModernSignScreen extends ScreenMapper implements IGui {

    private int editingIndex;
    private int page;
    private int totalPages;
    private int columns;
    private int rows;
    private final BlockPos signPos;
    private final boolean isModernSign;
    private final int length;
    private boolean luminance;
    private final String[] signIds;
    private final Set<Long> selectedIds;
    private final List<NameColorDataBase> exitsForList = new ArrayList<>();
    private final List<NameColorDataBase> platformsForList = new ArrayList<>();
    private final List<NameColorDataBase> routesForList = new ArrayList<>();
    private final List<NameColorDataBase> stationsForList = new ArrayList<>();
    private final List<String> allSignIds = new ArrayList<>();
    private final Button[] buttonsEdit;
    private final Button[] buttonsSelection;
    private final Button buttonClear;
    private final Button buttonLight;
    private final ImageButton buttonPrevPage;
    private final ImageButton buttonNextPage;
    private static final int SIGN_SIZE = 32;
    private static final int SIGN_BUTTON_SIZE = 16;
    private static final int BUTTON_Y_START = SIGN_SIZE + SQUARE_SIZE + SQUARE_SIZE / 2;

    public ModernSignScreen(BlockPos signPos) {
        super(Text.literal(""));
        editingIndex = -1;
        this.signPos = signPos;
        final ClientLevel world = Minecraft.getInstance().level;
        for (final BlockRailwaySign.SignType signType : BlockRailwaySign.SignType.values()) {
            allSignIds.add(signType.toString());
        }
        final List<String> sortedKeys = new ArrayList<>(CustomResources.CUSTOM_SIGNS.keySet());
        Collections.sort(sortedKeys);
        allSignIds.addAll(sortedKeys);
        try {
            final KSDStation station = KSDRailwayData.getStation(KSDClientData.STATIONS, signPos);
            if (station != null) {
                final Map<String, List<String>> exits = station.getGeneratedExits();
                final List<String> exitParents = new ArrayList<>(exits.keySet());
                exitParents.sort(String::compareTo);
                exitParents.forEach(exitParent -> {
                    final List<String> destinations = exits.get(exitParent);
                    exitsForList.add(new DataConverter(KSDStation.serializeExit(exitParent), exitParent + " " + (!destinations.isEmpty() ? destinations.get(0) : ""), 0));
                });
                final List<KSDPlatform> platforms = new ArrayList<>(KSDClientData.DATA_CACHE.requestStationIdToPlatforms(station.id).values());
                Collections.sort(platforms);
                platforms.stream().map(platform -> new DataConverter(platform.id, platform.name + " " + IGui.mergeStations(KSDClientData.DATA_CACHE.requestPlatformIdToRoutes(platform.id).stream().map(route -> route.stationDetails.get(route.stationDetails.size() - 1).stationName).collect(Collectors.toList())), 0)).forEach(platformsForList::add);
                KSDClientData.DATA_CACHE.getAllRoutesIncludingConnectingStations(station).forEach((color, route) -> routesForList.add(new DataConverter(route.color, route.name, route.color)));
                KSDClientData.DATA_CACHE.getConnectingStationsIncludingThisOne(station).forEach(connectingStation -> stationsForList.add(new DataConverter(connectingStation.id, connectingStation.name, connectingStation.color)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (world != null) {
            final BlockEntity entity = world.getBlockEntity(signPos);
            if (entity instanceof BlockModernSign.TileEntityModernSign signEntity) {
                signIds = signEntity.getSignIds();
                selectedIds = signEntity.getSelectedIds();
                isModernSign = true;
                luminance = signEntity.luminance();
            } else {
                signIds = new String[0];
                selectedIds = new HashSet<>();
                isModernSign = false;
                if (entity instanceof BlockKCRRouteSignBase.TileEntityKCRRouteSignBase) {
                    selectedIds.add(((BlockKCRRouteSignBase.TileEntityKCRRouteSignBase) entity).getPlatformId());
                }
            }
            if (world.getBlockState(signPos).getBlock() instanceof BlockModernSign) {
                length = ((BlockModernSign) world.getBlockState(signPos).getBlock()).length;
            } else {
                length = 0;
            }
        } else {
            length = 0;
            signIds = new String[0];
            selectedIds = new HashSet<>();
            isModernSign = false;
            luminance = false;
        }
        buttonsEdit = new Button[length];
        for (int i = 0; i < buttonsEdit.length; i++) {
            final int index = i;
            buttonsEdit[i] = UtilitiesClient.newButton(Text.translatable("selectWorld.edit"), button -> edit(index));
        }
        buttonsSelection = new Button[allSignIds.size()];
        for (int i = 0; i < allSignIds.size(); i++) {
            final int index = i;
            buttonsSelection[i] = UtilitiesClient.newButton(SIGN_BUTTON_SIZE, Text.literal(""), button -> setNewSignId(allSignIds.get(index)));
        }
        buttonClear = UtilitiesClient.newButton(Text.translatable("gui.mtr.reset_sign"), button -> setNewSignId(null));
        buttonLight = UtilitiesClient.newButton(Text.translatable("gui.ivr.cycle_light").append(Text.translatable(luminance ? "options.mtr.on" : "options.mtr.off")), button -> setLuminance(!luminance));
        buttonPrevPage = new ImageButton(0, 0, 0, SQUARE_SIZE, 0, 0, 20, new ResourceLocation("mtr:textures/gui/icon_left.png"), 20, 40, button -> setPage(page - 1));
        buttonNextPage = new ImageButton(0, 0, 0, SQUARE_SIZE, 0, 0, 20, new ResourceLocation("mtr:textures/gui/icon_right.png"), 20, 40, button -> setPage(page + 1));
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < buttonsEdit.length; i++) {
            IDrawing.setPositionAndWidth(buttonsEdit[i], (width - SIGN_SIZE * length) / 2 + i * SIGN_SIZE, SIGN_SIZE, SIGN_SIZE);
            addDrawableChild(buttonsEdit[i]);
        }
        columns = Math.max((width - SIGN_BUTTON_SIZE * 3) / (SIGN_BUTTON_SIZE * 8) * 2, 1);
        rows = Math.max((height - SIGN_SIZE - SQUARE_SIZE * 4) / SIGN_BUTTON_SIZE, 1);
        final int xOffsetSmall = (width - SIGN_BUTTON_SIZE * (columns * 4 + 3)) / 2 + SIGN_BUTTON_SIZE;
        final int xOffsetBig = xOffsetSmall + SIGN_BUTTON_SIZE * (columns + 1);
        totalPages = loopSigns((index, x, y, isBig) -> {
            IDrawing.setPositionAndWidth(buttonsSelection[index], (isBig ? xOffsetBig : xOffsetSmall) + x, BUTTON_Y_START + y, isBig ? SIGN_BUTTON_SIZE * 3 : SIGN_BUTTON_SIZE);
            buttonsSelection[index].visible = false;
            addDrawableChild(buttonsSelection[index]);
        }, true);
        final int buttonClearX = (width - PANEL_WIDTH - SQUARE_SIZE * 4) / 2;
        final int buttonY = height - SQUARE_SIZE * 2;
        IDrawing.setPositionAndWidth(buttonClear, buttonClearX, buttonY, PANEL_WIDTH);
        buttonClear.visible = false;
        addDrawableChild(buttonClear);
        IDrawing.setPositionAndWidth(buttonLight, buttonClearX - 144, buttonY, PANEL_WIDTH);
        buttonLight.visible = true;
        addDrawableChild(buttonLight);
        IDrawing.setPositionAndWidth(buttonPrevPage, buttonClearX + PANEL_WIDTH, buttonY, SQUARE_SIZE);
        buttonPrevPage.visible = false;
        addDrawableChild(buttonPrevPage);
        IDrawing.setPositionAndWidth(buttonNextPage, buttonClearX + PANEL_WIDTH + SQUARE_SIZE * 3, buttonY, SQUARE_SIZE);
        buttonNextPage.visible = false;
        addDrawableChild(buttonNextPage);
        if (!isModernSign && minecraft != null) {
            UtilitiesClient.setScreen(minecraft, new DashboardListSelectorScreen(this::onClose, platformsForList, selectedIds, true, false));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        try {
            renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, delta);
            if (minecraft == null) {
                return;
            }
            for (int i = 0; i < signIds.length; i++) {
                if (signIds[i] != null) {
                    RenderModernSign.drawSign(guiGraphics.pose(), null, null, font, signPos, signIds[i], (width - SIGN_SIZE * length) / 2F + i * SIGN_SIZE, 0, SIGN_SIZE, RenderModernSign.getMaxWidth(signIds, i, false), RenderModernSign.getMaxWidth(signIds, i, true), selectedIds, Direction.UP, 0, (textureId, x, y, size, flipTexture) -> {
                        guiGraphics.blit(textureId, (int) x, (int) y, 0, 0, (int) size, (int) size, (int) (flipTexture ? -size : size), (int) size);
                    });
                }
            }
            if (editingIndex >= 0) {
                final int xOffsetSmall = (width - SIGN_BUTTON_SIZE * (columns * 4 + 3)) / 2 + SIGN_BUTTON_SIZE;
                final int xOffsetBig = xOffsetSmall + SIGN_BUTTON_SIZE * (columns + 1);
                loopSigns((index, x, y, isBig) -> {
                    final String signId = allSignIds.get(index);
                    final CustomResources.CustomSign sign = RenderRailwaySign.getSign(signId); // 使用 MTR 获取指示牌预览定义。
                    if (sign != null) {
                        final boolean moveRight = sign.hasCustomText() && sign.flipCustomText;
                        UtilitiesClient.beginDrawingTexture(sign.textureId);
                        RenderModernSign.drawSign(guiGraphics.pose(), null, null, font, signPos, signId, (isBig ? xOffsetBig : xOffsetSmall) + x + (moveRight ? SIGN_BUTTON_SIZE * 2 : 0), BUTTON_Y_START + y, SIGN_BUTTON_SIZE, 2, 2, selectedIds, Direction.UP, 0, (textureId, x1, y1, size, flipTexture) -> guiGraphics.blit(textureId, (int) x1, (int) y1, 0, 0, (int) size, (int) size, (int) (flipTexture ? -size : size), (int) size));
                    }
                }, false);
                guiGraphics.drawCenteredString(font, String.format("%s/%s", page + 1, totalPages), (width - PANEL_WIDTH - SQUARE_SIZE * 4) / 2 + PANEL_WIDTH + SQUARE_SIZE * 2, height - SQUARE_SIZE * 2 + TEXT_PADDING, ARGB_WHITE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        setPage(page + (int) Math.signum(-amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void onClose() {
        IVRPacketTrainDataGuiClient.sendModernSignIdsC2S(signPos, selectedIds, signIds, luminance);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void resize(Minecraft client, int width, int height) {
        super.resize(client, width, height);
        for (Button button : buttonsEdit) {
            button.active = true;
        }
        for (Button button : buttonsSelection) {
            button.visible = false;
        }
        editingIndex = -1;
    }

    private int loopSigns(LoopSignsCallback loopSignsCallback, boolean ignorePage) {
        int pageCount = rows * columns;
        int indexSmall = 0;
        int indexBig = 0;
        int columnSmall = 0;
        int columnBig = 0;
        int rowSmall = 0;
        int rowBig = 0;
        int totalPagesSmallCount = 1;
        int totalPagesBigCount = 1;
        for (int i = 0; i < allSignIds.size(); i++) {
            final CustomResources.CustomSign sign = RenderRailwaySign.getSign(allSignIds.get(i)); // 使用 MTR 获取指示牌分页定义。
            final boolean isBig = sign != null && sign.hasCustomText();
            final boolean onPage = (isBig ? indexBig : indexSmall) / pageCount == page;
            buttonsSelection[i].visible = onPage;
            if (ignorePage || onPage) {
                loopSignsCallback.loopSignsCallback(i, (isBig ? columnBig * 3 : columnSmall) * SIGN_BUTTON_SIZE, (isBig ? rowBig : rowSmall) * SIGN_BUTTON_SIZE, isBig);
            }
            if (isBig) {
                columnBig++;
                if (totalPagesBigCount < 0) {
                    totalPagesBigCount = -totalPagesBigCount + 1;
                }
                if (columnBig >= columns) {
                    columnBig = 0;
                    rowBig++;
                    if (rowBig >= rows) {
                        rowBig = 0;
                        totalPagesBigCount = -totalPagesBigCount;
                    }
                }
                indexBig++;
            } else {
                columnSmall++;
                if (totalPagesSmallCount < 0) {
                    totalPagesSmallCount = -totalPagesSmallCount + 1;
                }
                if (columnSmall >= columns) {
                    columnSmall = 0;
                    rowSmall++;
                    if (rowSmall >= rows) {
                        rowSmall = 0;
                        totalPagesSmallCount = -totalPagesSmallCount;
                    }
                }
                indexSmall++;
            }
        }
        return Math.max(Math.abs(totalPagesBigCount), Math.abs(totalPagesSmallCount));
    }

    private void edit(int editingIndex) {
        this.editingIndex = editingIndex;
        for (Button button : buttonsEdit) {
            button.active = true;
        }
        buttonClear.visible = true;
        setPage(page);
        buttonsEdit[editingIndex].active = false;
    }

    private void setNewSignId(String newSignId) {
        if (editingIndex >= 0 && editingIndex < signIds.length) {
            signIds[editingIndex] = newSignId;
            final boolean isExitLetter = newSignId != null && (newSignId.equals(BlockRailwaySign.SignType.EXIT_LETTER.toString()) || newSignId.equals(BlockRailwaySign.SignType.EXIT_LETTER_FLIPPED.toString()));
            final boolean isPlatform = newSignId != null && (newSignId.equals(BlockRailwaySign.SignType.PLATFORM.toString()) || newSignId.equals(BlockRailwaySign.SignType.PLATFORM_FLIPPED.toString()));
            final boolean isLine = newSignId != null && (newSignId.equals(BlockRailwaySign.SignType.LINE.toString()) || newSignId.equals(BlockRailwaySign.SignType.LINE_FLIPPED.toString()));
            final boolean isStation = newSignId != null && (newSignId.equals(BlockRailwaySign.SignType.STATION.toString()) || newSignId.equals(BlockRailwaySign.SignType.STATION_FLIPPED.toString()));
            if ((isExitLetter || isPlatform || isLine || isStation) && minecraft != null) {
                UtilitiesClient.setScreen(minecraft, new DashboardListSelectorScreen(this, isExitLetter ? exitsForList : isPlatform ? platformsForList : isLine ? routesForList : stationsForList, selectedIds, false, false));
            }
        }
    }

    private void setLuminance(boolean luminance) {
        if (isModernSign) this.luminance = luminance;
        buttonLight.setMessage(Text.translatable("gui.ivr.cycle_light").append(Text.translatable(luminance ? "options.mtr.on" : "options.mtr.off")));
    }

    private void setPage(int newPage) {
        page = Mth.clamp(newPage, 0, totalPages - 1);
        buttonPrevPage.visible = editingIndex >= 0 && page > 0;
        buttonNextPage.visible = editingIndex >= 0 && page < totalPages - 1;
    }

    @FunctionalInterface
    private interface LoopSignsCallback {

        void loopSignsCallback(int index, int x, int y, boolean isBig);
    }
}
