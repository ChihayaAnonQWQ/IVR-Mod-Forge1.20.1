package net.hulan.ksd.sreen;

import mtr.data.IGui;
import net.minecraft.client.gui.GuiGraphics;

public interface KSDGui extends IGui {

    //Paying Screen
    int PAYING_PADDING = 50;
    int RGB_RED = 0xFF0000;

    //RMS
    int RMH_HEADER_HEIGHT = 50;
    int RMH_PADDING = 10;
    float RMH_CHI_HEIGHT = 40F;
    float RMH_ENG_HEIGHT = 20F;
    int RGB_HEADER_BLUE = 0x004684;
    int LEGEND_WIDTH = 100;

    //Rail Map
    double SCALE_UPPER_LIMIT = 64F;
    double SCALE_LOWER_LIMIT = 0.0078125F;
    float RADIUS = 5F;
    int RADIUS_PADDING = 8;
    int SEGMENTS = 64;
    float LINE_WIDTH = 5.0F;
    float STATION_RING_THICKNESS = 1.5F;
    int LIGHT_RAIL_COLOR = 0xFFF98C2B;
    // 站名文字缩放倍数：原版字体行高 9 像素，1.2 倍约等于原动态贴图（10 像素高）的显示效果
    float STATION_NAME_SCALE = 1.2F;
    // 站名英文行缩放倍数（小号，置于中文行下方）
    float STATION_EN_SCALE = 0.7F;

    //Rail Map Legend
    // 面板内边距
    int LEGEND_PADDING = 8;
    // 行高（中文 1.0 倍 + 英文 0.65 倍 + 行距，约 18.5 像素，留余量）
    int ROW_HEIGHT = 24;
    // 行内图像区宽度
    int IMAGE_SIZE = 16;
    // 图像与文字之间的间距
    int IMAGE_TEXT_GAP = 12;
    // 滑动条轨道宽度
    int TRACK_WIDTH = 6;
    // 滑块最小高度
    int THUMB_MIN_HEIGHT = 20;
    // 圆环厚度
    float RING_THICKNESS = 1.5F;
    // 圆两侧短线长度（小于半径 5）
    float STUB_LENGTH = 3.0F;
    // 线路名中文行缩放
    float CN_SCALE = 1.0F;
    // 线路名英文行缩放
    float EN_SCALE = 0.65F;
    int OTHER_NETWORK_COLOR = 0xFF808080;
    int ORANGE_NETWORK_COLOR = 0xFFF98C2B;

    //
    int BUTTON_WIDTH = 100;
    int BUTTON_HEIGHT = 50;
    int BUTTON_GAP = 10;

    static void renderBg(GuiGraphics guiGraphics, int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        panelWidth = Math.max(8, panelWidth);
        panelHeight = Math.max(8, panelHeight);
        final int left = screenWidth / 2 - panelWidth / 2;
        final int top = screenHeight / 2 - panelHeight / 2;
        final int right = left + panelWidth;
        final int bottom = top + panelHeight;
        guiGraphics.fill(left, top, right, bottom, 0xFFC6C6C6);
        guiGraphics.fill(left + 4, top + 4, right - 4, bottom - 4, 0xFF4A4A4A);
    }
}
