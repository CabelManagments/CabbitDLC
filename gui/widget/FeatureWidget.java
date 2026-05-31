// src/main/java/com/cabbitdlc/gui/widget/FeatureWidget.java
package com.cabbitdlc.gui.widget;

import com.cabbitdlc.feature.JumpCircleFeature;
import com.cabbitdlc.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Виджет одной фичи в ClickGUI.
 *
 * ЛКМ / ПКМ  → переключить фичу on/off
 * Hover       → подсветка строки
 * Middle Click → открыть/закрыть SettingsPanel
 */
public class FeatureWidget extends ClickableWidget {

    // ─── Цвета ────────────────────────────────────────────────────────────────
    private static final int COLOR_ROW_HOVER    = 0x22FFFFFF;
    private static final int COLOR_ROW_ACTIVE   = 0x1A7C5CBF;
    private static final int COLOR_TOGGLE_ON    = 0xFF7C5CBF;
    private static final int COLOR_TOGGLE_OFF   = 0xFF3A3D4A;
    private static final int COLOR_TOGGLE_KNOB  = 0xFFE8E8F0;
    private static final int COLOR_TEXT         = 0xFFE8E8F0;
    private static final int COLOR_TEXT_ENABLED = 0xFFB89FE0;

    private final JumpCircleFeature feature;

    // Анимация переключателя (0.0 = off, 1.0 = on)
    private float toggleAnim = 0f;
    private long  lastToggleTime = 0L;
    private static final long TOGGLE_ANIM_MS = 180L;

    // Hover анимация
    private float hoverAnim = 0f;
    private long  lastHoverTime = 0L;
    private static final long HOVER_ANIM_MS = 120L;
    private boolean wasHovered = false;

    // Открыта ли панель настроек
    private SettingsPanel settingsPanel = null;
    private boolean settingsOpen = false;

    public FeatureWidget(JumpCircleFeature feature, int x, int y, int width, int height) {
        super(x, y, width, height, Text.literal(feature.getName()));
        this.feature = feature;
        this.toggleAnim = feature.isEnabled() ? 1f : 0f;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean hovered = isHovered();

        // ── Обновляем анимацию hover ──────────────────────────────────────────
        if (hovered != wasHovered) {
            lastHoverTime = System.currentTimeMillis();
            wasHovered = hovered;
        }
        float hoverElapsed = (System.currentTimeMillis() - lastHoverTime) / (float) HOVER_ANIM_MS;
        hoverAnim = hovered
                ? Math.min(1f, hoverAnim + hoverElapsed * delta / 20f)
                : Math.max(0f, hoverAnim - hoverElapsed * delta / 20f);
        // Плавный пересчёт через elapsed
        hoverAnim = hovered
                ? Math.min(1f, (float)(System.currentTimeMillis() - lastHoverTime) / HOVER_ANIM_MS)
                : Math.max(0f, 1f - (float)(System.currentTimeMillis() - lastHoverTime) / HOVER_ANIM_MS);

        // ── Обновляем анимацию переключателя ─────────────────────────────────
        float toggleTarget = feature.isEnabled() ? 1f : 0f;
        float toggleElapsed = (System.currentTimeMillis() - lastToggleTime) / (float) TOGGLE_ANIM_MS;
        if (feature.isEnabled()) {
            toggleAnim = Math.min(1f, toggleElapsed);
        } else {
            toggleAnim = Math.max(0f, 1f - toggleElapsed);
        }

        // ── Фон строки ───────────────────────────────────────────────────────
        if (hoverAnim > 0f) {
            int hoverAlpha = (int)(0x22 * RenderUtil.easeOutCubic(hoverAnim));
            RenderUtil.drawRoundedRect(context,
                    getX(), getY(), getWidth(), getHeight(),
                    6, (hoverAlpha << 24) | 0xFFFFFF);
        }
        if (feature.isEnabled()) {
            RenderUtil.drawRoundedRect(context,
                    getX(), getY(), getWidth(), getHeight(),
                    6, COLOR_ROW_ACTIVE);
        }

        // ── Название фичи ────────────────────────────────────────────────────
        int textColor = feature.isEnabled() ? COLOR_TEXT_ENABLED : COLOR_TEXT;
        context.drawText(mc.textRenderer,
                Text.literal(feature.getName()),
                getX() + 8,
                getY() + (getHeight() - mc.textRenderer.fontHeight) / 2,
                textColor, false);

        // ── Тоггл (pill-switch) ───────────────────────────────────────────────
        int toggleWidth  = 28;
        int toggleHeight = 14;
        int toggleX = getX() + getWidth() - toggleWidth - 6;
        int toggleY = getY() + (getHeight() - toggleHeight) / 2;

        // Фон тоггла: интерполируем цвет между off и on
        int toggleBg = lerpColor(COLOR_TOGGLE_OFF, COLOR_TOGGLE_ON,
                RenderUtil.easeInOutQuad(toggleAnim));
        RenderUtil.drawRoundedRect(context,
                toggleX, toggleY, toggleWidth, toggleHeight,
                toggleHeight / 2f, toggleBg);

        // Knob тоггла
        int knobSize   = toggleHeight - 4;
        float knobMinX = toggleX + 2;
        float knobMaxX = toggleX + toggleWidth - knobSize - 2;
        float knobX    = RenderUtil.lerp(knobMinX, knobMaxX,
                RenderUtil.easeInOutQuad(toggleAnim));
        int knobY = toggleY + 2;
        RenderUtil.drawRoundedRect(context,
                knobX, knobY, knobSize, knobSize,
                knobSize / 2f, COLOR_TOGGLE_KNOB);

        // ── Рендер SettingsPanel если открыта ─────────────────────────────────
        if (settingsOpen && settingsPanel != null) {
            settingsPanel.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // ЛКМ → переключить
        toggleFeature();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            // Клик вне виджета: закрываем settings если открыты и клик не по ней
            if (settingsOpen && settingsPanel != null) {
                if (!settingsPanel.isMouseOver(mouseX, mouseY)) {
                    settingsOpen = false;
                }
            }
            return false;
        }
        // ПКМ → тоже переключить
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            toggleFeature();
            return true;
        }
        // Middle click → открыть/закрыть настройки
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            settingsOpen = !settingsOpen;
            if (settingsOpen) {
                settingsPanel = new SettingsPanel(
                        feature,
                        getX(),
                        getY() + getHeight() + 4,
                        getWidth()
                );
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleFeature() {
        feature.setEnabled(!feature.isEnabled());
        lastToggleTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double deltaX, double deltaY) {
        if (settingsOpen && settingsPanel != null) {
            return settingsPanel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    /** Интерполяция двух ARGB цветов */
    private int lerpColor(int c0, int c1, float t) {
        int a0 = (c0 >> 24) & 0xFF, r0 = (c0 >> 16) & 0xFF,
            g0 = (c0 >>  8) & 0xFF, b0 =  c0        & 0xFF;
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF,
            g1 = (c1 >>  8) & 0xFF, b1 =  c1        & 0xFF;
        return (((int)(a0 + (a1-a0)*t)) << 24)
             | (((int)(r0 + (r1-r0)*t)) << 16)
             | (((int)(g0 + (g1-g0)*t)) <<  8)
             |  ((int)(b0 + (b1-b0)*t));
    }

    @Override
    protected void appendClickableNarrations(
            net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        // Нарративная доступность — минимальная реализация
        builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE,
                Text.literal(feature.getName()));
    }
}
