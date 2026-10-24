package net.hulan.ksd.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import mtr.mappings.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.InvocationTargetException;

public abstract class RenderUtilities {

    private static RenderUtilities instance;
    private static final float LIGHT_FACTOR = 0.5f;
    private static final float ANTIALIAS_PIXELS = 1.0F; // 设置抗锯齿渐变宽度为一个物理像素
    private static final Style FONT = Style.EMPTY.withFont(new ResourceLocation("ivr", "mtr"));
    // 「中文在上、英文在下」两行文字之间的行间距（像素）
    private static final float CJK_LINE_GAP = 2.0F;
    // 一行文字的高度（自定义字体字号约 10 像素）
    private static final float TEXT_LINE_HEIGHT = 10.0F;

    public static RenderUtilities getInstance() {
        if (instance == null || instance instanceof NullRenderUtilities) {
            String version = Utilities.getIVRMinecraftVersion();
            String className = "RenderUtilities_" + version;
            RenderUtilities tempInstance = new NullRenderUtilities();
            try {
                Class<?> clazz = Class.forName("net.hulan.ksd.utils." + className);
                tempInstance = (RenderUtilities) clazz.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException e) {
                e.printStackTrace();
            }
            instance = tempInstance;
        }
        return instance;
    }

    public void drawStationCircle(PoseStack matrices,
                                  float centerX, float centerY,
                                  float radius, int segments,
                                  float borderThickness, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        beginDrawingCircle(buffer);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float innerRadius = radius - borderThickness;
        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float outerX = centerX + cos * radius;
            float outerY = centerY + sin * radius;
            float innerX = centerX + cos * innerRadius;
            float innerY = centerY + sin * innerRadius;
            buffer.vertex(matrices.last().pose(), outerX, outerY, 0)
                    .color(r, g, b, a)
                    .endVertex();
            buffer.vertex(matrices.last().pose(), innerX, innerY, 0)
                    .color(r, g, b, a)
                    .endVertex();
        }
        tesselator.end();
        float antialiasWidth = getAntialiasWidth(); // 根据界面缩放换算渐变宽度
        drawCircleEdge(matrices, centerX, centerY, radius, radius + antialiasWidth, segments, r, g, b, a, false); // 绘制圆外侧透明渐变
        if (innerRadius > 0.0F && borderThickness < radius) { // 仅为空心圆绘制内侧渐变
            drawCircleEdge(matrices, centerX, centerY, innerRadius - antialiasWidth, innerRadius, segments, r, g, b, a, true); // 绘制圆内侧透明渐变
        }
        finishDrawingCircle();
        RenderSystem.disableBlend();
    }

    public int lightenColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = (int) (r + (255 - r) * LIGHT_FACTOR);
        g = (int) (g + (255 - g) * LIGHT_FACTOR);
        b = (int) (b + (255 - b) * LIGHT_FACTOR);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public void drawFilledRectangle(PoseStack matrices, float x1, float y1, float x2, float y2, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        beginDrawingCircle(buffer);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        buffer.vertex(matrices.last().pose(), x1, y1, 0)
                .color(r, g, b, a)
                .endVertex();
        buffer.vertex(matrices.last().pose(), x1, y2, 0)
                .color(r, g, b, a)
                .endVertex();
        buffer.vertex(matrices.last().pose(), x2, y1, 0)
                .color(r, g, b, a)
                .endVertex();
        buffer.vertex(matrices.last().pose(), x2, y2, 0)
                .color(r, g, b, a)
                .endVertex();
        tesselator.end();
        finishDrawingCircle();
        RenderSystem.disableBlend();
    }

    public void drawThickLine(PoseStack matrices, float x1, float y1, float x2, float y2, float thickness, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001f) {
            return;
        }
        float nx = -dy / length * thickness / 2;
        float ny = dx / length * thickness / 2;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        beginDrawingCircle(buffer);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        buffer.vertex(matrices.last().pose(), x1 - nx, y1 - ny, 0)
                .color(r, g, b, a)
                .endVertex();
        buffer.vertex(matrices.last().pose(), x1 + nx, y1 + ny, 0)
                .color(r, g, b, a)
                .endVertex();
        buffer.vertex(matrices.last().pose(), x2 - nx, y2 - ny, 0)
                .color(r, g, b, a)
                .endVertex();
        buffer.vertex(matrices.last().pose(), x2 + nx, y2 + ny, 0)
                .color(r, g, b, a)
                .endVertex();
        tesselator.end();
        drawLineEdge(matrices, x1, y1, x2, y2, nx, ny, getAntialiasWidth(), r, g, b, a); // 绘制粗线外侧透明渐变
        finishDrawingCircle();
        RenderSystem.disableBlend();
    }

    /** 在无纹理圆形边缘绘制一个物理像素宽的透明度渐变。 */
    private void drawCircleEdge(PoseStack matrices, float centerX, float centerY, float innerRadius, float outerRadius,
                                int segments, float r, float g, float b, float a, boolean transparentInnerEdge) {
        if (innerRadius < 0.0F || outerRadius <= innerRadius) { // 检查圆环半径是否合法
            return; // 忽略无效的渐变区域
        }
        Tesselator tesselator = Tesselator.getInstance(); // 获取当前顶点绘制器
        BufferBuilder buffer = tesselator.getBuilder(); // 获取顶点缓冲区
        beginDrawingCircle(buffer); // 开始圆形三角带绘制
        for (int i = 0; i <= segments; i++) { // 遍历圆周采样点
            double angle = 2.0 * Math.PI * i / segments; // 计算当前采样角度
            float cos = (float) Math.cos(angle); // 计算角度余弦值
            float sin = (float) Math.sin(angle); // 计算角度正弦值
            float innerAlpha = transparentInnerEdge ? 0.0F : a; // 计算内圈顶点透明度
            float outerAlpha = transparentInnerEdge ? a : 0.0F; // 计算外圈顶点透明度
            buffer.vertex(matrices.last().pose(), centerX + cos * innerRadius, centerY + sin * innerRadius, 0)
                    .color(r, g, b, innerAlpha)
                    .endVertex(); // 写入内圈渐变顶点
            buffer.vertex(matrices.last().pose(), centerX + cos * outerRadius, centerY + sin * outerRadius, 0)
                    .color(r, g, b, outerAlpha)
                    .endVertex(); // 写入外圈渐变顶点
        }
        tesselator.end(); // 提交圆形渐变顶点
    }

    /** 在粗线矩形主体和端部周围绘制透明到不透明的渐变环。 */
    private void drawLineEdge(PoseStack matrices, float x1, float y1, float x2, float y2, float nx, float ny,
                              float antialiasWidth, float r, float g, float b, float a) {
        float dx = x2 - x1; // 计算线段的水平位移
        float dy = y2 - y1; // 计算线段的垂直位移
        float length = (float) Math.sqrt(dx * dx + dy * dy); // 计算线段长度
        float extensionX = dx / length * antialiasWidth; // 计算线段端点的水平外扩量
        float extensionY = dy / length * antialiasWidth; // 计算线段端点的垂直外扩量
        float normalScale = (1.0F + antialiasWidth / (float) Math.sqrt(nx * nx + ny * ny)); // 计算法线方向外扩比例

        float i0x = x1 - nx; // 计算起点左侧内边缘横坐标
        float i0y = y1 - ny; // 计算起点左侧内边缘纵坐标
        float i1x = x1 + nx; // 计算起点右侧内边缘横坐标
        float i1y = y1 + ny; // 计算起点右侧内边缘纵坐标
        float i2x = x2 + nx; // 计算终点右侧内边缘横坐标
        float i2y = y2 + ny; // 计算终点右侧内边缘纵坐标
        float i3x = x2 - nx; // 计算终点左侧内边缘横坐标
        float i3y = y2 - ny; // 计算终点左侧内边缘纵坐标
        float o0x = x1 - extensionX - nx * normalScale; // 计算起点左侧外边缘横坐标
        float o0y = y1 - extensionY - ny * normalScale; // 计算起点左侧外边缘纵坐标
        float o1x = x1 - extensionX + nx * normalScale; // 计算起点右侧外边缘横坐标
        float o1y = y1 - extensionY + ny * normalScale; // 计算起点右侧外边缘纵坐标
        float o2x = x2 + extensionX + nx * normalScale; // 计算终点右侧外边缘横坐标
        float o2y = y2 + extensionY + ny * normalScale; // 计算终点右侧外边缘纵坐标
        float o3x = x2 + extensionX - nx * normalScale; // 计算终点左侧外边缘横坐标
        float o3y = y2 + extensionY - ny * normalScale; // 计算终点左侧外边缘纵坐标

        Tesselator tesselator = Tesselator.getInstance(); // 获取当前顶点绘制器
        BufferBuilder buffer = tesselator.getBuilder(); // 获取顶点缓冲区
        beginDrawingCircle(buffer); // 开始线路渐变三角带绘制
        drawLineEdgeVertex(buffer, matrices, o0x, o0y, r, g, b, 0.0F); // 写入起点左侧外边缘顶点
        drawLineEdgeVertex(buffer, matrices, i0x, i0y, r, g, b, a); // 写入起点左侧内边缘顶点
        drawLineEdgeVertex(buffer, matrices, o3x, o3y, r, g, b, 0.0F); // 写入终点左侧外边缘顶点
        drawLineEdgeVertex(buffer, matrices, i3x, i3y, r, g, b, a); // 写入终点左侧内边缘顶点
        drawLineEdgeVertex(buffer, matrices, o2x, o2y, r, g, b, 0.0F); // 写入终点右侧外边缘顶点
        drawLineEdgeVertex(buffer, matrices, i2x, i2y, r, g, b, a); // 写入终点右侧内边缘顶点
        drawLineEdgeVertex(buffer, matrices, o1x, o1y, r, g, b, 0.0F); // 写入起点右侧外边缘顶点
        drawLineEdgeVertex(buffer, matrices, i1x, i1y, r, g, b, a); // 写入起点右侧内边缘顶点
        drawLineEdgeVertex(buffer, matrices, o0x, o0y, r, g, b, 0.0F); // 闭合外边缘渐变带
        drawLineEdgeVertex(buffer, matrices, i0x, i0y, r, g, b, a); // 闭合内边缘渐变带
        tesselator.end(); // 提交线路渐变顶点
    }

    private static void drawLineEdgeVertex(BufferBuilder buffer, PoseStack matrices, float x, float y,
                                           float r, float g, float b, float a) {
        buffer.vertex(matrices.last().pose(), x, y, 0)
                .color(r, g, b, a)
                .endVertex(); // 写入一个线路渐变顶点
    }

    private static float getAntialiasWidth() {
        return ANTIALIAS_PIXELS / (float) Math.max(1.0D, Minecraft.getInstance().getWindow().getGuiScale()); // 将物理像素宽度换算为 GUI 坐标
    }

    public void drawTexture(PoseStack matrices, ResourceLocation resourceLocation, float x, float y, float width, float height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        beginDrawingTexture(buffer, resourceLocation);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        buffer.vertex(matrices.last().pose(), x, y, 0).uv(0, 0).endVertex();
        buffer.vertex(matrices.last().pose(), x, y + height, 0).uv(0, 1).endVertex();
        buffer.vertex(matrices.last().pose(), x + width, y + height, 0).uv(1, 1).endVertex();
        buffer.vertex(matrices.last().pose(), x + width, y, 0).uv(1, 0).endVertex();
        tesselator.end();
        finishDrawingTexture();
        RenderSystem.disableBlend();
    }

    public void drawText(PoseStack matrices, String text, float x, float y, float maxWidth, float maxHeight, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0 || maxHeight <= 0) {
            return;
        }
        float textWidth = getTextWidth(text, 1.0F);
        float textHeight = Minecraft.getInstance().font.lineHeight;
        if (textWidth <= 0) {
            return;
        }
        float scale = Math.min(maxWidth / textWidth, maxHeight / textHeight);
        float offsetY = (maxHeight - textHeight * scale) / 2;
        drawText(matrices, text, x, y + offsetY, scale, color);
    }

    public void drawText(PoseStack matrices, String text, float x, float y, float scale, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        matrices.pushPose();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1.0F);
        // 1.20.1 removed Font#draw(PoseStack, ...); draw through a GuiGraphics
        // view that carries the caller's transform instead.
        final Minecraft minecraft = Minecraft.getInstance();
        final GuiGraphics guiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        guiGraphics.pose().mulPoseMatrix(matrices.last().pose());
        guiGraphics.drawString(minecraft.font, Text.literal(text).withStyle(FONT), 0, 0, color, true);
        guiGraphics.flush();
        matrices.popPose();
    }

    public float getTextWidth(MutableComponent component) {
        return Minecraft.getInstance().font.width(component.withStyle(FONT));
    }

    public float getTextWidth(String text, float scale) {
        return getTextWidth(Text.literal(text)) * scale;
    }

    public static String[] splitCjk(String text) {
        if (text == null) {
            return new String[]{""};
        }
        String main = text.split("\\|\\|")[0];
        return main.split("\\|", -1);
    }

    // 测量「中文在上、英文在下」两行文字的整体尺寸，返回 {宽, 高}（缺少某行时按单行计算）
    public float[] getTextSizeCjk(String text, float cnScale, float enScale) {
        // 拆分中文与英文行（去除首尾空格）
        String[] parts = splitCjk(text);
        String cn = parts.length > 0 ? parts[0].trim() : "";
        String en = parts.length > 1 ? parts[1].trim() : "";
        // 整体宽度累加
        float width = 0;
        // 整体高度累加
        float height = 0;
        // 有中文行时累加宽度与高度
        if (!cn.isEmpty()) {
            width = Math.max(width, getTextWidth(cn, cnScale));
            height += TEXT_LINE_HEIGHT * cnScale;
        }
        // 有英文行时累加宽度与高度，并与中文行之间留行间距
        if (!en.isEmpty()) {
            width = Math.max(width, getTextWidth(en, enScale));
            height += TEXT_LINE_HEIGHT * enScale;
            if (!cn.isEmpty()) {
                height += CJK_LINE_GAP;
            }
        }
        // 返回整体宽高
        return new float[]{width, height};
    }

    // 把「中文||English」形式的名称分成两行绘制：中文（大号）在上、英文（小号）在下，
    // (x, y) 为两行文字整体的左边界与垂直中心，color 为 ARGB 颜色
    public void drawTextCjk(PoseStack matrices, String text, float x, float y, float cnScale, float enScale, int color) {
        // 文本为空时直接跳过
        if (text == null || text.isEmpty()) {
            return;
        }
        // 拆分中文与英文行（去除首尾空格）
        String[] parts = splitCjk(text);
        String cn = parts.length > 0 ? parts[0].trim() : "";
        String en = parts.length > 1 ? parts[1].trim() : "";
        // 两行文字整体为空时直接跳过
        if (cn.isEmpty() && en.isEmpty()) {
            return;
        }
        // 整体高度换算成顶边
        // 当前行顶边游标
        float yCursor = y - getTextSizeCjk(text, cnScale, enScale)[1] / 2;
        // 有中文行时绘制并下移一行
        if (!cn.isEmpty()) {
            drawText(matrices, cn, x, yCursor, cnScale, color);
            yCursor += TEXT_LINE_HEIGHT * cnScale + CJK_LINE_GAP;
        }
        // 有英文行时绘制在中文行下方
        if (!en.isEmpty()) {
            drawText(matrices, en, x, yCursor, enScale, color);
        }
    }

    public abstract void beginDrawingCircle(BufferBuilder buffer);

    public abstract void beginDrawingTexture(BufferBuilder buffer, ResourceLocation texture);

    public abstract void finishDrawingCircle();

    public abstract void finishDrawingTexture();

    static class NullRenderUtilities extends RenderUtilities {

        public NullRenderUtilities() {
        }

        public void beginDrawingCircle(BufferBuilder buffer) {
        }

        public void beginDrawingTexture(BufferBuilder buffer, ResourceLocation texture) {
        }

        public void finishDrawingCircle() {
        }

        public void finishDrawingTexture() {
        }
    }
}
