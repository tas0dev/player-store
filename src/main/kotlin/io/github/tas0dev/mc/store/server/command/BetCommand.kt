package io.github.tas0dev.mc.store.server.command

import com.mojang.brigadier.CommandDispatcher
import io.github.tas0dev.mc.store.blockentity.BetPhase
import io.github.tas0dev.mc.store.blockentity.BetTableBlockEntity
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

object BetCommand {

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerInternal(dispatcher)
        }
    }

    private fun registerInternal(
        dispatcher: CommandDispatcher<ServerCommandSource>
    ) {
        dispatcher.register(
            literal("bet")
                .requires { it.hasPermissionLevel(4) }
                .then(
                    literal("gm")
                        .then(
                            literal("on")
                                .executes {
                                    setGM(it.source.playerOrThrow)
                                }
                        )
                        .then(
                            literal("off")
                                .executes {
                                    removeGM(it.source.playerOrThrow)
                                }
                        )
                )
        )
    }

    private fun setGM(player: ServerPlayerEntity): Int {
        val world = player.serverWorld
        var count = 0

        for (be in BetTableBlockEntity.INSTANCES.toList()) {

            if (be.world != world) continue
            if (be.phase == BetPhase.IDLE) continue

            be.gameMasterUuid = player.uuid
            be.markDirty()

            world.chunkManager.markForUpdate(be.pos)

            count++
        }

        player.sendMessage(
            Text.literal("${count}個の賭けテーブルでGMになりました"),
            false
        )

        return count
    }

    private fun removeGM(player: ServerPlayerEntity): Int {
        val world = player.serverWorld
        var count = 0

        for (be in BetTableBlockEntity.INSTANCES.toList()) {

            if (be.world != world) continue
            if (be.gameMasterUuid != player.uuid) continue

            be.gameMasterUuid = null
            be.markDirty()

            world.chunkManager.markForUpdate(be.pos)

            count++
        }

        player.sendMessage(
            Text.literal("${count}個の賭けテーブルのGMを解除しました"),
            false
        )

        return count
    }
}