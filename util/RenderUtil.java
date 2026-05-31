// src/main/java/com/cabbitdlc/util/RenderUtil.java
package com.cabbitdlc.util;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

public class RenderUtil {

    /**
     * Рисует прямоугольник с закруглёнными углами.
     * Реализация через набор прямоугольников + заполненные окружности по углам.
     *
     * @param context   DrawContext
     * @param x         левый X
     * @param y         верхний Y
     * @param width     ширина
     * @param height    высота
     * @param radius    радиус скругления
     * @param color     цвет в формате ARGB int
     */
    public static void drawRoundedRect(DrawContext context, float x, float y,
                                       float width, float height,
                                       float radius, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Центральный прямоугольник
        context.fill((int)(x + radius), (int)y,
                     (int)(x + width - radius), (int)(y + height), color);
        // Левый столбец
        context.fill((int)x, (int)(y + radius),
                     (int)(x + radius), (int)(y + height - radius), color);
        // Правый столбец
        context.fill((int)(x + width - radius), (int)(y + radius),
                     (int)(x + width), (int)(y + height - radius), color);

        // Закруглённые углы через сегменты окружности
        drawCircleQuarter(context, x + radius,           y + radius,            radius, 180, a, r, g, b);
        drawCircleQuarter(context, x + width - radius,   y + radius,            radius, 270, a, r, g, b);
        drawCircleQuarter(context, x + width - radius,   y + height - radius,   radius, 0,   a, r, g, b);
        drawCircleQuarter(context, x + radius,           y + height - radius,   radius, 90,  a, r, g, b);
    }

    /**
     * Рисует четверть закрашенного круга (для углов roundedRect).
     *
     * @param context DrawContext
     * @param cx      центр X
     * @param cy      центр Y
     * @param radius  радиус
     * @param startAngle угол начала четверти (0, 90, 180, 270)
     * @param a,r,g,b компоненты цвета
     */
    private static void drawCircleQuarter(DrawContext context,
                                          float cx, float cy, float radius,
                                          float startAngle,
                                          int a, int r, int g, int b) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN,
                VertexFormats.POSITION_COLOR);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        float fa = a / 255f, fr = r / 255f, fg = g / 255f, fb = b / 255f;

        // центр веера
        buffer.vertex(matrix, cx, cy, 0).color(fr, fg, fb, fa);

        int segments = 12; // сегментов на четверть
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(startAngle + (90.0 / segments) * i);
            float vx = cx + (float)(Math.cos(angle) * radius);
            float vy = cy + (float)(Math.sin(angle) * radius);
            buffer.vertex(matrix, vx, vy, 0).color(fr, fg, fb, fa);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    /**
     * Рисует обводку закруглённого прямоугольника (только контур).
     */
    public static void drawRoundedRectOutline(DrawContext context, float x, float y,
                                              float width, float height,
                                              float radius, float lineWidth, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP,
                VertexFormats.POSITION_COLOR);

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        float fa = a / 255f, fr = r / 255f, fg = g / 255f, fb = b / 255f;

        int segments = 12;
        float[] cx = { x + radius, x + width - radius, x + width - radius, x + radius };
        float[] cy = { y + radius, y + radius, y + height - radius, y + height - radius };
        float[] startAngles = { 180f, 270f, 0f, 90f };

        for (int corner = 0; corner < 4; corner++) {
            for (int i = 0; i <= segments; i++) {
                double angle = Math.toRadians(startAngles[corner] + (90.0 / segments) * i);
                float ox = (float) Math.cos(angle);
                float oy = (float) Math.sin(angle);
                // внешняя вершина
                buffer.vertex(matrix,
                        cx[corner] + ox * radius,
                        cy[corner] + oy * radius, 0)
                      .color(fr, fg, fb, fa);
                // внутренняя вершина
                buffer.vertex(matrix,
                        cx[corner] + ox * (radius - lineWidth),
                        cy[corner] + oy * (radius - lineWidth), 0)
                      .color(fr, fg, fb, fa);
            }
        }
        // замыкаем контур обратно к первой вершине
        double angle0 = Math.toRadians(180f);
        buffer.vertex(matrix,
                cx[0] + (float)Math.cos(angle0) * radius,
                cy[0] + (float)Math.sin(angle0) * radius, 0)
              .color(fr, fg, fb, fa);
        buffer.vertex(matrix,
                cx[0] + (float)Math.cos(angle0) * (radius - lineWidth),
                cy[0] + (float)Math.sin(angle0) * (radius - lineWidth), 0)
              .color(fr, fg, fb, fa);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    /** Линейная интерполяция */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Ease-out cubic: плавное замедление в конце */
    public static float easeOutCubic(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    /** Ease-in-out quad */
    public static float easeInOutQuad(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f;
    }

    /**
     * Перемножает цвет спрайта с заданным RGB (color multiply).
     * Возвращает ARGB int с заменённым RGB на target при сохранении альфа-компоненты.
     */
    public static int multiplyColor(int baseArgb, int targetRgb) {
        int a = (baseArgb >> 24) & 0xFF;
        int tr = (targetRgb >> 16) & 0xFF;
        int tg = (targetRgb >> 8) & 0xFF;
        int tb = targetRgb & 0xFF;
        return (a << 24) | (tr << 16) | (tg << 8) | tb;
    }
}
