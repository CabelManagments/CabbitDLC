// src/main/java/com/cabbitdlc/gui/ClickGuiScreen.java
package com.cabbitdlc.gui;

import com.cabbitdlc.CabbitDLCMod;
import com.cabbitdlc.feature.JumpCircleFeature;
import com.cabbitdlc.gui.widget.FeatureWidget;
import com.cabbitdlc.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Главный экран ClickGUI CabbitDLC.
 *
 * Внешний вид:
 *  - Полупрозрачный тёмный overlay на весь экран (blur-имитация)
 *  - Центральная панель с закруглёнными углами и стеклянным эффектом
 *  - Заголовок "CabbitDLC" с градиентом
 *  - Список фич в виде FeatureWidget
 *  - Анимация появления: панель "выезжает" снизу с ease-out
 */
public class ClickGuiScreen extends Screen {

    // ─── Размеры панели ───────────────────────────────────────────────────────
    private static final int PANEL_WIDTH  = 280;
    private static final int PANEL_HEIGHT = 320;
    private static final int CORNER_RADIUS = 14;

    // ─── Цвета (ARGB) ─────────────────────────────────────────────────────────
    private static final int COLOR_OVERLAY       = 0x88000000; // overlay на весь экран
    private static final int COLOR_PANEL_BG      = 0xE6111318; // фон панели
    private static final int COLOR_PANEL_BORDER  = 0xFF2A2D3A; // обводка панели
    private static final int COLOR_HEADER_BG     = 0xFF181B24; // фон заголовка
    private static final int COLOR_ACCENT_1      = 0xFF7C5CBF; // фиолетовый акцент (градиент)
    private static final int COLOR_ACCENT_2      = 0xFF4A90D9; // синий акцент
    private static final int COLOR_TEXT_PRIMARY  = 0xFFE8E8F0; // основной текст
    private static final int COLOR_TEXT_MUTED    = 0xFF6B7080; // приглушённый текст

    // ─── Анимация появления ───────────────────────────────────────────────────
    private float openProgress  = 0f; // 0.0 → 1.0
    private long  openStartTime = -1L;
    private static final long OPEN_DURATION_MS = 280L;

    // ─── Позиция панели (вычисляется в init) ──────────────────────────────────
    private int panelX, panelY;

    // ─── Виджеты фич ──────────────────────────────────────────────────────────
    private final List<FeatureWidget> featureWidgets = new ArrayList<>();

    public ClickGuiScreen() {
        super(Text.literal("CabbitDLC"));
    }

