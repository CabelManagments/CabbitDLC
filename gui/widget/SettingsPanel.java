// src/main/java/com/cabbitdlc/gui/widget/SettingsPanel.java
package com.cabbitdlc.gui.widget;

import com.cabbitdlc.feature.JumpCircleFeature;
import com.cabbitdlc.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Панель настроек JumpCircle.
 * Открывается по Middle Click на FeatureWidget, выезжает снизу.
 *
 * Содержит:
 *  1. Выбор цвета (цветовая полоска HSB)
 *  2. Слайдер скорости анимации (1x – 2x)
 */
public class SettingsPanel {

    private static final int PADDING     = 10;
    private static final int ROW_HEIGHT  = 22;
    private static final int COLOR_BG    = 0xF0161920;
    private static final int COLOR_BORDER = 0xFF2A2D3A;
    private static final int COLOR_LABEL  = 0xFF9A9DB0;
    private static final int COLOR_TEXT   = 0xFFE8E8F0;
    private static final int CORNER_R     = 8;

    private final JumpCircleFeature feature;

    private final int x, y, width;
    private final int height;

    // ─── Цветовая полоска (Hue bar) ───────────────────────────────────────────
    private final int hueBarX, hueBarY;
    private final int hueBarW = 0; // вычислится в конструкторе
    private final int hueBarH = 10;
    private boolean draggingHue = false;

    // ─── Слайдер скорости ────────────────────────────────────────────────────
    private final int speedBarX, speedBarY;
    private final int speedBarW, speedBarH = 10;
    private boolean draggingSpeed = false;

    // Анимация появления
    private final long openTime;
    private static final long APPEAR_MS = 200L;

    // Внутренние вычисленные поля
    private final int innerW;

    public SettingsPanel(JumpCircleFeature feature, int x, int y, int panelWidth) {
        this.feature = feature;
        this.x       = x;
        this.y       = y;
        this.width   = panelWidth;
        this.innerW  = panelWidth - PADDING * 2;
        this.height  = PADDING + ROW_HEIGHT + 6 + hueBarH + PADDING
                     + ROW_HEIGHT + 6 + hueBarH + PADDING;

        int contentX = x + PADDING;
        this.hueBarX  = contentX;
        this.hueBarY  = y + PADDING + ROW_HEIGHT;

        this.speedBarX = contentX;
        this.speedBarY = y + PADDING + ROW_HEIGHT + 6 + hueBarH + PADDING + ROW_HEIGHT;
        this.speedBarW = innerW;

        this.openTime  = System.currentTimeMillis();
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();

        float progress = Math.min(1f,
                (float)(System.currentTimeMillis() - openTime) / APPEAR_MS);
        float eased   = RenderUtil.easeOutCubic(progress);
        float offsetY = (1f - eased) * 10f;
        int   alpha   = (int)(0xFF * eased);

        int drawY = (int)(y + offsetY);

        // ── Фон панели ───────────────────────────────────────────────────────
        RenderUtil.drawRoundedRect(context,
                x, drawY, width, height, CORNER_R,
                (alpha == 255 ? COLOR_BG : ((alpha << 24) | (COLOR_BG & 0x00FFFFFF))));
        RenderUtil.drawRoundedRectOutline(context,
                x, drawY, width, height, CORNER_R, 1f,
                ((alpha) << 24) | (COLOR_BORDER & 0x00FFFFFF));

        // ── Секция: Цвет ──────────────────────────────────────────────────────
        context.drawText(mc.textRenderer,
                Text.literal("Color"),
                x + PADDING, drawY + PADDING,
                (alpha << 24) | (COLOR_LABEL & 0x00FFFFFF), false);

        // Превью текущего цвета (маленький квадратик)
        int previewColor = (alpha << 24) | (feature.getColor() & 0x00FFFFFF);
        RenderUtil.drawRoundedRect(context,
                x + width - PADDING - 14, drawY + PADDING - 1,
                14, 14, 3, previewColor);

        // Hue bar (радужная полоска)
        int hueDrawY = drawY + PADDING + ROW_HEIGHT;
        drawHueBar(context, x + PADDING, hueDrawY, innerW, hueBarH, alpha);

        // Индикатор выбранного Hue
        float currentHue = feature.getColorHue();
        int   indicatorX = (int)(x + PADDING + currentHue * innerW);
        RenderUtil.drawRoundedRect(context,
                indicatorX - 2, hueDrawY - 2,
                4, hueBarH + 4, 2,
                (alpha << 24) | 0xFFFFFF);

        // ── Секция: Скорость ──────────────────────────────────────────────────
        int speedSectionY = hueDrawY + hueBarH + PADDING;
        context.drawText(mc.textRenderer,
                Text.literal("Speed"),
                x + PADDING, speedSectionY,
                (alpha << 24) | (COLOR_LABEL & 0x00FFFFFF), false);

        // Значение скорости справа
        String speedStr = String.format("%.1fx", feature.getAnimationSpeed());
        context.drawText(mc.textRenderer,
                Text.literal(speedStr),
                x + width - PADDING - mc.textRenderer.getWidth(speedStr),
                speedSectionY,
                (alpha << 24) | (COLOR_TEXT & 0x00FFFFFF), false);

        int speedBarDrawY = speedSectionY + ROW_HEIGHT;

        // Трек слайдера
        RenderUtil.drawRoundedRect(context,
                x + PADDING, speedBarDrawY,
                innerW, hueBarH, hueBarH / 2f,
                ((int)(0x33 * eased) << 24) | 0xFFFFFF);

        // Заполненная часть слайдера
        float speedT = (feature.getAnimationSpeed() - 1f); // 0..1 (1x=0, 2x=1)
        int   fillW  = (int)(speedT * innerW);
        if (fillW > 0) {
            RenderUtil.drawRoundedRect(context,
                    x + PADDING, speedBarDrawY,
                    fillW, hueBarH, hueBarH / 2f,
                    (alpha << 24) | 0x7C5CBF);
        }

        // Ручка слайдера
        int knobX2 = x + PADDING + fillW;
        RenderUtil.drawRoundedRect(context,
                knobX2 - 5, speedBarDrawY - 3,
                10, hueBarH + 6, 5,
                (alpha << 24) | 0xE8E8F0);
    }

