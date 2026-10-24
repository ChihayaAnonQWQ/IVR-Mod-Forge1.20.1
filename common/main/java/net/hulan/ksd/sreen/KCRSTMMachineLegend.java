package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.mappings.SelectableMapper;
import mtr.mappings.WidgetMapper;
import net.hulan.ksd.data.SingleTicketSystem;
import net.hulan.ksd.data.KSDRoute;
import net.hulan.ksd.utils.RailDataUtilities;
import net.hulan.ksd.utils.RenderUtilities;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KCRSTMMachineLegend implements WidgetMapper, SelectableMapper, GuiEventListener, KSDGui {

    private int x;
    private int y;
    private int width;
    private int height;
    private final List<LegendRow> rows = new ArrayList<>();
    private final KCRSTMachineScreen ticketMachineScreen;
    private float scrollOffset;
    private boolean dragging;
    private float dragGrabOffsetY;

    public KCRSTMMachineLegend(KCRSTMachineScreen ticketMachineScreen) {
        this.ticketMachineScreen = ticketMachineScreen;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        final PoseStack matrices = guiGraphics.pose();
        guiGraphics.fill(x, y, x + width, y + height, ARGB_WHITE);
        // Flush the batched panel fill before the raw vertex drawing below.
        guiGraphics.flush();
        RenderUtilities renderUtilities = RenderUtilities.getInstance();
        // 逐行绘制（跳过已滚出的行，超出面板底部的行不画）
        int startRow = (int) (scrollOffset / ROW_HEIGHT);
        for (int i = startRow; i < rows.size(); i++) {
            // 该行顶边在面板内的纵坐标
            float rowY = y + LEGEND_PADDING + i * ROW_HEIGHT - scrollOffset;
            // 超出面板底部时结束绘制
            if (rowY > y + height) {
                break;
            }
            drawLegendRow(matrices, renderUtilities, rows.get(i), x + LEGEND_PADDING, rowY);
        }
        // 黑边（最后画，盖住越界的行）
        drawBorder(matrices, renderUtilities);
        // 内容超界时绘制滑动条
        if (maxScrollOffset() > 0) {
            drawScrollbar(matrices, renderUtilities);
        }
    }

    public void load(int maxHeight) {
        rows.clear();
        scrollOffset = 0;
        List<KSDRoute> routeList = new ArrayList<>(ticketMachineScreen.mainRoutes.stream().toList());
        routeList.sort(Comparator.comparing(RailDataUtilities::getMainName));
        for (KSDRoute r : routeList) {
            if (rows.stream().anyMatch(row -> RailDataUtilities.isSameRoute(row.route, r))) {
                continue;
            }
            rows.add(new LegendRow(getMainRouteColor(r), r.name, r, true));
        }
        switch (ticketMachineScreen.ticketType) {
            case MTR -> {
                rows.add(new LegendRow(OTHER_NETWORK_COLOR, "九廣鐵路|KCR", null, false));
                rows.add(new LegendRow(ORANGE_NETWORK_COLOR, "輕鐵|Light Rail", null, false));
            }
            case KCR -> {
                rows.add(new LegendRow(OTHER_NETWORK_COLOR, "地鐵|MTR", null, false));
                rows.add(new LegendRow(ORANGE_NETWORK_COLOR, "輕鐵|Light Rail", null, false));
            }
            case LRT -> {
                rows.add(new LegendRow(OTHER_NETWORK_COLOR, "九廣鐵路|KCR", null, false));
                rows.add(new LegendRow(OTHER_NETWORK_COLOR, "地鐵|MTR", null, false));
            }
        }
        height = Math.min(rows.size() * ROW_HEIGHT + LEGEND_PADDING * 2, maxHeight);
    }

    private int getMainRouteColor(KSDRoute route) {
        if (ticketMachineScreen.ticketType == SingleTicketSystem.TicketType.LRT) {
            return ORANGE_NETWORK_COLOR;
        }
        return argb(route.color);
    }

    // 绘制一行图例：站点圆图像（左右短线+圆环）在左，线路名（中文+英文两行）在右
    private void drawLegendRow(PoseStack matrices, RenderUtilities renderUtilities, LegendRow row, float rowX, float rowY) {
        // 图像圆心（行内垂直居中）
        float circleX = rowX + IMAGE_SIZE / 2.0F;
        float circleY = rowY + ROW_HEIGHT / 2.0F;
        // 先画穿过圆心的水平粗线（颜色与线路一致）
        renderUtilities.drawThickLine(matrices, circleX - RADIUS - STUB_LENGTH, circleY, circleX + RADIUS + STUB_LENGTH, circleY, 2.0F, row.color);
        if (row.drawCircle) {
            // 白色实心圆盖住中间，两侧各露出 3 像素短线
            renderUtilities.drawStationCircle(matrices, circleX, circleY, RADIUS, 16, RADIUS, ARGB_WHITE);
            // 线路色圆环
            renderUtilities.drawStationCircle(matrices, circleX, circleY, RADIUS, 16, RING_THICKNESS, row.color);
        }
        // 线路名：中文在上、英文在下，垂直居中于行内
        renderUtilities.drawTextCjk(matrices, row.name, rowX + IMAGE_SIZE + IMAGE_TEXT_GAP, circleY, CN_SCALE, EN_SCALE, ARGB_BLACK);
    }

    // 绘制 1 像素黑色边框
    private void drawBorder(PoseStack matrices, RenderUtilities renderUtilities) {
        // 顶边
        renderUtilities.drawFilledRectangle(matrices, x, y, x + width, y + 1, ARGB_BLACK);
        // 底边
        renderUtilities.drawFilledRectangle(matrices, x, y + height - 1, x + width, y + height, ARGB_BLACK);
        // 左边
        renderUtilities.drawFilledRectangle(matrices, x, y, x + 1, y + height, ARGB_BLACK);
        // 右边
        renderUtilities.drawFilledRectangle(matrices, x + width - 1, y, x + width, y + height, ARGB_BLACK);
    }

    // 绘制滑动条：右侧轨道 + 滑块
    private void drawScrollbar(PoseStack matrices, RenderUtilities renderUtilities) {
        // 轨道
        renderUtilities.drawFilledRectangle(matrices, trackX(), y + LEGEND_PADDING, trackX() + TRACK_WIDTH, y + height - LEGEND_PADDING, 0xFFCCCCCC);
        // 滑块（拖动时加深）
        int thumbColor = dragging ? 0xFF404040 : 0xFF808080;
        renderUtilities.drawFilledRectangle(matrices, trackX(), thumbY(), trackX() + TRACK_WIDTH, thumbY() + thumbHeight(), thumbColor);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // 内容放得下时不拦截滚轮
        if (maxScrollOffset() <= 0) {
            return false;
        }
        // 只处理悬停在图例上时的滚动
        if (isMouseOver(mouseX, mouseY)) {
            // 向上滚动（amount 为正）减少偏移，向下滚动增加偏移
            scrollOffset -= (float) (amount * ROW_HEIGHT);
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset());
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            // 命中滑块则进入拖动态并记录抓取点相对滑块顶边的偏移
            if (maxScrollOffset() > 0 && mouseX >= trackX() && mouseX <= trackX() + TRACK_WIDTH
                    && mouseY >= thumbY() && mouseY <= thumbY() + thumbHeight()) {
                dragging = true;
                dragGrabOffsetY = (float) mouseY - thumbY();
            }
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // 只有拖动滑块时才处理滚动
        if (dragging) {
            if (maxScrollOffset() > 0) {
                // 滑块顶边相对轨道顶部的比例换算成滚动偏移
                float thumbTop = (float) mouseY - dragGrabOffsetY - y - LEGEND_PADDING;
                float ratio = thumbTop / (visibleHeight() - thumbHeight());
                scrollOffset = Mth.clamp(ratio * maxScrollOffset(), 0, maxScrollOffset());
            }
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 结束拖动状态
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= (double) x && mouseY >= (double) y && mouseX < (double) (x + width) && mouseY < (double) (y + height);
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

    // 内容可视高度（不含上下内边距）
    private int visibleHeight() {
        return height - LEGEND_PADDING * 2;
    }

    // 内容总高度
    private int contentHeight() {
        return rows.size() * ROW_HEIGHT;
    }

    // 最大滚动偏移（内容放得下时为 0）
    private float maxScrollOffset() {
        return Math.max(0, contentHeight() - visibleHeight());
    }

    // 轨道 X 坐标
    private float trackX() {
        return x + width - LEGEND_PADDING - TRACK_WIDTH;
    }

    // 滑块高度：按可见/内容比例计算，带最小高度
    private float thumbHeight() {
        return Math.max(THUMB_MIN_HEIGHT, visibleHeight() * (float) visibleHeight() / contentHeight());
    }

    // 滑块顶部 Y 坐标
    private float thumbY() {
        return y + LEGEND_PADDING + (visibleHeight() - thumbHeight()) * (scrollOffset / maxScrollOffset());
    }

    // 把 RGB 颜色补全为不透明的 ARGB
    private static int argb(int color) {
        return 0xFF000000 | (color & 0xFFFFFF);
    }

    // 图例行数据：线路对象、线路颜色与线路名（「中文|English||注释」形式）
    private static final class LegendRow {

        final KSDRoute route;
        final int color;
        final String name;
        final boolean drawCircle;

        // 构造器：记录线路对象、颜色与线路名
        LegendRow(int color, String name, KSDRoute route, boolean drawCircle) {
            this.color = color;
            this.name = name;
            this.route = route;
            this.drawCircle = drawCircle;
        }
    }
}
