package net.hulan.ksd.sreen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.*;
import mtr.mappings.SelectableMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.mappings.WidgetMapper;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.KSDAreaBase;
import net.hulan.ksd.data.KSDStation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class KSDWidgetMap implements WidgetMapper, SelectableMapper, GuiEventListener, IGui {

    public MapType mapType;
    private int x;
    private int y;
    private int width;
    private int height;
    private double scale;
    private double centerX;
    private double centerY;
    private Tuple<Integer, Integer> drawArea1;
    private Tuple<Integer, Integer> drawArea2;
    private MapState mapState;
    private final TransportMode transportMode;
    private final OnDrawCorners onDrawCorners;
    private final Runnable onDrawCornersMouseRelease;
    private final Consumer<Long> onClickAddPlatformToRoute;
    private final Consumer<SavedRailBase> onClickEditSavedRail;
    private final BiFunction<Double, Double, Boolean> isRestrictedMouseArea;
    private final ClientLevel world;
    private final LocalPlayer player;
    private final Font textRenderer;
    private static final int ARGB_BLUE = -12417548;
    private static final double SCALE_UPPER_LIMIT = 64F;
    private static final double SCALE_LOWER_LIMIT = 0.0078125F;

    public KSDWidgetMap(MapType mapType, TransportMode transportMode, OnDrawCorners onDrawCorners, Runnable onDrawCornersMouseRelease, Consumer<Long> onClickAddPlatformToRoute, Consumer<SavedRailBase> onClickEditSavedRail, BiFunction<Double, Double, Boolean> isRestrictedMouseArea) {
        this.mapType = mapType;
        this.transportMode = transportMode;
        this.onDrawCorners = onDrawCorners;
        this.onDrawCornersMouseRelease = onDrawCornersMouseRelease;
        this.onClickAddPlatformToRoute = onClickAddPlatformToRoute;
        this.onClickEditSavedRail = onClickEditSavedRail;
        this.isRestrictedMouseArea = isRestrictedMouseArea;
        Minecraft minecraftClient = Minecraft.getInstance();
        world = minecraftClient.level;
        player = minecraftClient.player;
        textRenderer = minecraftClient.font;
        if (player == null) {
            centerX = 0.0F;
            centerY = 0.0F;
        } else {
            centerX = mapType != MapType.Z_Y ? player.getX() : player.getZ();
            centerY = mapType != MapType.X_Z ? player.getY() : player.getZ();
        }
        scale = 1.0F;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        PoseStack matrices = guiGraphics.pose();
        // Flush any batched GUI content before drawing the map through raw vertex buffers.
        guiGraphics.flush();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        UtilitiesClient.beginDrawingRectangle(buffer);
        RenderSystem.enableBlend();
        switch (mapType) {
            case X_Y -> {
                Tuple<Integer, Integer> topLeft = cordsToWorldPos(0, 0);
                Tuple<Integer, Integer> bottomRight = cordsToWorldPos(width, height);
                int increment = scale >= (double)1.0F ? 1 : (int)Math.ceil((double)1.0F / scale);
                for(int i = topLeft.getA(); i <= bottomRight.getA(); i += increment) {
                    for(int j = topLeft.getB(); j >= bottomRight.getB(); j -= increment) {
                        int color;
                        if (world != null) {
                            BlockPos blockPos = RailwayData.newBlockPos(i, j, player.getZ());
                            BlockState blockState = world.getBlockState(blockPos);
                            color = divideColorRGB(blockState.getMapColor(world, blockPos).col);
                            drawRectangleFromWorldCords(buffer, i, j, i + increment, j - increment, ARGB_BLACK | color);
                        }
                    }
                }
                Tuple<Double, Double> mouseWorldPos = cordsToWorldPos((double) (mouseX - x), mouseY - y);
                try {
                    KSDClientData.DATA_CACHE.getPosToPlatforms(transportMode).forEach((
                            platformPos,
                            platforms) ->
                            drawRectangleFromWorldCords(
                                    buffer,
                                    platformPos.getX(),
                                    platformPos.getY(),
                                    platformPos.getX() + 1,
                                    platformPos.getY() - 1,
                                    -1));
                    for(KSDStation station : KSDClientData.STATIONS) {
                        if (KSDAreaBase.nonNullCorners(station)) {
                            drawRectangleFromWorldCords(buffer, station.corner1.toTupleWithXY(), station.corner2.toTupleWithXY(), ARGB_BLACK_TRANSLUCENT + station.color);
                        }
                    }
                    mouseOnSavedRail(mouseWorldPos, (savedRail, x1, z1, x2, z2) -> drawRectangleFromWorldCords(buffer, x1, z1, x2, z2, -1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (mapState == MapState.EDITING_AREA && drawArea1 != null && drawArea2 != null) {
                    drawRectangleFromWorldCords(buffer, drawArea1, drawArea2, Integer.MAX_VALUE);
                }
                if (player != null) {
                    drawFromWorldCords(player.getX(), player.getY(), (x1, y1) -> {
                        drawRectangle(buffer, x1 - (double) 2.0F, y1 - (double) 3.0F, x1 + (double) 2.0F, y1 + (double) 3.0F, -1);
                        drawRectangle(buffer, x1 - (double) 3.0F, y1 - (double) 2.0F, x1 + (double) 3.0F, y1 + (double) 2.0F, -1);
                        drawRectangle(buffer, x1 - (double) 2.0F, y1 - (double) 2.0F, x1 + (double) 2.0F, y1 + (double) 2.0F, ARGB_BLUE);
                    });
                }
                tesselator.end();
                RenderSystem.disableBlend();
                UtilitiesClient.finishDrawingRectangle();
                if (mapState == MapState.EDITING_AREA) {
                    guiGraphics.drawString(textRenderer, Text.translatable("gui.mtr.edit_area").getString(), x + TEXT_PADDING, y + TEXT_PADDING, ARGB_WHITE);
                } else if (mapState == MapState.EDITING_ROUTE) {
                    guiGraphics.drawString(textRenderer, Text.translatable("gui.mtr.edit_route").getString(), x + TEXT_PADDING, y + TEXT_PADDING, ARGB_WHITE);
                }
                if (scale >= (double) 8.0F) {
                    try {
                        KSDClientData.DATA_CACHE.getPosToPlatforms(transportMode).forEach((platformPos, platforms) -> drawSavedRail(guiGraphics, platformPos, platforms));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
                for (KSDStation station : KSDClientData.STATIONS) {
                    if (canDrawAreaText(station)) {
                        BlockPos pos = station.getCenter();
                        String stationString = String.format("%s|(%s)", station.name, Text.translatable("gui.mtr.zone_number", station.zone).getString());
                        drawFromWorldCords(pos.getX(), pos.getY(), (x1, y1) -> IDrawing.drawStringWithFont(matrices, this.textRenderer, immediate, stationString, (float) this.x + x1.floatValue(), (float) this.y + y1.floatValue(), 15728880));
                    }
                }
                immediate.endBatch();
                String mousePosText = "X-Y" + String.format("(%s, %s)", RailwayData.round(mouseWorldPos.getA(), 1), RailwayData.round(mouseWorldPos.getB(), 1));
                guiGraphics.drawString(this.textRenderer, mousePosText, this.x + this.width - 6 - this.textRenderer.width(mousePosText), this.y + 6, -1);
            }
            case X_Z -> {
                Tuple<Integer, Integer> topLeft = cordsToWorldPos(0, 0);
                Tuple<Integer, Integer> bottomRight = cordsToWorldPos(width, height);
                int increment = scale >= (double)1.0F ? 1 : (int)Math.ceil((double)1.0F / scale);
                for(int i = topLeft.getA(); i <= bottomRight.getA(); i += increment) {
                    for(int j = topLeft.getB(); j <= bottomRight.getB(); j += increment) {
                        int color;
                        if (world != null) {
                            BlockPos blockPos = RailwayData.newBlockPos(i, world.getHeight(Heightmap.Types.MOTION_BLOCKING, i, j) - 1, j);
                            BlockState blockState = world.getBlockState(blockPos);
                            color = divideColorRGB(blockState.getMapColor(world, blockPos).col);
                            drawRectangleFromWorldCords(buffer, i, j, i + increment, j + increment, ARGB_BLACK | color);
                        }
                    }
                }
                Tuple<Double, Double> mouseWorldPos = cordsToWorldPos((double) (mouseX - x), mouseY - y);
                try {
                    KSDClientData.DATA_CACHE.getPosToPlatforms(transportMode).forEach((platformPos,
                                                                                    platforms) ->
                            drawRectangleFromWorldCords(
                                    buffer,
                                    platformPos.getX(),
                                    platformPos.getZ(),
                                    platformPos.getX() + 1,
                                    platformPos.getZ() + 1,
                                    -1));
                    for(KSDStation station : KSDClientData.STATIONS) {
                        if (KSDAreaBase.nonNullCorners(station)) {
                            drawRectangleFromWorldCords(buffer, station.corner1.toTupleWithXZ(), station.corner2.toTupleWithXZ(), ARGB_BLACK_TRANSLUCENT + station.color);
                        }
                    }
                    mouseOnSavedRail(mouseWorldPos, (savedRail, x1, z1, x2, z2) -> drawRectangleFromWorldCords(buffer, x1, z1, x2, z2, -1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (mapState == MapState.EDITING_AREA && drawArea1 != null && drawArea2 != null) {
                    drawRectangleFromWorldCords(buffer, drawArea1, drawArea2, Integer.MAX_VALUE);
                }
                if (player != null) {
                    drawFromWorldCords(player.getX(), player.getZ(), (x1, y1) -> {
                        drawRectangle(buffer, x1 - (double) 2.0F, y1 - (double) 3.0F, x1 + (double) 2.0F, y1 + (double) 3.0F, -1);
                        drawRectangle(buffer, x1 - (double) 3.0F, y1 - (double) 2.0F, x1 + (double) 3.0F, y1 + (double) 2.0F, -1);
                        drawRectangle(buffer, x1 - (double) 2.0F, y1 - (double) 2.0F, x1 + (double) 2.0F, y1 + (double) 2.0F, ARGB_BLUE);
                    });
                }
                tesselator.end();
                RenderSystem.disableBlend();
                UtilitiesClient.finishDrawingRectangle();
                if (mapState == MapState.EDITING_AREA) {
                    guiGraphics.drawString(textRenderer, Text.translatable("gui.mtr.edit_area").getString(), x + TEXT_PADDING, y + TEXT_PADDING, ARGB_WHITE);
                } else if (mapState == MapState.EDITING_ROUTE) {
                    guiGraphics.drawString(textRenderer, Text.translatable("gui.mtr.edit_route").getString(), x + TEXT_PADDING, y + TEXT_PADDING, ARGB_WHITE);
                }
                if (scale >= (double) 8.0F) {
                    try {
                        KSDClientData.DATA_CACHE.getPosToPlatforms(transportMode).forEach((platformPos, platforms) -> this.drawSavedRail(guiGraphics, platformPos, platforms));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
                for (KSDStation station : KSDClientData.STATIONS) {
                    if (canDrawAreaText(station)) {
                        BlockPos pos = station.getCenter();
                        String stationString = String.format("%s|(%s)", station.name, Text.translatable("gui.mtr.zone_number", station.zone).getString());
                        drawFromWorldCords(pos.getX(), pos.getZ(), (x1, y1) -> IDrawing.drawStringWithFont(matrices, this.textRenderer, immediate, stationString, (float) this.x + x1.floatValue(), (float) this.y + y1.floatValue(), 15728880));
                    }
                }
                immediate.endBatch();
                String mousePosText = "X-Z" + String.format("(%s, %s)", RailwayData.round(mouseWorldPos.getA(), 1), RailwayData.round(mouseWorldPos.getB(), 1));
                guiGraphics.drawString(this.textRenderer, mousePosText, this.x + this.width - 6 - this.textRenderer.width(mousePosText), this.y + 6, -1);
            }
            case Z_Y -> {
                Tuple<Integer, Integer> topLeft = cordsToWorldPos(0, 0);
                Tuple<Integer, Integer> bottomRight = cordsToWorldPos(width, height);
                int increment = scale >= (double)1.0F ? 1 : (int)Math.ceil((double)1.0F / scale);
                for(int i = topLeft.getA(); i <= bottomRight.getA(); i += increment) {
                    for(int j = topLeft.getB(); j >= bottomRight.getB(); j -= increment) {
                        int color;
                        if (world != null) {
                            BlockPos blockPos = RailwayData.newBlockPos(player.getX(), j, i);
                            BlockState blockState = world.getBlockState(blockPos);
                            color = divideColorRGB(blockState.getMapColor(world, blockPos).col);
                            drawRectangleFromWorldCords(buffer, i, j, i + increment, j - increment, ARGB_BLACK | color);
                        }
                    }
                }
                Tuple<Double, Double> mouseWorldPos = cordsToWorldPos((double) (mouseX - x), mouseY - y);
                try {
                    KSDClientData.DATA_CACHE.getPosToPlatforms(transportMode).forEach((platformPos,
                                                                                    platforms) ->
                            drawRectangleFromWorldCords(
                                    buffer,
                                    platformPos.getZ(),
                                    platformPos.getY(),
                                    platformPos.getZ() + 1,
                                    platformPos.getY() - 1,
                                    -1));
                    for(KSDStation station : KSDClientData.STATIONS) {
                        if (KSDAreaBase.nonNullCorners(station)) {
                            drawRectangleFromWorldCords(buffer, station.corner1.toTupleWithZY(), station.corner2.toTupleWithZY(), ARGB_BLACK_TRANSLUCENT + station.color);
                        }
                    }
                    mouseOnSavedRail(mouseWorldPos, (savedRail, x1, z1, x2, z2) -> drawRectangleFromWorldCords(buffer, x1, z1, x2, z2, -1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (player != null) {
                    drawFromWorldCords(player.getZ(), player.getY(), (x1, y1) -> {
                        drawRectangle(buffer, x1 - (double) 2.0F, y1 - (double) 3.0F, x1 + (double) 2.0F, y1 + (double) 3.0F, -1);
                        drawRectangle(buffer, x1 - (double) 3.0F, y1 - (double) 2.0F, x1 + (double) 3.0F, y1 + (double) 2.0F, -1);
                        drawRectangle(buffer, x1 - (double) 2.0F, y1 - (double) 2.0F, x1 + (double) 2.0F, y1 + (double) 2.0F, ARGB_BLUE);
                    });
                }
                if (mapState == MapState.EDITING_AREA && drawArea1 != null && drawArea2 != null) {
                    drawRectangleFromWorldCords(buffer, drawArea1, drawArea2, Integer.MAX_VALUE);
                }
                tesselator.end();
                RenderSystem.disableBlend();
                UtilitiesClient.finishDrawingRectangle();
                if (mapState == MapState.EDITING_AREA) {
                    guiGraphics.drawString(textRenderer, Text.translatable("gui.mtr.edit_area").getString(), x + TEXT_PADDING, y + TEXT_PADDING, ARGB_WHITE);
                } else if (mapState == MapState.EDITING_ROUTE) {
                    guiGraphics.drawString(textRenderer, Text.translatable("gui.mtr.edit_route").getString(), x + TEXT_PADDING, y + TEXT_PADDING, ARGB_WHITE);
                }
                if (scale >= (double) 8.0F) {
                    try {
                        KSDClientData.DATA_CACHE.getPosToPlatforms(transportMode).forEach((platformPos, platforms) -> drawSavedRail(guiGraphics, platformPos, platforms));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
                for (KSDStation station : KSDClientData.STATIONS) {
                    if (canDrawAreaText(station)) {
                        BlockPos pos = station.getCenter();
                        String stationString = String.format("%s|(%s)", station.name, Text.translatable("gui.mtr.zone_number", station.zone).getString());
                        drawFromWorldCords(pos.getZ(), pos.getY(), (x1, y1) -> IDrawing.drawStringWithFont(matrices, textRenderer, immediate, stationString, (float) x + x1.floatValue(), (float) y + y1.floatValue(), 15728880));
                    }
                }
                immediate.endBatch();
                String mousePosText = "Z-Y" + String.format("(%s, %s)", RailwayData.round(mouseWorldPos.getA(), 1), RailwayData.round(mouseWorldPos.getB(), 1));
                guiGraphics.drawString(textRenderer, mousePosText, x + width - 6 - textRenderer.width(mousePosText), y + 6, -1);
            }
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mapState == MapState.EDITING_AREA) {
            drawArea2 = cordsToWorldPos((int)Math.round(mouseX - (double)x), (int)Math.round(mouseY - (double)y));
            if (drawArea1.getA().equals(drawArea2.getA())) {
                drawArea2 = new Tuple<>(drawArea2.getA() + 1, drawArea2.getB());
            }
            if (drawArea1.getB().equals(drawArea2.getB())) {
                drawArea2 = new Tuple<>(drawArea2.getA(), drawArea2.getB() + 1);
            }
            onDrawCorners.onDrawCorners(drawArea1, drawArea2);
        } else {
            switch (mapType) {
                case X_Y, Z_Y -> {
                    centerX -= deltaX / scale;
                    centerY += deltaY / scale;
                }
                case X_Z -> {
                    centerX -= deltaX / scale;
                    centerY -= deltaY / scale;
                }
            }
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mapState == MapState.EDITING_AREA) {
            this.onDrawCornersMouseRelease.run();
        }
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            if (ClientData.hasPermission()) {
                if (this.mapState == MapState.EDITING_AREA) {
                    this.drawArea1 = this.cordsToWorldPos((int)(mouseX - (double)this.x), (int)(mouseY - (double)this.y));
                    this.drawArea2 = null;
                } else if (mapState == MapState.EDITING_ROUTE) {
                    final Tuple<Double, Double> mouseWorldPos = cordsToWorldPos(mouseX - x, mouseY - y);
                    mouseOnSavedRail(mouseWorldPos, (savedRail, x1, z1, x2, z2) -> onClickAddPlatformToRoute.accept(savedRail.id));
                } else {
                    final Tuple<Double, Double> mouseWorldPos = cordsToWorldPos(mouseX - x, mouseY - y);
                    mouseOnSavedRail(mouseWorldPos, (savedRail, x1, z1, x2, z2) -> onClickEditSavedRail.accept(savedRail));
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        switch (mapType) {
            case X_Y, Z_Y -> {
                double oldScale = this.scale;
                if (oldScale > SCALE_LOWER_LIMIT && amount < (double)0.0F) {
                    this.centerX -= (mouseX - (double)this.x - (double)this.width / (double)2.0F) / this.scale;
                    this.centerY += (mouseY - (double)this.y - (double)this.height / (double)2.0F) / this.scale;
                }
                this.scale(amount);
                if (oldScale < SCALE_UPPER_LIMIT && amount > (double)0.0F) {
                    this.centerX += (mouseX - (double)this.x - (double)this.width / (double)2.0F) / this.scale;
                    this.centerY -= (mouseY - (double)this.y - (double)this.height / (double)2.0F) / this.scale;
                }
                return true;
            }
            case X_Z -> {
                double oldScale = this.scale;
                if (oldScale > SCALE_LOWER_LIMIT && amount < (double)0.0F) {
                    this.centerX -= (mouseX - (double)this.x - (double)this.width / (double)2.0F) / this.scale;
                    this.centerY -= (mouseY - (double)this.y - (double)this.height / (double)2.0F) / this.scale;
                }
                this.scale(amount);
                if (oldScale < SCALE_UPPER_LIMIT && amount > (double)0.0F) {
                    this.centerX += (mouseX - (double)this.x - (double)this.width / (double)2.0F) / this.scale;
                    this.centerY += (mouseY - (double)this.y - (double)this.height / (double)2.0F) / this.scale;
                }
                return true;
            }
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= (double)this.x
                && mouseY >= (double)this.y
                && mouseX < (double)(this.x + this.width)
                && mouseY < (double)(this.y + this.height)
                && (!(mouseX >= (double)(this.x + this.width - 300)) || !(mouseY >= (double)(this.y + this.height - 20)))
                && !(Boolean)this.isRestrictedMouseArea.apply(mouseX, mouseY);
    }

    public void setFocused(boolean focused) {
    }

    public boolean isFocused() {
        return false;
    }

    public void setPositionAndSize(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void scale(double amount) {
        this.scale *= Math.pow(2.0F, amount);
        this.scale = Mth.clamp(this.scale, SCALE_LOWER_LIMIT, SCALE_UPPER_LIMIT);
    }

    public void find(double x1, double z1, double x2, double z2) {
        this.centerX = (x1 + x2) / (double)2.0F;
        this.centerY = (z1 + z2) / (double)2.0F;
        this.scale = Math.max(2.0F, this.scale);
    }

    public void find(BlockPos pos) {
        switch (mapType) {
            case X_Y -> {
                this.centerX = pos.getX();
                this.centerY = pos.getY();
            }
            case X_Z -> {
                this.centerX = pos.getX();
                this.centerY = pos.getZ();
            }
            case Z_Y -> {
                this.centerX = pos.getZ();
                this.centerY = pos.getY();
            }
        }
        this.scale = Math.max(8.0F, this.scale);
    }

    public void startEditingArea(KSDAreaBase editingArea) {
        mapState = MapState.EDITING_AREA;
        switch (mapType) {
            case X_Y -> {
                drawArea1 = editingArea.corner1 != null ? editingArea.corner1.toTupleWithXY() : null;
                drawArea2 = editingArea.corner2 != null ? editingArea.corner2.toTupleWithXY() : null;
            }
            case X_Z -> {
                drawArea1 = editingArea.corner1 != null ? editingArea.corner1.toTupleWithXZ() : null;
                drawArea2 = editingArea.corner2 != null ? editingArea.corner2.toTupleWithXZ() : null;
            }
            case Z_Y -> {
                drawArea1 = editingArea.corner1 != null ? editingArea.corner1.toTupleWithZY() : null;
                drawArea2 = editingArea.corner2 != null ? editingArea.corner2.toTupleWithZY() : null;
            }
        }
    }

    public void startEditingRoute() {
        mapState = MapState.EDITING_ROUTE;
    }

    public void stopEditing() {
        mapState = MapState.DEFAULT;
    }

    public void setMapType(MapType mapType, KSDAreaBase editingArea) {
        this.mapType = mapType;
        if (player == null) {
            centerX = 0.0F;
            centerY = 0.0F;
        } else {
            centerX = mapType != MapType.Z_Y ? player.getX() : player.getZ();
            centerY = mapType != MapType.X_Z ? player.getY() : player.getZ();
        }
        if (mapState.equals(MapState.EDITING_AREA) && editingArea != null) {
            switch (this.mapType) {
                case X_Y -> {
                    drawArea1 = editingArea.corner1 != null ? editingArea.corner1.toTupleWithXY() : null;
                    drawArea2 = editingArea.corner2 != null ? editingArea.corner2.toTupleWithXY() : null;
                }
                case X_Z -> {
                    drawArea1 = editingArea.corner1 != null ? editingArea.corner1.toTupleWithXZ() : null;
                    drawArea2 = editingArea.corner2 != null ? editingArea.corner2.toTupleWithXZ() : null;
                }
                case Z_Y -> {
                    drawArea1 = editingArea.corner1 != null ? editingArea.corner1.toTupleWithZY() : null;
                    drawArea2 = editingArea.corner2 != null ? editingArea.corner2.toTupleWithZY() : null;
                }
            }
        }
    }

    private void mouseOnSavedRail(Tuple<Double, Double> mouseWorldPos, MouseOnSavedRailCallback mouseOnSavedRailCallback) {
        try {
            KSDClientData.DATA_CACHE.getPosToPlatforms(transportMode).forEach((savedRailPos, savedRails) -> {
                int savedRailCount = savedRails.size();
                for(int i = 0; i < savedRailCount; ++i) {
                    switch (mapType) {
                        case X_Y -> {
                            float left = savedRailPos.getX();
                            float right = (savedRailPos.getX() + 1);
                            float top = savedRailPos.getY() + (float) i / savedRailCount;
                            float bottom = savedRailPos.getY() + (i - 1.0F) / savedRailCount;
                            if (RailwayData.isBetween(mouseWorldPos.getA(), left, right) && RailwayData.isBetween(mouseWorldPos.getB(), top, bottom)) {
                                mouseOnSavedRailCallback.mouseOnSavedRailCallback(savedRails.get(i), left, top, right, bottom);
                            }
                        }
                        case X_Z -> {
                            float left = savedRailPos.getX();
                            float right = (savedRailPos.getX() + 1);
                            float top = savedRailPos.getZ() + (float) i / savedRailCount;
                            float bottom = savedRailPos.getZ() + (i + 1.0F) / savedRailCount;
                            if (RailwayData.isBetween(mouseWorldPos.getA(), left, right) && RailwayData.isBetween(mouseWorldPos.getB(), top, bottom)) {
                                mouseOnSavedRailCallback.mouseOnSavedRailCallback(savedRails.get(i), left, top, right, bottom);
                            }
                        }
                        case Z_Y -> {
                            float left = savedRailPos.getZ();
                            float right = (savedRailPos.getZ() + 1);
                            float top = savedRailPos.getY() + (float) i / savedRailCount;
                            float bottom = savedRailPos.getY() + (i - 1.0F) / savedRailCount;
                            if (RailwayData.isBetween(mouseWorldPos.getA(), left, right) && RailwayData.isBetween(mouseWorldPos.getB(), top, bottom)) {
                                mouseOnSavedRailCallback.mouseOnSavedRailCallback(savedRails.get(i), left, top, right, bottom);
                            }
                        }
                    }
                }
            });
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Tuple<Integer, Integer> cordsToWorldPos(int mouseX, int mouseY) {
        Tuple<Double, Double> worldPos = cordsToWorldPos(mouseX, (double)mouseY);
        return switch (mapType) {
            case X_Y, Z_Y -> new Tuple<>((int)Math.floor(worldPos.getA()), (int)Math.ceil(worldPos.getB()));
            case X_Z -> new Tuple<>((int)Math.floor(worldPos.getA()), (int)Math.floor(worldPos.getB()));
        };
    }

    private Tuple<Double, Double> cordsToWorldPos(double mouseX, double mouseY) {
        switch (mapType) {
            case X_Y, Z_Y -> {
                double left = (mouseX - (double) width / (double)2.0F) / scale + centerX;
                double right = -((mouseY - (double) height / (double)2.0F) / scale) + centerY;
                return new Tuple<>(left, right);
            }
            case X_Z -> {
                double left = (mouseX - (double) width / (double)2.0F) / scale + centerX;
                double right = (mouseY - (double) height / (double)2.0F) / scale + centerY;
                return new Tuple<>(left, right);
            }
        }
        return new Tuple<>(mouseX, mouseY);
    }

    private void drawFromWorldCords(double worldX, double worldZ, BiConsumer<Double, Double> callback) {
        switch (mapType) {
            case X_Y, Z_Y -> {
                double cordsX = (worldX - centerX) * scale + (double)width / (double)2.0F;
                double cordsY = (centerY - worldZ) * scale + (double)height / (double)2.0F;
                if (RailwayData.isBetween(cordsX, 0.0F, width) && RailwayData.isBetween(cordsY, 0.0F, height)) {
                    callback.accept(cordsX, cordsY);
                }
            }
            case X_Z -> {
                double cordsX = (worldX - centerX) * scale + (double)width / (double)2.0F;
                double cordsY = (worldZ - centerY) * scale + (double)height / (double)2.0F;
                if (RailwayData.isBetween(cordsX, 0.0F, width) && RailwayData.isBetween(cordsY, 0.0F, height)) {
                    callback.accept(cordsX, cordsY);
                }
            }
        }
    }

    private void drawRectangleFromWorldCords(BufferBuilder buffer, Tuple<Integer, Integer> corner1, Tuple<Integer, Integer> corner2, int color) {
        switch (mapType) {
            case X_Y, Z_Y ->
                    drawRectangleFromWorldCords(
                            buffer,
                            corner1.getA(),
                            corner1.getB() - 1,
                            corner2.getA(),
                            corner2.getB() - 1,
                            color);
            case X_Z ->
                    drawRectangleFromWorldCords(
                            buffer,
                            corner1.getA(),
                            corner1.getB(),
                            corner2.getA(),
                            corner2.getB(),
                            color);
        }
    }

    private void drawRectangleFromWorldCords(BufferBuilder buffer, double posX1, double posZ1, double posX2, double posZ2, int color) {
        switch (mapType) {
            case X_Y, Z_Y -> {
                double x1 = (posX1 - centerX) * scale + (double)width / (double)2.0F;
                double y1 = (centerY - posZ1) * scale + (double)height / (double)2.0F;
                double x2 = (posX2 - centerX) * scale + (double)width / (double)2.0F;
                double y2 = (centerY - posZ2) * scale + (double)height / (double)2.0F;
                drawRectangle(buffer, x1, y1, x2, y2, color);
            }
            case X_Z -> {
                double x1 = (posX1 - centerX) * scale + (double)width / (double)2.0F;
                double z1 = (posZ1 - centerY) * scale + (double)height / (double)2.0F;
                double x2 = (posX2 - centerX) * scale + (double)width / (double)2.0F;
                double z2 = (posZ2 - centerY) * scale + (double)height / (double)2.0F;
                drawRectangle(buffer, x1, z1, x2, z2, color);
            }
        }
    }

    private void drawRectangle(BufferBuilder buffer, double xA, double yA, double xB, double yB, int color) {
        double x1 = Math.min(xA, xB);
        double y1 = Math.min(yA, yB);
        double x2 = Math.max(xA, xB);
        double y2 = Math.max(yA, yB);
        if (x1 < (double) width && y1 < (double) height && x2 >= (double) 0.0F && y2 >= (double) 0.0F) {
            IDrawing.drawRectangle(buffer, (double) x + Math.max(0.0F, x1), (double) y + y1, (double) x + x2, (double) y + y2, color);
        }

    }

    private boolean canDrawAreaText(KSDAreaBase areaBase) {
        return areaBase.getCenter() != null && scale >= (double) (80.0F / (float)Math.max(Math.abs(areaBase.corner1.getX() - areaBase.corner2.getX()), Math.abs(areaBase.corner1.getZ() - areaBase.corner2.getZ())));
    }

    private void drawSavedRail(GuiGraphics guiGraphics, BlockPos savedRailPos, List<? extends SavedRailBase> savedRails) {
        int savedRailCount = savedRails.size();
        for(int i = 0; i < savedRailCount; ++i) {
            int finalI = i;
            switch (mapType) {
                case X_Y ->
                        drawFromWorldCords((double)savedRailPos.getX() + (double)0.5F,
                                (double)savedRailPos.getY() + ((double)i - (double)0.5F) / (double)savedRailCount,
                                (x1, y1) -> guiGraphics.drawCenteredString(
                                        textRenderer,
                                        savedRails.get(finalI).name,
                                        x + x1.intValue(),
                                        y + y1.intValue() - 4,
                                        -1));
                case X_Z ->
                        drawFromWorldCords((double)savedRailPos.getX() + (double)0.5F,
                                (double)savedRailPos.getZ() + ((double)i + (double)0.5F) / (double)savedRailCount,
                                (x1, y1) -> guiGraphics.drawCenteredString(
                                        textRenderer,
                                        savedRails.get(finalI).name,
                                        x + x1.intValue(),
                                        y + y1.intValue() - 4,
                                        -1));
                case Z_Y ->
                        drawFromWorldCords((double)savedRailPos.getZ() + (double)0.5F,
                                (double)savedRailPos.getY() + ((double)i - (double)0.5F) / (double)savedRailCount,
                                (x1, y1) -> guiGraphics.drawCenteredString(
                                        textRenderer,
                                        savedRails.get(finalI).name,
                                        x + x1.intValue(),
                                        y + y1.intValue() - 4,
                                        -1));
            }
        }
    }

    private static int divideColorRGB(int color) {
        int r = (color >> 16 & 255) / 2;
        int g = (color >> 8 & 255) / 2;
        int b = (color & 255) / 2;
        return (r << 16) + (g << 8) + b;
    }

    private enum MapState {
        DEFAULT,
        EDITING_AREA,
        EDITING_ROUTE
    }

    @FunctionalInterface
    private interface MouseOnSavedRailCallback {
        void mouseOnSavedRailCallback(SavedRailBase var1, double var2, double var4, double var6, double var8);
    }

    @FunctionalInterface
    public interface OnDrawCorners {
        void onDrawCorners(Tuple<Integer, Integer> var1, Tuple<Integer, Integer> var2);
    }

    public enum MapType {
        X_Z,
        X_Y,
        Z_Y;

        public MapType next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
}