    /**
     * Рисует радужную полоску Hue (HSB с S=1, B=1).
     */
    private void drawHueBar(DrawContext context, int x, int y,
                            int width, int height, int alpha) {
        int segments = 36;
        for (int i = 0; i < segments; i++) {
            float h0 = (float) i       / segments;
            float h1 = (float)(i + 1) / segments;
            int c0 = hsbToArgb(h0, 1f, 1f, alpha);
            int c1 = hsbToArgb(h1, 1f, 1f, alpha);
            int sx = x + (int)(h0 * width);
            int ex = x + (int)(h1 * width);
            // Каждый сегмент — однородный цвет (тонкий, выглядит как градиент)
            context.fill(sx, y, ex, y + height, c0);
        }
        // Закругления у краёв hue bar
        RenderUtil.drawRoundedRectOutline(context, x, y, width, height,
                height / 2f, 1f, (alpha << 24) | 0x444444);
    }

    /** Конвертация HSB → ARGB int */
    private int hsbToArgb(float h, float s, float b, int alpha) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, b);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    /** Проверка: находится ли точка (mx, my) в пределах панели */
    public boolean isMouseOver(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height + 10;
    }

    /**
     * Обработка перетаскивания мышью (для слайдеров).
     */
    public boolean mouseDragged(double mouseX, double mouseY,
                                int button, double deltaX, double deltaY) {
        if (button != 0) return false;

        // Hue bar
        if (draggingHue) {
            float hue = (float)((mouseX - (x + PADDING)) / innerW);
            hue = Math.max(0f, Math.min(1f, hue));
            feature.setColorFromHue(hue);
            return true;
        }
        // Speed slider
        if (draggingSpeed) {
            float t = (float)((mouseX - (x + PADDING)) / innerW);
            t = Math.max(0f, Math.min(1f, t));
            feature.setAnimationSpeed(1f + t); // 1.0 .. 2.0
            return true;
        }
        return false;
    }

    /**
     * Начало перетаскивания: определяем, по какому элементу кликнули.
     */
    public boolean mousePressed(double mouseX, double mouseY) {
        // Hue bar
        if (mouseX >= x + PADDING && mouseX <= x + PADDING + innerW
                && mouseY >= hueBarY - 4 && mouseY <= hueBarY + hueBarH + 4) {
            draggingHue  = true;
            draggingSpeed = false;
            float hue = (float)((mouseX - (x + PADDING)) / innerW);
            feature.setColorFromHue(Math.max(0f, Math.min(1f, hue)));
            return true;
        }
        // Speed slider
        if (mouseX >= x + PADDING && mouseX <= x + PADDING + innerW
                && mouseY >= speedBarY - 4 && mouseY <= speedBarY + hueBarH + 4) {
            draggingSpeed = true;
            draggingHue   = false;
            float t = (float)((mouseX - (x + PADDING)) / innerW);
            feature.setAnimationSpeed(1f + Math.max(0f, Math.min(1f, t)));
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        draggingHue   = false;
        draggingSpeed = false;
    }
}
