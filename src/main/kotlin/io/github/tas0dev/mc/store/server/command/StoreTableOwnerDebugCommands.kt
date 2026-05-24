package io.github.tas0dev.mc.store.server.command

import io.github.tas0dev.mc.store.blockentity.StoreTableBlockEntity
import io.github.tas0dev.mc.store.server.LoadedStoreTableTracker
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object StoreTableOwnerDebugCommands {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            dispatcher.register(
                literal("storetable")
                    .requires { it.hasPermissionLevel(2) }
                    .then(
                        literal("ownerize_all_loaded")
                            .executes { ctx ->
                                val player = ctx.source.playerOrThrow
                                val changed = forEachLoadedStoreTable(ctx.source) { be ->
                                    be.forceOwner(player.uuid, player.gameProfile.name)
                                }
                                ctx.source.sendFeedback({ Text.literal("店テーブル店主を設定しました（対象: $changed）") }, true)
                                changed
                            },
                    )
                    .then(
                        literal("deownerize_all_loaded")
                            .executes { ctx ->
                                val changed = forEachLoadedStoreTable(ctx.source) { be ->
                                    be.clearOwner()
                                }
                                ctx.source.sendFeedback({ Text.literal("店テーブル店主を解除しました（対象: $changed）") }, true)
                                changed
                            },
                    ),
            )
        })
    }

    private inline fun forEachLoadedStoreTable(
        source: ServerCommandSource,
        action: (StoreTableBlockEntity) -> Unit,
    ): Int {
        val server = source.server
        var count = 0
        for (world in server.getWorlds()) {
            for (pos in LoadedStoreTableTracker.positions(world)) {
                val be = world.getBlockEntity(pos) as? StoreTableBlockEntity ?: continue
                action(be)
                count++
            }
        }
        return count
    }
}
