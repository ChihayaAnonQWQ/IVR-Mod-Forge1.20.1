package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.*;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.mappings.SelectableMapper;
import mtr.mappings.WidgetMapper;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.*;
import net.hulan.ksd.utils.RailDataUtilities;
import net.hulan.ksd.utils.RenderUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; // 引入界面背景填充工具
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class KCRSTMachineRailMap implements WidgetMapper, SelectableMapper, GuiEventListener, KSDGui {

    private int x;
    private int y;
    private int width;
    private int height; // 保存线路图组件的实际高度
    private double scale;
    private double centerX;
    private double centerY;
    private final boolean isLRT;
    private final SingleTicketSystem.TicketType ticketType;
    private final Consumer<KSDStation> onClickedOnDestination;
    private final KSDStation currentStation;
    private final Set<KSDRoute> mainRoutes = new HashSet<>();
    private final Set<KSDRoute> otherRoutes = new HashSet<>();
    private final Set<KSDRoute> lightRailRoutes = new HashSet<>();
    private final Set<KSDStation> mainStations = new HashSet<>();
    private final Set<KSDStation> otherStations = new HashSet<>();
    private final Set<KSDStation> lightRailStations = new HashSet<>();
    private final Map<Long, List<RailMapStation>> layouts = new HashMap<>();
    private final List<StationCircle> stationCircles = new ArrayList<>();
    private final List<InterchangeCapsule> interchangeCapsules = new ArrayList<>();
    private final List<Tuple<Float, Float>> logicalStationCenters = new ArrayList<>();
    private final List<List<Tuple<Float, Float>>> drawingLines = new ArrayList<>();
    private final List<Integer> drawingLineColors = new ArrayList<>();
    private final List<StationLabel> stationLabels = new ArrayList<>();
    private final Map<Long, Integer> mainStationColors = new HashMap<>();
    private boolean lastHovering;
    private long handCursor;

    public KCRSTMachineRailMap(Consumer<KSDStation> onClickedOnDestination,
                               SingleTicketSystem.TicketType ticketType,
                               KSDStation currentStation) {
        this.ticketType = ticketType; // 保存当前线路图类型
        this.onClickedOnDestination = onClickedOnDestination;
        this.currentStation = currentStation;
        Minecraft minecraftClient = Minecraft.getInstance();
        LocalPlayer player = minecraftClient.player;
        if (player == null) {
            centerX = 0.0F;
            centerY = 0.0F;
        } else {
            centerX = player.getX();
            centerY = player.getZ();
        }
        scale = 0.05F;
        isLRT = ticketType.equals(SingleTicketSystem.TicketType.LRT); // 缓存轻铁线路图标记
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        PoseStack matrices = guiGraphics.pose();
        guiGraphics.fill(x, y, x + width, y + height, ARGB_WHITE); // 清除上一帧内容并填充白色背景
        // Flush the batched background before drawing the map through raw vertex buffers.
        guiGraphics.flush();
        buildRenderData();
        stationLabels.clear();
        stationLabels.addAll(buildStationLabels());
        drawRoutes(matrices);
        drawStationsAndNames(matrices, mouseX, mouseY);
    }

    private void drawRoutes(PoseStack matrices) {
        RenderUtilities renderUtilities = RenderUtilities.getInstance();
        for (int i = 0; i < drawingLines.size(); i++) {
            List<Tuple<Float, Float>> points = drawingLines.get(i);
            int color = drawingLineColors.get(i);
            for (int j = 0; j < points.size() - 1; j++) {
                renderUtilities.drawThickLine(matrices, x + points.get(j).getA(), y + points.get(j).getB(),
                        x + points.get(j + 1).getA(), y + points.get(j + 1).getB(), LINE_WIDTH, color);
            }
            // 为每条线路段补上线宽直径的圆形端盖。
            if (points.size() >= 2) { // 仅在线路段至少包含起止点时绘制端盖
                Tuple<Float, Float> start = points.get(0); // 读取折线起点
                Tuple<Float, Float> end = points.get(points.size() - 1); // 读取折线终点
                float endpointRadius = LINE_WIDTH / 2.0F; // 计算端点圆半径
                renderUtilities.drawStationCircle(matrices, x + start.getA(), y + start.getB(),
                        endpointRadius, SEGMENTS, endpointRadius, color); // 绘制起点圆端盖
                renderUtilities.drawStationCircle(matrices, x + end.getA(), y + end.getB(),
                        endpointRadius, SEGMENTS, endpointRadius, color); // 绘制终点圆端盖
            }
            // 用圆点覆盖折线接缝，避免相邻线段之间出现裂缝。
            for (int j = 1; j < points.size() - 1; j++) {
                Tuple<Float, Float> point = points.get(j);
                renderUtilities.drawStationCircle(matrices, x + point.getA(), y + point.getB(),
                        LINE_WIDTH / 2, SEGMENTS, LINE_WIDTH / 2, color);
            }
        }
    }

    private void drawStationsAndNames(PoseStack matrices, int mouseX, int mouseY) {
        RenderUtilities renderUtilities = RenderUtilities.getInstance();
        Tuple<Float, Float> mouse = new Tuple<>((float) mouseX - x, (float) mouseY - y);
        InterchangeCapsule hoveredCapsule = null;
        for (InterchangeCapsule capsule : interchangeCapsules) {
            if (isPointInCapsule(mouse, capsule)) {
                hoveredCapsule = capsule;
                break;
            }
        }

        // 先确定最终颜色，再把每个胶囊完整绘制一次，避免同深度重复叠画产生闪烁和错位。
        for (InterchangeCapsule capsule : interchangeCapsules) {
            int color = capsule == hoveredCapsule ? renderUtilities.lightenColor(capsule.color) : capsule.color;
            drawInterchangeCapsule(matrices, capsule, color);
        }
        // 遍历所有车站圆
        for (StationCircle circle : stationCircles) {
            // 先画白色实心圆作为车站底色
            renderUtilities.drawStationCircle(matrices, (float) x + (float) circle.centerX, (float) y + (float) circle.centerY,
                    RADIUS, SEGMENTS, RADIUS, ARGB_WHITE);
            // 再画一圈线路颜色的圆环表示所属线路
            renderUtilities.drawStationCircle(matrices, (float) x + (float) circle.centerX, (float) y + (float) circle.centerY,
                    RADIUS, SEGMENTS, STATION_RING_THICKNESS, circle.color);
        }
        AtomicBoolean hovered = new AtomicBoolean(false);
        // 检测鼠标是否落在任一车站圆内，若命中则高亮该站的所有圆环
        mouseOnStation(stationCircles, new Tuple<>((float) mouseX - x, (float) mouseY - y), station -> {
            // 标记为悬停状态
            hovered.set(true);
            // 遍历所有车站圆，把与命中站同 id 的圆环提亮
            for (StationCircle circle : stationCircles) {
                // 判断圆是否属于被命中的车站
                if (circle.stationId == station.id) {
                    // 用提亮后的颜色重画该圆的圆环
                    int color = renderUtilities.lightenColor(isLRT ? ARGB_BLACK : circle.color);
                    renderUtilities.drawStationCircle(matrices, (float) x + (float) circle.centerX, (float) y + (float) circle.centerY,
                            RADIUS, SEGMENTS, STATION_RING_THICKNESS, color);
                }
            }
        });
        if (hoveredCapsule != null) {
            hovered.set(true);
        }
        // 根据悬停状态更新鼠标指针（悬停时显示小手）
        updateCursor(hovered.get());
        for (StationLabel label : stationLabels) {
            RenderUtilities.getInstance().drawTextCjk(matrices, label.text,
                    x + label.x, y + label.y, STATION_NAME_SCALE, STATION_EN_SCALE, ARGB_BLACK);
        }
    }

    private List<StationLabel> buildStationLabels() {
        List<StationLabel> labels = new ArrayList<>();
        RenderUtilities renderUtilities = RenderUtilities.getInstance();
        List<LabelBox> placedLabels = new ArrayList<>();
        for (KSDStation station : getRenderableStations()) {
            if (KSDAreaBase.nonNullCorners(station)) {
                // 站名原文（「中文||English」形式）
                String name = station.name;
                // 测量「中文在上、英文在下」两行站名的整体宽高（供避让计算）
                float[] textSize = renderUtilities.getTextSizeCjk(name, STATION_NAME_SCALE, STATION_EN_SCALE);
                // 站名标签整体宽度
                float textWidth = textSize[0];
                // 站名标签整体高度
                float textHeight = textSize[1];
                Tuple<Float, Float> labelAnchor = getVisibleStationCenter(station.id);
                // 优先以避让后的实际圆圈质心作为站名锚点，无可见圆圈时退回世界坐标。
                if (labelAnchor == null) {
                    BlockPos stationCenter = station.getCenter(); // 获取车站选区中心作为文字锚点回退位置
                    Tuple<Double, Double> fallback = worldPosToCords(stationCenter.getX(), stationCenter.getZ()); // 将选区中心转换为线路图坐标
                    labelAnchor = new Tuple<>(fallback.getA().floatValue(), fallback.getB().floatValue());
                }
                if (RailwayData.isBetween(labelAnchor.getA(), 0.0F, width)
                        && RailwayData.isBetween(labelAnchor.getB(), 0.0F, height)) { // 使用组件高度限制站名标签范围
                    float anchorX = labelAnchor.getA();
                    float anchorY = labelAnchor.getB();
                            // 八个候选方位，优先水平两侧，再尝试上下及四个对角方向。
                            float[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
                            // 已记录的最优重叠得分（越小越干净）
                            float bestScore = Float.MAX_VALUE;
                            // 最优方位序号
                            int bestSide = 0;
                            // 依次评估每个方位
                            for (int i = 0; i < sides.length; i++) {
                                float[] position = getLabelPosition(anchorX, anchorY, textWidth, textHeight, sides[i][0], sides[i][1]);
                                float labelX = position[0];
                                float labelY = position[1];
                                // 同时检查所有线路、主/辅助逻辑站点和已经放置的站名。
                                float score = labelOverlapScore(labelX, labelY, textWidth, textHeight, placedLabels);
                                // 无任何重叠时直接选中该方位并结束评估
                                if (score == 0) {
                                    bestSide = i;
                                    break;
                                }
                                // 有重叠时记录重叠最少的方位
                                if (score < bestScore) {
                                    bestScore = score;
                                    bestSide = i;
                                }
                            }
                            // 站名始终绘制：优先无重叠方位，全部重叠时选重叠最少的方位
                            // （y 为两行文字整体的垂直中心，中文在上英文在下）
                            float[] position = getLabelPosition(anchorX, anchorY, textWidth, textHeight,
                                    sides[bestSide][0], sides[bestSide][1]);
                            placedLabels.add(new LabelBox(position[0], position[1] - textHeight / 2,
                                    position[0] + textWidth, position[1] + textHeight / 2));
                            labels.add(new StationLabel(name, position[0], position[1]));
                }
            }
        }
        return labels;
    }

    private void drawInterchangeCapsule(PoseStack matrices, InterchangeCapsule capsule, int color) {
        RenderUtilities renderUtilities = RenderUtilities.getInstance();
        float x1 = x + capsule.start.getA();
        float y1 = y + capsule.start.getB();
        float x2 = x + capsule.end.getA();
        float y2 = y + capsule.end.getB();
        float innerRadius = capsule.radius - STATION_RING_THICKNESS; // 根据胶囊宽度计算内部留白半径

        renderUtilities.drawThickLine(matrices, x1, y1, x2, y2, capsule.radius * 2, color); // 绘制胶囊外层长轴
        renderUtilities.drawStationCircle(matrices, x1, y1, capsule.radius, SEGMENTS, capsule.radius, color); // 绘制胶囊起点外圆
        renderUtilities.drawStationCircle(matrices, x2, y2, capsule.radius, SEGMENTS, capsule.radius, color); // 绘制胶囊终点外圆
        renderUtilities.drawThickLine(matrices, x1, y1, x2, y2, innerRadius * 2, ARGB_WHITE);
        renderUtilities.drawStationCircle(matrices, x1, y1, innerRadius, SEGMENTS, innerRadius, ARGB_WHITE);
        renderUtilities.drawStationCircle(matrices, x2, y2, innerRadius, SEGMENTS, innerRadius, ARGB_WHITE);
    }

    private static boolean isPointInCapsule(Tuple<Float, Float> point, InterchangeCapsule capsule) {
        return pointSegmentDistance(point, capsule.start, capsule.end) <= capsule.radius; // 使用胶囊实际半径判断鼠标命中
    }

    private Tuple<Float, Float> getVisibleStationCenter(long stationId) {
        float sumX = 0;
        float sumY = 0;
        int count = 0;
        for (StationCircle circle : stationCircles) {
            if (circle.stationId == stationId) {
                sumX += (float) circle.centerX;
                sumY += (float) circle.centerY;
                count++;
            }
        }
        for (InterchangeCapsule capsule : interchangeCapsules) {
            if (capsule.stationId == stationId) {
                sumX += capsule.mainCenter.getA();
                sumY += capsule.mainCenter.getB();
                count++;
            }
        }
        return count == 0 ? null : new Tuple<>(sumX / count, sumY / count);
    }

    // 计算某个站名标签的包围盒（左上角 labelX,labelY，宽 textWidth、高 textHeight，垂直居中）与所有线路线段的重叠总长度。
    // 得分越小摆放越干净：0 表示完全无重叠，用于站名避让时选出最优方位。
    private float labelOverlapScore(float labelX, float labelY, float textWidth, float textHeight, List<LabelBox> placedLabels) {
        // 包围盒左边界
        float padding = 2;
        float minX = labelX - padding;
        // 包围盒右边界
        float maxX = labelX + textWidth + padding;
        // 包围盒上边界（文字垂直居中）
        float minY = labelY - textHeight / 2 - padding;
        // 包围盒下边界
        float maxY = labelY + textHeight / 2 + padding;
        // 重叠总长度累加器
        float total = 0;
        // 遍历每一条线路折线
        for (List<Tuple<Float, Float>> points : drawingLines) {
            // 遍历该折线的每一段
            for (int i = 0; i < points.size() - 1; i++) {
                // 段起点
                Tuple<Float, Float> a = points.get(i);
                // 段终点
                Tuple<Float, Float> b = points.get(i + 1);
                // 累加该线段落在包围盒内的长度
                total += clippedSegmentLength(a.getA(), a.getB(), b.getA(), b.getB(),
                        minX - LINE_WIDTH / 2, minY - LINE_WIDTH / 2, maxX + LINE_WIDTH / 2, maxY + LINE_WIDTH / 2) * 10;
            }
        }
        for (InterchangeCapsule capsule : interchangeCapsules) {
            total += clippedSegmentLength(capsule.start.getA(), capsule.start.getB(), capsule.end.getA(), capsule.end.getB(),
                    minX - capsule.radius, minY - capsule.radius, maxX + capsule.radius, maxY + capsule.radius) * 100; // 按胶囊实际半宽评估站名重叠
        }
        // 辅助站虽然不画圆，也必须为其线路端点保留空间。
        for (Tuple<Float, Float> center : logicalStationCenters) {
            double closestX = Mth.clamp(center.getA(), minX, maxX);
            double closestY = Mth.clamp(center.getB(), minY, maxY);
            if (Math.hypot(center.getA() - closestX, center.getB() - closestY) < RADIUS + padding) {
                total += 10000;
            }
        }
        for (LabelBox box : placedLabels) {
            float overlapWidth = Math.max(0, Math.min(maxX, box.maxX) - Math.max(minX, box.minX));
            float overlapHeight = Math.max(0, Math.min(maxY, box.maxY) - Math.max(minY, box.minY));
            total += overlapWidth * overlapHeight * 100;
        }
        // 返回重叠总长度
        return total;
    }

    private static float[] getLabelPosition(float centerX, float centerY, float width, float height, float directionX, float directionY) {
        float gap = RADIUS + RADIUS_PADDING;
        float labelX = directionX > 0 ? centerX + gap : directionX < 0 ? centerX - gap - width : centerX - width / 2;
        float labelY = directionY > 0 ? centerY + gap + height / 2 : directionY < 0 ? centerY - gap - height / 2 : centerY;
        return new float[]{labelX, labelY};
    }

    // 用 Liang-Barsky 线段裁剪算法求线段 (x1,y1)-(x2,y2) 落在矩形 [minX,maxX]x[minY,maxY] 内的长度，
    // 完全在矩形外返回 0，用于衡量站名与线路的重叠程度。
    private static float clippedSegmentLength(double x1, double y1, double x2, double y2, float minX, float minY, float maxX, float maxY) {
        // 线段进入矩形的参数 t 下界，从 0 开始
        double t0 = 0;
        // 线段离开矩形的参数 t 上界，到 1 结束
        double t1 = 1;
        // 线段在 X 方向上的增量
        double dx = x2 - x1;
        // 线段在 Y 方向上的增量
        double dy = y2 - y1;
        // Liang-Barsky 的 p 系数：-dx, dx, -dy, dy 对应四条裁剪边
        double[] p = {-dx, dx, -dy, dy};
        // Liang-Barsky 的 q 系数：起点到四条边界的距离
        double[] q = {x1 - minX, maxX - x1, y1 - minY, maxY - y1};
        // 依次处理四条裁剪边
        for (int i = 0; i < 4; i++) {
            // 该边与线段方向平行（p 接近 0）
            if (Math.abs(p[i]) < 1e-9) {
                // 起点已在该边外侧，线段整体在矩形外
                if (q[i] < 0) {
                    return 0;
                }
            } else {
                // 计算线段与该裁剪边的交点参数
                double r = q[i] / p[i];
                // p < 0 表示从矩形外进入内部，更新下界 t0
                if (p[i] < 0) {
                    // 交点比上界还远，线段已被完全裁剪掉
                    if (r > t1) {
                        return 0;
                    }
                    // 交点推进下界
                    if (r > t0) {
                        t0 = r;
                    }
                } else {
                    // p > 0 表示从矩形内离开外部，更新上界 t1
                    if (r < t0) {
                        return 0;
                    }
                    // 交点回收上界
                    if (r < t1) {
                        t1 = r;
                    }
                }
            }
        }
        // 把裁剪后的参数区间换算成实际线段长度
        return (float) Math.hypot(dx * (t1 - t0), dy * (t1 - t0));
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        centerX -= deltaX / scale;
        centerY -= deltaY / scale;
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            Tuple<Float, Float> mouse = new Tuple<>((float) mouseX - x, (float) mouseY - y);
            for (InterchangeCapsule capsule : interchangeCapsules) {
                if (isPointInCapsule(mouse, capsule)) {
                    KSDStation station = getRenderableStation(capsule.stationId);
                    if (station != null) {
                        updateCursor(false);
                        onClickedOnDestination.accept(station);
                        return true;
                    }
                }
            }
            mouseOnStation(stationCircles, new Tuple<>((float) mouseX - x, (float) mouseY - y), s -> {
                updateCursor(false);
                onClickedOnDestination.accept(s);
            });
            return true;
        } else {
            return false;
        }
    }

    private KSDStation getRenderableStation(long stationId) {
        for (KSDStation station : mainStations) {
            if (station.id == stationId) {
                return station;
            }
        }
        if (isLRT) {
            for (KSDStation station : lightRailStations) {
                if (station.id == stationId) {
                    return station;
                }
            }
        }
        return null;
    }

    private Collection<KSDStation> getRenderableStations() {
        Map<Long, KSDStation> stationsById = new LinkedHashMap<>();
        mainStations.forEach(station -> stationsById.put(station.id, station));
        if (isLRT) {
            lightRailStations.forEach(station -> stationsById.putIfAbsent(station.id, station));
        }
        return stationsById.values();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        double oldScale = this.scale;
        if (oldScale > SCALE_LOWER_LIMIT && amount < (double) 0.0F) {
            this.centerX -= (mouseX - (double) this.x - (double) this.width / (double) 2.0F) / this.scale;
            this.centerY -= (mouseY - (double) this.y - (double) this.height / (double) 2.0F) / this.scale; // 按组件高度修正缩放中心
        }
        this.scale(amount);
        if (oldScale < SCALE_UPPER_LIMIT && amount > (double) 0.0F) {
            this.centerX += (mouseX - (double) this.x - (double) this.width / (double) 2.0F) / this.scale;
            this.centerY += (mouseY - (double) this.y - (double) this.height / (double) 2.0F) / this.scale; // 按组件高度修正缩放中心
        }
        return true;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= (double) this.x
                && mouseY >= (double) this.y
                && mouseX < (double) (this.x + this.width)
                && mouseY < (double) (this.y + this.height); // 使用组件高度判断鼠标是否位于线路图内
    }

    public void setFocused(boolean focused) {
    }

    public boolean isFocused() {
        return false;
    }

    // 重建线路图布局数据：计算各站在屏幕上的圆位置、每条线路的折线顶点列表
    private void buildRenderData() {
        stationCircles.clear();
        interchangeCapsules.clear();
        logicalStationCenters.clear();
        drawingLines.clear();
        drawingLineColors.clear();
        mainStationColors.clear();
        Map<Long, Map<String, Tuple<Float, Float>>> endpointsByStation = new HashMap<>();
        Map<Long, List<Tuple<Float, Float>>> centersByStation = new HashMap<>();
        Map<Long, List<StationRouteEndpoint>> routeEndpointsByStation = new HashMap<>(); // 记录每个车站各条线路的真实端点
        Map<Long, Integer> routeCountsByStation = countRoutesByStation(); // 统计每个车站接入的线路数量
        Map<Long, Integer> mainRouteCountsByStation = countRoutesByStation(mainRoutes);
        Map<Long, Integer> otherRouteCountsByStation = countRoutesByStation(otherRoutes);
        Map<Long, Integer> lightRailRouteCountsByStation = countRoutesByStation(lightRailRoutes);
        Set<Long> specialInterchangeStationIds = new HashSet<>();
        if (isLRT) {
            routeCountsByStation.forEach((stationId, count) -> {
                // 轻铁图中所有多线路车站都使用胶囊站点。
                if (count > 1) {
                    specialInterchangeStationIds.add(stationId);
                }
            });
        } else if (ticketType == SingleTicketSystem.TicketType.KCR) {
            mainRouteCountsByStation.forEach((stationId, count) -> {
                // KCR 图中，主线路与任一辅助线路换乘时使用胶囊站点。
                if (count > 0 && (otherRouteCountsByStation.getOrDefault(stationId, 0) > 0
                        || lightRailRouteCountsByStation.getOrDefault(stationId, 0) > 0)) {
                    specialInterchangeStationIds.add(stationId);
                }
            });
        }
        Set<Long> capsuleStationIds = new HashSet<>(specialInterchangeStationIds);
        routeCountsByStation.forEach((stationId, count) -> {
            if (count > 3) {
                capsuleStationIds.add(stationId);
            }
        });
        Set<String> visibleCircleKeys = new HashSet<>();
        Set<String> drawnPairs = new HashSet<>();

        // 严格按主线路、其他线路、轻轨线路的顺序逐条完成站点与线段布局。
        for (KSDRoute route : getAllRoutes()) {
            List<RailMapStation> railMapStations = layouts.get(route.id);
            if (railMapStations == null || railMapStations.size() < 2) {
                continue;
            }
            String lineKey = RailDataUtilities.getRouteKey(route);
            boolean mainRoute = mainRoutes.contains(route) || isLRT && lightRailRoutes.contains(route);
            List<Tuple<Float, Float>> path = new ArrayList<>();

            // 先为本线路依次放置站点；候选位置只参考此前已经登记的线路和站点。
            for (RailMapStation railMapStation : railMapStations) {
                long stationId = railMapStation.station.id;
                Map<String, Tuple<Float, Float>> endpoints = endpointsByStation.computeIfAbsent(stationId, key -> new HashMap<>());
                Tuple<Float, Float> endpoint = endpoints.get(lineKey);
                String circleKey = stationId + ":" + lineKey; // 生成当前站点与线路的唯一圆圈键
                if (endpoint == null) {
                    Tuple<Float, Float> preferred = getPreferredStationPosition(railMapStation);
                    List<Tuple<Float, Float>> sameStationCenters = centersByStation.get(stationId); // 读取该站已经放置的所有圆心
                    if (mainRoute && (sameStationCenters == null || sameStationCenters.isEmpty())) { // 第一条主线路固定使用选区中心
                        endpoint = preferred; // 保留车站选区中心作为初始圆心
                    } else {
                        endpoint = chooseStationPosition(preferred, stationId, railMapStation.direction, centersByStation);
                    }
                    endpoints.put(lineKey, endpoint);
                    centersByStation.computeIfAbsent(stationId, key -> new ArrayList<>()).add(endpoint);
                    logicalStationCenters.add(endpoint);
                    if (mainRoute) {
                        mainStationColors.putIfAbsent(stationId, getDrawingColor(route));
                    }
                    routeEndpointsByStation.computeIfAbsent(stationId, key -> new ArrayList<>()) // 获取当前车站的线路端点列表
                            .add(new StationRouteEndpoint(lineKey, endpoint, railMapStation.direction, getDrawingColor(route))); // 记录线路的真实连接端点
                }
                path.add(endpoint);

                // 非胶囊站点按原逻辑为每条可见线路分别生成一个圆圈。
                if (!capsuleStationIds.contains(stationId)
                        && getRenderableStation(stationId) != null
                    && visibleCircleKeys.add(circleKey)) {
                    stationCircles.add(new StationCircle(stationId, endpoint.getA(), endpoint.getB(),
                            isLRT ? ARGB_BLACK : getDrawingColor(route)));
                }
            }

            // 本线路站点确定后立即生成线段，供下一条线路的站点和线段避让。
            for (int i = 0; i < path.size() - 1; i++) {
                String pairKey = stationPairKey(railMapStations.get(i).station.id, railMapStations.get(i + 1).station.id);
                if (!drawnPairs.add(lineKey + ":" + pairKey)) {
                    continue;
                }
                Tuple<Float, Float> p0 = path.get(i);
                Tuple<Float, Float> p3 = path.get(i + 1);
                if (dist(p0, p3) < 0.001) {
                    continue;
                }
                RailMapStation startStation = railMapStations.get(i);
                RailMapStation endStation = railMapStations.get(i + 1);
                // Keep the existing station endpoints, but connect them using MTR Web Map's direction-aware path rules.
                List<Tuple<Float, Float>> curve = connectWebMapLine(p0, startStation.direction, p3, endStation.direction);
                drawingLines.add(curve);
                drawingLineColors.add(getDrawingColor(route));
            }
        }

        for (Map.Entry<Long, List<StationRouteEndpoint>> entry : routeEndpointsByStation.entrySet()) { // 遍历每个车站的真实线路端点
            long stationId = entry.getKey(); // 获取当前车站编号
            if (capsuleStationIds.contains(stationId) && getRenderableStation(stationId) != null) {
                int color = getStationMarkerColor(stationId, entry.getValue().get(0).color);
                interchangeCapsules.add(createAxisAlignedCapsule(stationId, entry.getValue(), color)); // 胶囊始终使用水平或竖直长轴
            }
        }
    }

    /** Returns routes in deterministic drawing priority: main network, other network, then light rail. */
    private List<KSDRoute> getAllRoutes() {
        List<KSDRoute> routes = new ArrayList<>();
        WayFinder wayFinder = KSDClientData.DATA_CACHE.wayFinder; // 读取当前客户端的线路关系缓存
        addSortedRoutes(routes, mainRoutes, wayFinder); // 添加主线路并过滤无效数据
        addSortedRoutes(routes, otherRoutes, wayFinder); // 添加其他线路并过滤无效数据
        addSortedRoutes(routes, lightRailRoutes, wayFinder); // 添加轻铁线路并过滤无效数据
        return routes; // 返回按类别和线路标识稳定排序的线路
    }

    /** Adds one route group in stable key/id order so incremental collision decisions remain reproducible. */
    private static void addSortedRoutes(List<KSDRoute> target, Set<KSDRoute> source, WayFinder wayFinder) {
        List<KSDRoute> sorted = new ArrayList<>(source.stream() // 开始遍历当前类别的线路集合
                // WayFinder 与客户端数据包同步，可过滤缺少有效站点索引的线路。
                .filter(route -> wayFinder.routeIdToStationsWithIndex.containsKey(route.id)
                        && wayFinder.routeIdToStationsWithIndex.get(route.id).size() >= 2)
                .toList()); // 收集至少包含两个有效站点的线路
        sorted.sort(Comparator.comparing(RailDataUtilities::getRouteKey).thenComparingLong(route -> route.id)); // 按线路标识和编号排序
        target.addAll(sorted); // 将排序后的线路追加到绘制列表
    }

    private Map<Long, Integer> countRoutesByStation() {
        return countRoutesByStation(getAllRoutes());
    }

    private Map<Long, Integer> countRoutesByStation(Collection<KSDRoute> routes) {
        Map<Long, Set<String>> routeKeysByStation = new HashMap<>(); // 按线路唯一标识记录每个车站接入的线路
        for (KSDRoute route : routes) { // 遍历指定类别中的全部线路
            List<RailMapStation> stations = layouts.get(route.id); // 获取线路对应的布局站点
            if (stations == null) { // 跳过尚未生成布局的线路
                continue; // 继续处理下一条线路
            }
            String lineKey = RailDataUtilities.getRouteKey(route); // 同一逻辑线路的不同 Route 变体共用一个标识
            Set<Long> stationIds = new HashSet<>(); // 记录本线路已经统计过的车站
            for (RailMapStation station : stations) { // 遍历线路上的布局站点
                if (stationIds.add(station.station.id)) { // 确保同一线路在同一车站只计数一次
                    routeKeysByStation.computeIfAbsent(station.station.id, key -> new HashSet<>()).add(lineKey);
                }
            }
        }
        Map<Long, Integer> routeCounts = new HashMap<>(); // 将唯一线路集合转换为绘制分支需要的数量映射
        routeKeysByStation.forEach((stationId, lineKeys) -> routeCounts.put(stationId, lineKeys.size()));
        return routeCounts; // 返回每个车站的线路数量
    }

    /** Converts the station selection center into local map coordinates. */
    private Tuple<Float, Float> getPreferredStationPosition(RailMapStation railMapStation) {
        KSDStation station = railMapStation.station;
        BlockPos pos = station.getCenter(); // 获取车站选区中心方块
        double worldX = pos.getX(); // 使用车站选区中心作为线路圆心的世界 X 坐标
        double worldZ = pos.getZ(); // 使用车站选区中心作为线路圆心的世界 Z 坐标
        Tuple<Double, Double> point = worldPosToCords(worldX, worldZ); // 将世界坐标转换为线路图坐标
        return new Tuple<>(point.getA().floatValue(), point.getB().floatValue()); // 返回浮点线路图坐标
    }

    private static InterchangeCapsule createAxisAlignedCapsule(long stationId,
                                                               List<StationRouteEndpoint> endpoints,
                                                               int color) {
        float minX = Float.MAX_VALUE; // 初始化线路圆心最小 X 坐标
        float maxX = -Float.MAX_VALUE; // 初始化线路圆心最大 X 坐标
        float minY = Float.MAX_VALUE; // 初始化线路圆心最小 Y 坐标
        float maxY = -Float.MAX_VALUE; // 初始化线路圆心最大 Y 坐标
        for (StationRouteEndpoint endpoint : endpoints) { // 遍历该站的每条换乘线路
            minX = Math.min(minX, endpoint.center.getA()); // 更新线路圆心最小 X 坐标
            maxX = Math.max(maxX, endpoint.center.getA()); // 更新线路圆心最大 X 坐标
            minY = Math.min(minY, endpoint.center.getB()); // 更新线路圆心最小 Y 坐标
            maxY = Math.max(maxY, endpoint.center.getB()); // 更新线路圆心最大 Y 坐标
        }
        CapsuleBounds horizontalBounds = createCapsuleBounds(minX, maxX, minY, maxY, endpoints.size(), true); // 计算水平胶囊包络
        CapsuleBounds verticalBounds = createCapsuleBounds(minX, maxX, minY, maxY, endpoints.size(), false); // 计算竖直胶囊包络
        CapsuleBounds bounds = horizontalBounds.area <= verticalBounds.area ? horizontalBounds : verticalBounds; // 选择占用面积更小的轴对齐胶囊
        return new InterchangeCapsule(stationId, bounds.center, bounds.start, bounds.end,
                bounds.radius, bounds.horizontal, color); // 创建覆盖全部线路圆心的轴对齐胶囊
    }

    private static CapsuleBounds createCapsuleBounds(float minX, float maxX, float minY, float maxY,
                                                      int routeCount, boolean horizontal) {
        float axisMin = horizontal ? minX : minY; // 获取胶囊长轴最小坐标
        float axisMax = horizontal ? maxX : maxY; // 获取胶囊长轴最大坐标
        float crossMin = horizontal ? minY : minX; // 获取胶囊短轴最小坐标
        float crossMax = horizontal ? maxY : maxX; // 获取胶囊短轴最大坐标
        float crossSpan = crossMax - crossMin; // 计算线路圆心在短轴方向的跨度
        float radius = Math.max(RADIUS, crossSpan / 2 + RADIUS); // 根据圆心位置计算胶囊半宽
        int crossCapacity = Math.max(1, (int) Math.floor(radius / RADIUS)); // 估算胶囊短轴能容纳的圆圈行数
        int axisCapacity = (int) Math.ceil((double) routeCount / crossCapacity); // 根据线路数量计算长轴所需圆圈列数
        float minimumAxisSpan = Math.max(0, (axisCapacity - 1) * RADIUS * 2); // 根据线路数量计算胶囊最小长轴跨度
        float axisCenter = (axisMin + axisMax) / 2; // 计算线路圆心的长轴中心
        float crossCenter = (crossMin + crossMax) / 2; // 计算线路圆心的短轴中心
        float axisSpan = Math.max(axisMax - axisMin, minimumAxisSpan); // 同时满足位置跨度与线路数量要求
        Tuple<Float, Float> center = horizontal ? new Tuple<>(axisCenter, crossCenter) : new Tuple<>(crossCenter, axisCenter); // 生成胶囊几何中心
        Tuple<Float, Float> start = horizontal ? new Tuple<>(axisCenter - axisSpan / 2, crossCenter)
                : new Tuple<>(crossCenter, axisCenter - axisSpan / 2); // 生成轴对齐胶囊起点
        Tuple<Float, Float> end = horizontal ? new Tuple<>(axisCenter + axisSpan / 2, crossCenter)
                : new Tuple<>(crossCenter, axisCenter + axisSpan / 2); // 生成轴对齐胶囊终点
        double area = (axisSpan + radius * 2) * radius * 2; // 计算胶囊近似包围面积供方向选择
        return new CapsuleBounds(center, start, end, radius, horizontal, area); // 返回胶囊候选包络
    }

    /** Places an interchange circle with axis-aligned grouping before falling back to route-aware offsets. */
    private Tuple<Float, Float> chooseStationPosition(Tuple<Float, Float> preferred, long stationId, int direction,
                                                      Map<Long, List<Tuple<Float, Float>>> centersByStation) {
        List<Tuple<Float, Float>> sameStationCenters = centersByStation.get(stationId);
        if (sameStationCenters == null || sameStationCenters.isEmpty()) {
            return findFreeStationPos(preferred, stationId, direction, centersByStation, null);
        }

        Tuple<Float, Float> best = null; // 保存当前评分最低的候选圆心
        double bestScore = Double.MAX_VALUE; // 初始化候选圆心评分
        double spacing = RADIUS * 2; // 使用圆直径作为同站圆心间距
        int maxRing = Math.max(8, sameStationCenters.size() + 1); // 预留足够的法线搜索层数

        // 换乘圆优先保持共线：根据现有圆组的跨度选择水平或竖直方向，并从两端继续扩展。
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float sumX = 0;
        float sumY = 0;
        for (Tuple<Float, Float> center : sameStationCenters) {
            minX = Math.min(minX, center.getA());
            maxX = Math.max(maxX, center.getA());
            minY = Math.min(minY, center.getB());
            maxY = Math.max(maxY, center.getB());
            sumX += center.getA();
            sumY += center.getB();
        }
        boolean preferHorizontal = maxX - minX >= maxY - minY;
        for (int axisPass = 0; axisPass < 2 && best == null; axisPass++) {
            boolean horizontal = (axisPass == 0) == preferHorizontal;
            float cross = horizontal ? sumY / sameStationCenters.size() : sumX / sameStationCenters.size();
            float axisMin = horizontal ? minX : minY;
            float axisMax = horizontal ? maxX : maxY;
            for (int ring = 1; ring <= maxRing && best == null; ring++) {
                double distance = spacing * ring;
                for (int sign : new int[]{-1, 1}) {
                    float axis = sign < 0 ? (float) (axisMin - distance) : (float) (axisMax + distance);
                    Tuple<Float, Float> candidate = horizontal
                            ? new Tuple<>(axis, cross) : new Tuple<>(cross, axis);
                    if (!checkIsOverlapped(candidate, sameStationCenters, spacing)) {
                        continue;
                    }
                    double score = stationPositionScore(candidate, preferred, stationId, centersByStation, sameStationCenters);
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
        }

        // 共线候选受阻时，仍优先使用水平/竖直轴向的局部位置。
        if (best == null) {
            for (Tuple<Float, Float> center : sameStationCenters) {
                for (int ring = 1; ring <= maxRing; ring++) {
                    double distance = spacing * ring;
                    for (boolean horizontal : new boolean[]{true, false}) {
                        for (int sign : new int[]{-1, 1}) {
                            Tuple<Float, Float> candidate = offsetByAxis(center, distance, horizontal, sign);
                            if (!checkIsOverlapped(candidate, sameStationCenters, spacing)) {
                                continue;
                            }
                            double score = stationPositionScore(candidate, preferred, stationId, centersByStation, sameStationCenters);
                            if (score < bestScore) {
                                bestScore = score;
                                best = candidate;
                            }
                        }
                    }
                }
            }
        }

        // 若轴向候选都不可用，再沿本线路法线两侧寻找位置。
        double directionalBestScore = Double.MAX_VALUE;
        Tuple<Float, Float> directionalBest = null;
        for (Tuple<Float, Float> center : sameStationCenters) {
            for (int ring = 1; ring <= maxRing; ring++) { // 从已有圆心向外逐层搜索相切位置
                for (int sign : new int[]{-1, 1}) {
                    Tuple<Float, Float> candidate = offsetByRouteDirection(center, direction, spacing * ring, true, sign); // 沿线路法线生成候选圆心
                    if (!checkIsOverlapped(candidate, sameStationCenters, spacing)) { // 过滤会造成同站圆圈重叠的候选位置
                        continue; // 跳过不满足相切间距的候选位置
                    }
                    double score = stationPositionScore(candidate, preferred, stationId, centersByStation, sameStationCenters); // 评估候选位置的整体避让代价
                    if (score < directionalBestScore) {
                        directionalBestScore = score;
                        directionalBest = candidate;
                    }
                }
            }
        }
        if (best != null) {
            return best;
        }
        return directionalBest == null
                ? findFreeStationPos(preferred, stationId, direction, centersByStation, sameStationCenters)
                : directionalBest; // 无法相切时回退到通用避让算法
    }

    private static Tuple<Float, Float> offsetByAxis(Tuple<Float, Float> center, double distance,
                                                    boolean horizontal, int sign) {
        return horizontal
                ? new Tuple<>((float) (center.getA() + sign * distance), center.getB())
                : new Tuple<>(center.getA(), (float) (center.getB() + sign * distance));
    }

    private static boolean checkIsOverlapped(Tuple<Float, Float> candidate,
                                             List<Tuple<Float, Float>> sameStationCenters,
                                             double spacing) {
        for (Tuple<Float, Float> center : sameStationCenters) { // 检查候选圆心与同站已有圆心的距离
            if (dist(candidate, center) < spacing - 0.001) { // 判断两个圆圈是否会发生重叠
                return false; // 拒绝小于直径的候选间距
            }
        }
        return true; // 候选位置满足同站圆圈不重叠要求
    }

    /** Finds a free location, preferring route-normal movement over movement along the route. */
    private Tuple<Float, Float> findFreeStationPos(Tuple<Float, Float> preferred, long stationId, int direction,
                                                   Map<Long, List<Tuple<Float, Float>>> centersByStation,
                                                   List<Tuple<Float, Float>> sameStationCenters) {
        Tuple<Float, Float> best = preferred;
        double bestScore = stationPositionScore(preferred, preferred, stationId, centersByStation, sameStationCenters);

        // 首选原锚点；冲突时先沿线路法线，再沿线路本身寻找最近空位。
        for (int ring = 1; ring <= 4; ring++) {
            double distance = ring * (RADIUS * 2 + 2);
            for (boolean normal : new boolean[]{true, false}) {
                for (int sign : new int[]{-1, 1}) {
                    Tuple<Float, Float> candidate = offsetByRouteDirection(preferred, direction, distance, normal, sign);
                    double score = stationPositionScore(candidate, preferred, stationId, centersByStation, sameStationCenters)
                            + (normal ? 0 : distance * 10);
                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }
            }
            if (bestScore < 1) {
                break;
            }
        }
        return best;
    }

    /** Produces a local point offset along a route or its perpendicular normal. */
    private static Tuple<Float, Float> offsetByRouteDirection(Tuple<Float, Float> center, int direction,
                                                              double distance, boolean normal, int sign) {
        double[] offset = normal
                ? rotatePoint(0, sign * distance, direction)
                : rotatePoint(sign * distance, 0, direction);
        return new Tuple<>((float) (center.getA() + offset[0]), (float) (center.getB() + offset[1]));
    }

    /** Penalizes movement, station overlap, existing lines and capsules when choosing a station endpoint. */
    private double stationPositionScore(Tuple<Float, Float> candidate, Tuple<Float, Float> preferred, long stationId,
                                        Map<Long, List<Tuple<Float, Float>>> centersByStation,
                                        List<Tuple<Float, Float>> sameStationCenters) {
        double score = dist(candidate, preferred);
        double sameStationSpacing = RADIUS * 2;
        double otherStationSpacing = RADIUS * 2 + 2;

        for (Map.Entry<Long, List<Tuple<Float, Float>>> entry : centersByStation.entrySet()) {
            for (Tuple<Float, Float> center : entry.getValue()) {
                double distance = dist(candidate, center);
                double required = entry.getKey() == stationId ? sameStationSpacing : otherStationSpacing;
                if (distance < required - 0.01) {
                    score += (required - distance) * 10000;
                }
            }
        }

        // 同站候选优先与至少一个既有圆心相切，不在圆组外留下明显缝隙。
        if (sameStationCenters != null && !sameStationCenters.isEmpty()) {
            double nearest = Double.MAX_VALUE;
            for (Tuple<Float, Float> center : sameStationCenters) {
                nearest = Math.min(nearest, dist(candidate, center));
            }
            score += Math.abs(nearest - sameStationSpacing) * 100;
        }

        double lineClearance = RADIUS + LINE_WIDTH / 2 + 2;
        for (List<Tuple<Float, Float>> line : drawingLines) {
            for (int i = 0; i < line.size() - 1; i++) {
                double distance = pointSegmentDistance(candidate, line.get(i), line.get(i + 1));
                if (distance < lineClearance) {
                    score += (lineClearance - distance) * 1000;
                }
            }
        }
        for (InterchangeCapsule capsule : interchangeCapsules) {
            double distance = pointSegmentDistance(candidate, capsule.start, capsule.end);
            if (capsule.stationId != stationId && distance < lineClearance) {
                score += (lineClearance - distance) * 1000;
            }
        }
        return score;
    }

    /** Applies category colors while preserving each main route's configured color. */
    private int getDrawingColor(KSDRoute route) {
        if (lightRailRoutes.contains(route)) {
            return LIGHT_RAIL_COLOR;
        }
        if (otherRoutes.contains(route)) {
            return argb(0x808080);
        }
        return argb(route.color);
    }

    private int getStationMarkerColor(long stationId, int fallbackColor) {
        if (isLRT) {
            return ARGB_BLACK;
        }
        return mainStationColors.getOrDefault(stationId, fallbackColor);
    }

    /**
     * Java port of the MTR Web Map connectLine algorithm. It constructs the connection in the start route's local
     * direction space, then rotates it back, so 0/45/90/135 degree station directions produce consistent bends.
     */
    private static List<Tuple<Float, Float>> connectWebMapLine(Tuple<Float, Float> point1, int direction1,
                                                                Tuple<Float, Float> point2, int direction2) {
        List<Tuple<Float, Float>> segments = new ArrayList<>();
        if (point2.getA() > point1.getA()) {
            connectWebMapLineForward(point1, direction1, point2, direction2, segments);
        } else {
            connectWebMapLineForward(point2, direction2, point1, direction1, segments);
        }
        return segments;
    }

    /** Implements MTR Web Map's connectLine1 using one already-resolved endpoint per route at each station. */
    private static void connectWebMapLineForward(Tuple<Float, Float> point1, int direction1,
                                                 Tuple<Float, Float> point2, int direction2,
                                                 List<Tuple<Float, Float>> segments) {
        double[] localEnd = rotatePoint(point2.getA() - point1.getA(), point2.getB() - point1.getB(), -direction1);
        double x = localEnd[0];
        double y = localEnd[1];
        int signX = x < 0 ? -1 : 1;
        int signY = y < 0 ? -1 : 1;
        double absX = Math.abs(x);
        double absY = Math.abs(y);
        int rotatedDirection = Math.floorMod(direction2 - direction1, 180);
        double halfLineWidth = LINE_WIDTH / 2.0;
        List<double[]> localPoints = new ArrayList<>();
        localPoints.add(new double[]{0, 0});

        if (rotatedDirection == 0) {
            if (absX > absY) {
                double difference = absY / 2.0;
                double endOffset = clampWeb(halfLineWidth, difference / 2.0);
                localPoints.add(new double[]{0, signY * endOffset});
                localPoints.add(new double[]{signX * difference - signX * endOffset, signY * difference});
                localPoints.add(new double[]{x - signX * difference + signX * endOffset, signY * difference});
                localPoints.add(new double[]{x, y - signY * endOffset});
            } else {
                double difference = (absY - absX) / 2.0;
                localPoints.add(new double[]{0, signY * difference});
                localPoints.add(new double[]{x, y - signY * difference});
            }
        } else if (absX > absY) {
            double endOffset = clampWeb(halfLineWidth, absY / 2.0);
            localPoints.add(new double[]{0, signY * endOffset});
            if (rotatedDirection == 90) {
                localPoints.add(new double[]{signX * absY - signX * endOffset, y});
            } else {
                double[] finalPoint = rotatePoint(0, clampWeb(halfLineWidth, absX / 2.0), rotatedDirection);
                int directionSign = direction2 == 45 ? -1 : 1;
                localPoints.add(new double[]{signX * absY - signX * endOffset + directionSign * signY * finalPoint[0], y + finalPoint[1]});
                localPoints.add(new double[]{x + finalPoint[0], y + finalPoint[1]});
            }
        } else if (rotatedDirection == 90) {
            double endOffset = clampWeb(halfLineWidth, absX / 2.0);
            localPoints.add(new double[]{0, y - signY * absX + signX * endOffset});
            localPoints.add(new double[]{x - signX * endOffset, y});
        }

        localPoints.add(new double[]{x, y});
        for (double[] localPoint : localPoints) {
            double[] rotated = rotatePoint(localPoint[0], localPoint[1], direction1);
            addWebMapPoint(segments, point1.getA() + rotated[0], point1.getB() + rotated[1]);
        }
    }

    /** Clamps an offset symmetrically around zero, matching the website's two-argument clamp helper. */
    private static double clampWeb(double value, double bound) {
        return Math.max(-bound, Math.min(value, bound));
    }

    /** Avoids zero-length polyline pieces that otherwise produce visible square artifacts in drawThickLine. */
    private static void addWebMapPoint(List<Tuple<Float, Float>> points, double x, double y) {
        Tuple<Float, Float> point = new Tuple<>((float) x, (float) y);
        if (points.isEmpty() || dist(points.get(points.size() - 1), point) > 0.001) {
            points.add(point);
        }
    }

    /** Returns the shortest distance between a point and a finite line segment. */
    private static double pointSegmentDistance(Tuple<Float, Float> point, Tuple<Float, Float> start, Tuple<Float, Float> end) {
        double dx = end.getA() - start.getA();
        double dy = end.getB() - start.getB();
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 1E-6) {
            return dist(point, start);
        }
        double t = ((point.getA() - start.getA()) * dx + (point.getB() - start.getB()) * dy) / lengthSquared;
        t = Mth.clamp(t, 0, 1);
        double x = start.getA() + t * dx;
        double y = start.getB() + t * dy;
        return Math.hypot(point.getA() - x, point.getB() - y);
    }

    public void setPositionAndSize(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height; // 保存线路图组件高度
    }

    public void scale(double amount) {
        this.scale *= Math.pow(2.0F, amount);
        this.scale = Mth.clamp(this.scale, SCALE_LOWER_LIMIT, SCALE_UPPER_LIMIT);
    }

    private void mouseOnStation(List<StationCircle> circles, Tuple<Float, Float> mouseCord, MouseOnStationCallback callback) {
        for (StationCircle circle : circles) {
            if (dist(new Tuple<>((float) circle.centerX, (float) circle.centerY), mouseCord) <= RADIUS) {
                KSDStation station = getRenderableStation(circle.stationId);
                if (station != null) {
                    callback.mouseOnStation(station);
                }
            }
        }
    }

    void updateCursor(boolean hovering) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (window == 0L) {
            return;
        }
        // 悬停状态
        if (hovering) {
            // 小手光标尚未创建则创建一次并缓存
            if (handCursor == 0L) {
                handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
            }
            // 创建成功则设置小手光标
            if (handCursor != 0L) {
                GLFW.glfwSetCursor(window, handCursor);
            }
        } else if (lastHovering) {
            // 之前悬停而现在离开，恢复默认光标
            GLFW.glfwSetCursor(window, 0L);
        }
        // 记录本次悬停状态供下次判断
        lastHovering = hovering;
    }

    public void load() {
        loadRoutes();
        loadStations();
        loadLayout();
    }

    private void loadRoutes() {
        mainRoutes.clear();
        otherRoutes.clear();
        lightRailRoutes.clear();
        Set<KSDRoute> routesInNetwork = currentStation == null
                ? Set.of() : KSDClientData.DATA_CACHE.wayFinder.getNetwork(currentStation.id);
        Set<KSDRoute> mtr = RailDataUtilities.getMTRRoutes(routesInNetwork);
        Set<KSDRoute> kcr = RailDataUtilities.getKCRRoutes(routesInNetwork);
        Set<KSDRoute> lightRail = RailDataUtilities.getLightRailRoutes(routesInNetwork);
        switch (ticketType) {
            case MTR -> {
                mainRoutes.addAll(mtr);
                otherRoutes.addAll(kcr);
                lightRailRoutes.addAll(lightRail);
            }
            case KCR, LRT -> {
                mainRoutes.addAll(kcr);
                otherRoutes.addAll(mtr);
                lightRailRoutes.addAll(lightRail);
            }
        }
    }

    private void loadStations() {
        mainStations.clear();
        otherStations.clear();
        lightRailStations.clear();
        WayFinder wayFinder = KSDClientData.DATA_CACHE.wayFinder;
        for (KSDRoute route : mainRoutes) {
            mainStations.addAll(wayFinder.routeIdToStationsWithIndex.getOrDefault(route.id, List.of()));
        }
        for (KSDRoute route : otherRoutes) {
            otherStations.addAll(wayFinder.routeIdToStationsWithIndex.getOrDefault(route.id, List.of()));
        }
        for (KSDRoute route : lightRailRoutes) {
            lightRailStations.addAll(wayFinder.routeIdToStationsWithIndex.getOrDefault(route.id, List.of()));
        }
    }

    // 计算线路图布局：每条线路的站点顺序、平台方向、站内分流序号与偏移
    private void loadLayout() {
        // 清空旧布局
        layouts.clear();
        // 每条线路的 RailMapStation 列表
        Map<Long, List<RailMapStation>> railMapStationsByRoute = new HashMap<>();
        // 每个站点、每个方向对应的线路名列表（用于分配分流序号）
        Map<Long, Map<Integer, List<String>>> stationRouteDirections = new HashMap<>();
        // 线路 id → 线路标识
        Map<Long, String> routeLineKeys = new HashMap<>();
        // 三类线路全部使用相同的站点顺序、选区中心、方向和分流判定。
        for (KSDRoute route : getAllRoutes()) {
            // 本线路去重后的站点列表
            List<KSDStation> stationList = new ArrayList<>();
            // 遍历线路的每个站台
            for (Route.RoutePlatform routePlatform : route.platformIds) {
                // 由站台 id 查站点
                KSDStation station = KSDClientData.DATA_CACHE.wayFinder.platformIdToStation.get(routePlatform.platformId); // 通过 WayFinder 索引平台所属车站
                if (KSDAreaBase.nonNullCorners(station) && isStationInRouteGroup(route, station)) {
                    // 连续相同车站跳过（站内多站台或同名同色车站）
                    if (!stationList.isEmpty() && RailDataUtilities.equals(stationList.get(stationList.size() - 1), station)) {
                        continue;
                    }
                    // 加入去重站点列表
                    stationList.add(station);
                }
            }
            // 站点不足两个无法成线
            if (stationList.size() < 2) {
                continue;
            }
            // 线路标识：颜色+中文线路名
            String lineKey = RailDataUtilities.getRouteKey(route);
            // 记录线路标识
            routeLineKeys.put(route.id, lineKey);
            // 构建 RailMapStation 列表
            List<RailMapStation> railMapStations = new ArrayList<>();
            // 为每个站点生成布局数据
            for (int i = 0; i < stationList.size(); i++) {
                // 新建布局站点对象
                RailMapStation railMapStation = new RailMapStation(stationList.get(i));
                // 计算站点处线路的方向（45 度快照）
                railMapStation.direction = stationDirection(stationList, i);
                // 加入线路站点列表
                railMapStations.add(railMapStation);
                // 按站点+方向登记该线路名
                List<String> lineKeys = stationRouteDirections.computeIfAbsent(stationList.get(i).id, key -> new HashMap<>())
                        .computeIfAbsent(railMapStation.direction, key -> new ArrayList<>());
                // 避免重复登记
                if (!lineKeys.contains(lineKey)) {
                    lineKeys.add(lineKey);
                }
            }
            // 保存该线路的布局站点列表
            railMapStationsByRoute.put(route.id, railMapStations);
        }
        // 为每条线路的每个站点分配分流序号（同站同方向的线路按线路名排序）
        for (Map.Entry<Long, List<RailMapStation>> entry : railMapStationsByRoute.entrySet()) {
            // 线路 id
            long routeId = entry.getKey();
            // 遍历该线路的布局站点
            for (RailMapStation railMapStation : entry.getValue()) {
                // 取该站该方向登记的所有线路名
                List<String> lineKeys = stationRouteDirections.get(railMapStation.station.id).get(railMapStation.direction);
                // 按线路名排序，保证序号稳定
                lineKeys.sort(String::compareTo);
                // 本线路在排序中的位置即分流序号
                railMapStation.offsetIndex = lineKeys.indexOf(routeLineKeys.get(routeId));
                // 同方向线路总数
                railMapStation.routeCount = lineKeys.size();
            }
        }
        // 写入布局结果供渲染使用
        layouts.putAll(railMapStationsByRoute);
    }

    private Tuple<Double, Double> worldPosToCords(double worldX, double worldZ) {
        double cordsX = (worldX - centerX) * scale + (double) width / (double) 2.0F;
        double cordsY = (worldZ - centerY) * scale + (double) height / (double) 2.0F; // 使用组件高度将世界纵坐标转换为屏幕坐标
        return new Tuple<>(cordsX, cordsY);
    }

    // 计算某站在线路中的走向角度并快照到 45 度倍数（0~135 之间）
    private static int stationDirection(List<KSDStation> stationList, int index) {
        // 前一个站中心，默认空
        BlockPos prev;
        // 后一个站中心，默认空
        BlockPos next;
        // 首站：用第一个与第二个站确定方向
        if (index == 0) {
            prev = stationList.get(0).getCenter();
            next = stationList.get(1).getCenter();
        } else if (index == stationList.size() - 1) {
            // 末站：用倒数第二与最后一个站确定方向
            prev = stationList.get(stationList.size() - 2).getCenter();
            next = stationList.get(stationList.size() - 1).getCenter();
        } else {
            // 中间站：用前后两个站确定方向
            prev = stationList.get(index - 1).getCenter();
            next = stationList.get(index + 1).getCenter();
        }
        // 任一中心缺失时返回 0 度
        if (prev == null || next == null) {
            return 0;
        }
        // 计算前后两站的连线角度（度）
        double angle = Math.toDegrees(Math.atan2(next.getZ() - prev.getZ(), next.getX() - prev.getX()));
        // 归一到 0~180 区间（线路不分正反）
        angle = ((angle % 180) + 180) % 180;
        // 快照到最近的 45 度倍数
        int snapped = (int) Math.round(angle / 45) * 45;
        // 避免出现 180 度，归一到 0~135
        if (snapped >= 180) {
            snapped -= 180;
        }
        // 返回快照后的方向角度
        return snapped;
    }

    private boolean isStationInRouteGroup(KSDRoute route, KSDStation station) {
        if (mainRoutes.contains(route)) {
            return mainStations.contains(station);
        }
        if (otherRoutes.contains(route)) {
            return otherStations.contains(station);
        }
        if (lightRailRoutes.contains(route)) {
            return lightRailStations.contains(station);
        }
        return false;
    }

    // 生成无向的站点对 key（小的在前，大的在后）
    private static String stationPairKey(long id1, long id2) {
        // 返回 "小id,大id"
        return Math.min(id1, id2) + "," + Math.max(id1, id2);
    }

    // 把点 (x,y) 绕原点旋转 direction 度（仅支持 45 度倍数的快照角度）
    private static double[] rotatePoint(double x, double y, int direction) {
        // 把角度归一到 0~359
        int d = ((direction % 360) + 360) % 360;
        // 正弦值，由分支赋值
        double sin;
        // 余弦值，按角度分支计算（含对角线）
        double cos = switch (d) {
            // 90 度：sin=1, cos=0
            case 90 -> {
                sin = 1;
                yield 0;
            }
            // 270 度：sin=-1, cos=0
            case 270 -> {
                sin = -1;
                yield 0;
            }
            // 45 度：两者都是 √0.5
            case 45 -> {
                sin = Math.sqrt(0.5);
                yield Math.sqrt(0.5);
            }
            // 135 度：sin=√0.5, cos=-√0.5
            case 135 -> {
                sin = Math.sqrt(0.5);
                yield -Math.sqrt(0.5);
            }
            // 225 度：sin=-√0.5, cos=-√0.5
            case 225 -> {
                sin = -Math.sqrt(0.5);
                yield -Math.sqrt(0.5);
            }
            // 315 度：sin=-√0.5, cos=√0.5
            case 315 -> {
                sin = -Math.sqrt(0.5);
                yield Math.sqrt(0.5);
            }
            // 180 度：sin=0, cos=-1
            case 180 -> {
                sin = 0;
                yield -1;
            }
            // 默认 0 度：sin=0, cos=1
            default -> {
                sin = 0;
                yield 1;
            }
        };
        // 应用二维旋转公式并返回
        return new double[]{x * cos - y * sin, x * sin + y * cos};
    }

    // 把 RGB 颜色补全为不透明的 ARGB
    private static int argb(int color) {
        // 高位补 0xFF 透明度
        return 0xFF000000 | (color & 0xFFFFFF);
    }

    private static double dist(Tuple<Float, Float> corner1, Tuple<Float, Float> corner2) {
        return Math.sqrt(Math.pow(corner1.getA() - corner2.getA(), 2) + Math.pow(corner1.getB() - corner2.getB(), 2));
    }

    // 线路图布局中的单个站点：记录所属站点与站内分流信息
    private static final class RailMapStation {

        final KSDStation station;
        int direction;
        int offsetIndex;
        int routeCount;

        // 构造器：绑定站点对象
        RailMapStation(KSDStation station) {
            // 保存站点引用
            this.station = station;
        }
    }

    // 已解析的车站圆：站点 id、圆心位置与线路颜色
    private static final class StationCircle {

        final long stationId;
        final double centerX;
        final double centerY;
        final int color;

        // 构造器：记录站点 id、圆心与颜色
        StationCircle(long stationId, double centerX, double centerY, int color) {
            this.stationId = stationId;
            this.centerX = centerX;
            this.centerY = centerY;
            this.color = color;
        }
    }

    private static final class StationRouteEndpoint {

        final String lineKey; // 保存线路唯一标识
        final Tuple<Float, Float> center; // 保存线路在当前车站的连接圆心
        final int direction; // 保存线路在当前车站的折线方向
        final int color; // 保存线路绘制颜色

        StationRouteEndpoint(String lineKey, Tuple<Float, Float> center, int direction, int color) {
            this.lineKey = lineKey; // 保存线路唯一标识
            this.center = center; // 保存线路在当前车站的连接圆心
            this.direction = direction; // 保存线路在当前车站的折线方向
            this.color = color; // 保存线路绘制颜色
        }
    }

    private static final class CapsuleBounds {

        final Tuple<Float, Float> center; // 保存胶囊几何中心
        final Tuple<Float, Float> start; // 保存胶囊长轴起点
        final Tuple<Float, Float> end; // 保存胶囊长轴终点
        final float radius; // 保存胶囊半宽
        final boolean horizontal; // 保存胶囊是否水平
        final double area; // 保存胶囊近似包围面积

        CapsuleBounds(Tuple<Float, Float> center, Tuple<Float, Float> start, Tuple<Float, Float> end,
                      float radius, boolean horizontal, double area) {
            this.center = center; // 保存胶囊几何中心
            this.start = start; // 保存胶囊长轴起点
            this.end = end; // 保存胶囊长轴终点
            this.radius = radius; // 保存胶囊半宽
            this.horizontal = horizontal; // 保存胶囊是否水平
            this.area = area; // 保存胶囊近似包围面积
        }
    }

    private static final class InterchangeCapsule {

        final long stationId; // 保存胶囊所属车站编号
        final Tuple<Float, Float> mainCenter; // 保存胶囊几何中心
        final float radius; // 保存胶囊半宽
        final boolean horizontal; // 保存胶囊是否水平
        final int color; // 保存胶囊绘制颜色
        final Tuple<Float, Float> start; // 保存胶囊长轴起点
        final Tuple<Float, Float> end; // 保存胶囊长轴终点

        InterchangeCapsule(long stationId, Tuple<Float, Float> mainCenter, Tuple<Float, Float> start,
                           Tuple<Float, Float> end, float radius, boolean horizontal, int color) {
            this.stationId = stationId; // 保存胶囊所属车站编号
            this.mainCenter = mainCenter; // 保存胶囊几何中心
            this.start = start; // 保存胶囊长轴起点
            this.end = end; // 保存胶囊长轴终点
            this.radius = radius; // 保存胶囊半宽
            this.horizontal = horizontal; // 保存胶囊是否水平
            this.color = color; // 保存胶囊绘制颜色
        }
    }

    private static final class StationLabel {

        final String text;
        final float x;
        final float y;

        StationLabel(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    private static final class LabelBox {

        final float minX;
        final float minY;
        final float maxX;
        final float maxY;

        LabelBox(float minX, float minY, float maxX, float maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }
    }

    // 鼠标命中车站时的回调接口
    @FunctionalInterface
    public interface MouseOnStationCallback {
        void mouseOnStation(KSDStation terminus);
    }
}
