package io.github.tas0dev.mc.store.client.hud

import io.github.tas0dev.mc.store.client.config.HudPosition
import io.github.tas0dev.mc.store.client.config.PlayerStoreClientConfig
import io.github.tas0dev.mc.store.client.state.ClientSilverState
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object SilverHud {
    fun register() {
        HudRenderCallback.EVENT.register(HudRenderCallback { drawContext, _ ->
            val client = MinecraftClient.getInstance()
            if (client.options.hudHidden) return@HudRenderCallback

            val text = Text.literal("Silver: ${ClientSilverState.balance}")
            val renderer = client.textRenderer
            val w = drawContext.scaledWindowWidth
            val h = drawContext.scaledWindowHeight
            val tw = renderer.getWidth(text)
            val th = renderer.fontHeight
            val pad = 4

            val (x, y) = position(PlayerStoreClientConfig.hudPosition, w, h, tw, th, pad)
            drawContext.drawTextWithShadow(renderer, text, x, y, 0xFFFFFF)
        })
    }

    private fun position(
        pos: HudPosition,
        screenW: Int,
        screenH: Int,
        textW: Int,
        textH: Int,
        pad: Int,
    ): Pair<Int, Int> {
        val x = when (pos) {
            HudPosition.TOP_LEFT, HudPosition.MIDDLE_LEFT, HudPosition.BOTTOM_LEFT -> pad
            HudPosition.TOP_CENTER, HudPosition.MIDDLE_CENTER, HudPosition.BOTTOM_CENTER -> (screenW - textW) / 2
            HudPosition.TOP_RIGHT, HudPosition.MIDDLE_RIGHT, HudPosition.BOTTOM_RIGHT -> screenW - textW - pad
        }
        val y = when (pos) {
            HudPosition.TOP_LEFT, HudPosition.TOP_CENTER, HudPosition.TOP_RIGHT -> pad
            HudPosition.MIDDLE_LEFT, HudPosition.MIDDLE_CENTER, HudPosition.MIDDLE_RIGHT -> (screenH - textH) / 2
            HudPosition.BOTTOM_LEFT, HudPosition.BOTTOM_CENTER, HudPosition.BOTTOM_RIGHT -> screenH - textH - pad
        }
        return x to y
    }
}

