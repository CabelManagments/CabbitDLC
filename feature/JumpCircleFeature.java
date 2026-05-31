// src/main/java/com/cabbitdlc/feature/JumpCircleFeature.java
package com.cabbitdlc.feature;

import com.cabbitdlc.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.awt.Color;

/**
 * JumpCircle — анимированное кольцо, появляющееся под игроком в момент прыжка.
 *
 * Механика:
 *  - При прыжке (velocity.y > 0 и игрок был на земле) → запускается анимация
 *  - Анимация: два спрайта интерполируются по прогрессу [0..1]
 *    (0 = jump_circle_start.png, 1 = jump_circle_end.png)
 *  - Спрайты белые → перекрашиваются в цвет пользователя через color multiply
 *  - Прогресс масштабируется скоростью animationSpeed (1x..2x)
 *
 * Рендер происходит в HUD (DrawContext), проецируя 2D-круг под ноги игрока
 * через матричные трансформации мирового рендера.
 * В 2D HUD рисуем в центре экрана внизу, симулируя перспективу.
 */
public class JumpCircleFeature {

    private static final Identifier SPRITE_START =
            Identifier.of("cabbitdlc", "textures/jump_circle_start.png");
    private static final Identifier SPRITE_END =
            Identifier.of("cabbitdlc", "textures/jump_circle_end.png");

    // ─── Состояние ────────────────────────────────────────────────────────────
    private boolean enabled = false;
    private String  name    = "JumpCircle";

    // ─── Настройки (изменяются через SettingsPanel) ───────────────────────────
    private float colorHue      = 0.75f;  // фиолетовый по умолчанию
    private int   color         = 0xFF7C5CBF;
    private float animationSpeed = 1.0f;  // 1.0 .. 2.0

    // ─── Анимационное состояние ───────────────────────────────────────────────
    private boolean  animating    = false;
    private float    progress     = 0f;   // 0.0 → 1.0
    private long     animStartMs  = 0L;
    private static final long ANIM_DURATION_BASE_MS = 600L; // при 1x

    // ─── Предыдущее состояние игрока ──────────────────────────────────────────
    private boolean wasOnGround = true;
    private double  prevVelocityY = 0.0;

    public String getName()      { return name; }
    public boolean isEnabled()   { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public float getColorHue()   { return colorHue; }
    public int   getColor()      { return color; }

    public void setColorFromHue(float hue) {
        this.colorHue = hue;
        int rgb = Color.HSBtoRGB(hue, 0.85f, 0.95f);
        this.color = 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    public float getAnimationSpeed()            { return animationSpeed; }
    public void  setAnimationSpeed(float speed) { this.animationSpeed = Math.max(1f, Math.min(2f, speed)); }

    /**
     * Вызывается каждый клиентский тик.
     * Определяет момент прыжка и запускает анимацию.
     */
    public void onTick(MinecraftClient client) {
        if (!enabled) return;
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        boolean onGround = player.isOnGround();
        double  velY     = player.getVelocity().y;

        // Детект прыжка: игрок был на земле, теперь нет, и velocity.y > 0
        if (wasOnGround && !onGround && velY > 0.0) {
            startAnimation();
        }

        wasOnGround  = onGround;
        prevVelocityY = velY;

        // Обновляем прогресс анимации
        if (animating) {
            long durationMs = (long)(ANIM_DURATION_BASE_MS / animationSpeed);
            float elapsed   = (float)(System.currentTimeMillis() - animStartMs) / durationMs;
            progress = Math.min(1f, elapsed);
            if (progress >= 1f) {
                animating = false;
            }
        }
    }

    private void startAnimation() {
        animating   = true;
        progress    = 0f;
        animStartMs = System.currentTimeMillis();
    }

    /**
     * Рендер JumpCircle в HUD.
     *
     * Позиция: центр экрана по X, нижняя треть по Y —
     * визуально выглядит как кольцо под ногами игрока от первого лица.
     *
     * Два спрайта накладываются с alpha:
     *  - sprite_start: alpha = 1 - progress (исчезает)
     *  - sprite_end:   alpha = progress     (появляется)
     * Оба перекрашены в color через RenderSystem.setShaderColor
     */
    public void onHudRender(DrawContext context, float tickDelta) {
        if (!enabled || !animating) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float eased = RenderUtil.easeOutCubic(progress);

        // Размер кольца: расширяется от 64 до 180 пикселей
        float size  = RenderUtil.lerp(64f, 180f, eased);
        float alpha = 1f - eased; // кольцо затухает к концу

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        // Центрируем кольцо (смещаем чуть ниже центра экрана, как под ногами)
        float cx = screenW / 2f;
        float cy = screenH * 0.72f;

        float halfSize = size / 2f;
        int drawX = (int)(cx - halfSize);
        int drawY = (int)(cy - halfSize * 0.35f); // сплющиваем по Y для перспективы

        int drawW = (int)size;
        int drawH = (int)(size * 0.35f);

        // ─── Рисуем sprite_start (начальный кадр, alpha = 1-progress) ────────
        float alphaStart = (1f - eased) * alpha;
        if (alphaStart > 0.01f) {
            int spriteColor = (((int)(alphaStart * 255)) << 24)
                            | (color & 0x00FFFFFF);
            context.setShaderColor(
                    ((color >> 16) & 0xFF) / 255f,
                    ((color >>  8) & 0xFF) / 255f,
                    ((color      ) & 0xFF) / 255f,
                    alphaStart);
            context.drawTexture(
                    net.minecraft.client.render.RenderLayer::getGuiTextured,
                    SPRITE_START,
                    drawX, drawY, 0f, 0f,
                    drawW, drawH, drawW, drawH);
        }

        // ─── Рисуем sprite_end (конечный кадр, alpha = progress) ─────────────
        float alphaEnd = eased * alpha;
        if (alphaEnd > 0.01f) {
            context.setShaderColor(
                    ((color >> 16) & 0xFF) / 255f,
                    ((color >>  8) & 0xFF) / 255f,
                    ((color      ) & 0xFF) / 255f,
                    alphaEnd);
            context.drawTexture(
                    net.minecraft.client.render.RenderLayer::getGuiTextured,
                    SPRITE_END,
                    drawX, drawY, 0f, 0f,
                    drawW, drawH, drawW, drawH);
        }

        // Сбрасываем shader color
        context.setShaderColor(1f, 1f, 1f, 1f);
    }
}