    @Override
    protected void init() {
        panelX = (width  - PANEL_WIDTH)  / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        openStartTime = System.currentTimeMillis();
        openProgress  = 0f;

        featureWidgets.clear();

        // ── Создаём виджет для JumpCircle ────────────────────────────────────
        JumpCircleFeature jcf = CabbitDLCMod.INSTANCE.getJumpCircleFeature();
        int featureY = panelY + 58; // отступ под заголовок

        FeatureWidget jcWidget = new FeatureWidget(
                jcf,
                panelX + 12,
                featureY,
                PANEL_WIDTH - 24,
                28
        );
        featureWidgets.add(jcWidget);
        addDrawableChild(jcWidget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // ── Обновляем прогресс анимации появления ────────────────────────────
        if (openStartTime >= 0) {
            long elapsed = System.currentTimeMillis() - openStartTime;
            openProgress = Math.min(1f, (float) elapsed / OPEN_DURATION_MS);
        }
        float easedProgress = RenderUtil.easeOutCubic(openProgress);

        // Смещение панели по Y для анимации выезда снизу
        float animOffsetY = (1f - easedProgress) * 30f;
        float alpha = easedProgress;

        // ── 1. Затемнённый overlay ────────────────────────────────────────────
        int overlayAlpha = (int)(0x88 * alpha);
        context.fill(0, 0, width, height, (overlayAlpha << 24));

        int drawPanelX = panelX;
        int drawPanelY = (int)(panelY + animOffsetY);

        // ── 2. Тень панели ────────────────────────────────────────────────────
        int shadowAlpha = (int)(0x55 * alpha);
        RenderUtil.drawRoundedRect(context,
                drawPanelX + 4, drawPanelY + 6,
                PANEL_WIDTH, PANEL_HEIGHT,
                CORNER_RADIUS, (shadowAlpha << 24));

        // ── 3. Фон панели ────────────────────────────────────────────────────
        int bgAlpha = (int)(0xE6 * alpha);
        RenderUtil.drawRoundedRect(context,
                drawPanelX, drawPanelY,
                PANEL_WIDTH, PANEL_HEIGHT,
                CORNER_RADIUS,
                (bgAlpha << 24) | (COLOR_PANEL_BG & 0x00FFFFFF));

        // ── 4. Обводка панели ────────────────────────────────────────────────
        int borderAlpha = (int)(0xFF * alpha);
        RenderUtil.drawRoundedRectOutline(context,
                drawPanelX, drawPanelY,
                PANEL_WIDTH, PANEL_HEIGHT,
                CORNER_RADIUS, 1f,
                (borderAlpha << 24) | (COLOR_PANEL_BORDER & 0x00FFFFFF));

        // ── 5. Заголовок ─────────────────────────────────────────────────────
        int headerAlpha = (int)(0xFF * alpha);
        RenderUtil.drawRoundedRect(context,
                drawPanelX, drawPanelY,
                PANEL_WIDTH, 46,
                CORNER_RADIUS,
                (headerAlpha << 24) | (COLOR_HEADER_BG & 0x00FFFFFF));

        // Закрываем нижние углы заголовка (плоские)
        context.fill(
                drawPanelX, drawPanelY + 30,
                drawPanelX + PANEL_WIDTH, drawPanelY + 46,
                ((int)(0xFF * alpha) << 24) | (COLOR_HEADER_BG & 0x00FFFFFF));

        // Акцентная линия под заголовком (градиент)
        drawHorizontalGradient(context,
                drawPanelX + CORNER_RADIUS, drawPanelY + 45,
                PANEL_WIDTH - CORNER_RADIUS * 2, 2,
                COLOR_ACCENT_1, COLOR_ACCENT_2, alpha);

        // Текст заголовка
        String title = "CabbitDLC";
        int titleWidth = textRenderer.getWidth(title);
        int titleAlpha = (int)(0xFF * alpha);
        context.drawText(textRenderer,
                Text.literal(title),
                drawPanelX + (PANEL_WIDTH - titleWidth) / 2,
                drawPanelY + 16,
                (titleAlpha << 24) | (COLOR_TEXT_PRIMARY & 0x00FFFFFF),
                false);

        // Версия
        String version = "v1.0.0";
        int versionAlpha = (int)(0x80 * alpha);
        context.drawText(textRenderer,
                Text.literal(version),
                drawPanelX + PANEL_WIDTH - textRenderer.getWidth(version) - 10,
                drawPanelY + 17,
                (versionAlpha << 24) | (COLOR_TEXT_MUTED & 0x00FFFFFF),
                false);

        // ── 6. Категория ─────────────────────────────────────────────────────
        int catAlpha = (int)(0xAA * alpha);
        context.drawText(textRenderer,
                Text.literal("VISUAL"),
                drawPanelX + 14,
                drawPanelY + 52,
                (catAlpha << 24) | (COLOR_TEXT_MUTED & 0x00FFFFFF),
                false);

        // ── 7. Виджеты (со смещением анимации) ───────────────────────────────
        // Временно сдвигаем матрицу, чтобы виджеты ехали вместе с панелью
        context.getMatrices().push();
        context.getMatrices().translate(0, animOffsetY, 0);
        super.render(context, mouseX, (int)(mouseY - animOffsetY), delta);
        context.getMatrices().pop();
    }

    /**
     * Рисует горизонтальный градиент (слева-направо) через context.fill с двумя цветами.
     */
    private void drawHorizontalGradient(DrawContext context,
                                        int x, int y, int width, int height,
                                        int colorLeft, int colorRight, float alpha) {
        int aL = (int)(((colorLeft  >> 24) & 0xFF) * alpha);
        int aR = (int)(((colorRight >> 24) & 0xFF) * alpha);
        // DrawContext.fillGradient: (x1,y1,x2,y2, colorTop, colorBottom) — не то что нам нужно,
        // поэтому бьём на сегменты
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i       / segments;
            float t1 = (float)(i + 1) / segments;
            int c0 = lerpColor(colorLeft, colorRight, t0, alpha);
            int c1 = lerpColor(colorLeft, colorRight, t1, alpha);
            // Для горизонтального: используем fillGradient по вертикали с одинаковым цветом
            // но нам нужен горизонтальный, поэтому рисуем узкие вертикальные полоски
            int sx = x + (int)(t0 * width);
            int ex = x + (int)(t1 * width);
            context.fillGradient(sx, y, ex, y + height, c0, c0);
        }
        // Упрощённый вариант: два прямоугольника с медиа-цветом
        // Для полноценного градиента нужен Tessellator — но fillGradient по вертикали
        // выдаёт нужный результат если повернуть логику:
        // Перезаписываем нормальным горизонтальным градиентом через fillGradient(x,y,w,h,left,right)
        // В 1.21.4 DrawContext.fillGradient принимает colorTop/colorBottom по вертикали,
        // поэтому горизонтальный градиент делаем через сегменты выше — код выше рабочий.
    }

    private int lerpColor(int c0, int c1, float t, float alpha) {
        int a0 = (c0 >> 24) & 0xFF, r0 = (c0 >> 16) & 0xFF,
            g0 = (c0 >>  8) & 0xFF, b0 =  c0        & 0xFF;
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF,
            g1 = (c1 >>  8) & 0xFF, b1 =  c1        & 0xFF;
        int a = (int)((a0 + (a1 - a0) * t) * alpha);
        int r = (int) (r0 + (r1 - r0) * t);
        int g = (int) (g0 + (g1 - g0) * t);
        int b = (int) (b0 + (b1 - b0) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Escape или Right Shift закрывают GUI
        if (keyCode == GLFW.GLFW_KEY_ESCAPE ||
            keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false; // GUI не паузит игру
    }
}
