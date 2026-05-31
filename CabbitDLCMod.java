// src/main/java/com/cabbitdlc/CabbitDLCMod.java
package com.cabbitdlc;

import com.cabbitdlc.feature.JumpCircleFeature;
import com.cabbitdlc.gui.ClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CabbitDLCMod implements ClientModInitializer {

    public static CabbitDLCMod INSTANCE;
    public static KeyBinding openGuiKey;

    private final JumpCircleFeature jumpCircleFeature = new JumpCircleFeature();

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        // Регистрация клавиши Right Shift → открыть ClickGUI
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cabbitdlc.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.cabbitdlc"
        ));

        // Тик-обработчик: открытие GUI + тик фич
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGuiScreen());
                }
            }
            // Тикаем JumpCircle каждый клиентский тик
            jumpCircleFeature.onTick(client);
        });

        // HUD рендер — рисуем JumpCircle поверх игры
        HudRenderCallback.EVENT.register((drawContext, tickDeltaManager) -> {
            float tickDelta = tickDeltaManager.getTickDelta(true);
            jumpCircleFeature.onHudRender(drawContext, tickDelta);
        });
    }

    public JumpCircleFeature getJumpCircleFeature() {
        return jumpCircleFeature;
    }
}
