package io.github.tas0dev.mc.store.client.command

import com.mojang.brigadier.arguments.StringArgumentType
import io.github.tas0dev.mc.store.client.config.HudPosition
import io.github.tas0dev.mc.store.client.config.PlayerStoreClientConfig
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.minecraft.text.Text

object HudCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher, _ ->
            dispatcher.register(
                literal("playerstore")
                    .then(
                        literal("hud")
                            .then(
                                argument("pos", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        for (p in HudPosition.entries) {
                                            builder.suggest(p.name.lowercase())
                                        }
                                        builder.buildFuture()
                                    }
                                    .executes { ctx ->
                                        val raw = StringArgumentType.getString(ctx, "pos")
                                        val pos = HudPosition.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                                        if (pos == null) {
                                            ctx.source.sendFeedback(Text.literal("Unknown pos: $raw"))
                                            return@executes 0
                                        }
                                        PlayerStoreClientConfig.setHudPosition(pos)
                                        ctx.source.sendFeedback(Text.literal("HUD position set to ${pos.name.lowercase()}"))
                                        1
                                    },
                            ),
                    ),
            )
        })
    }
}

