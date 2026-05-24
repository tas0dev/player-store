package io.github.tas0dev.mc.store.server.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import io.github.tas0dev.mc.store.economy.SilverBalances
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.command.argument.EntityArgumentType
import java.util.UUID

object SilverCommands {
    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            dispatcher.register(
                literal("silver")
                    .then(
                        literal("get")
                            .executes { ctx ->
                                val player = ctx.source.playerOrThrow
                                showBalance(ctx, player, player.uuid)
                            }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .requires { it.hasPermissionLevel(2) }
                                    .executes { ctx ->
                                        val player = EntityArgumentType.getPlayer(ctx, "player")
                                        showBalance(ctx, player, player.uuid)
                                    },
                            ),
                    )
                    .then(
                        literal("add")
                            .requires { it.hasPermissionLevel(2) }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .then(
                                        argument("amount", IntegerArgumentType.integer())
                                            .executes { ctx ->
                                                val player = EntityArgumentType.getPlayer(ctx, "player")
                                                val amount = IntegerArgumentType.getInteger(ctx, "amount")
                                                SilverBalances.add(ctx.source.server, player.uuid, amount)
                                                val newBalance = SilverBalances.get(ctx.source.server, player.uuid)
                                                ctx.source.sendFeedback(
                                                    { Text.literal("${player.name.string} のシルバー残高を $newBalance にしました") },
                                                    true,
                                                )
                                                1
                                            },
                                    ),
                            ),
                    )
                    .then(
                        literal("set")
                            .requires { it.hasPermissionLevel(2) }
                            .then(
                                argument("player", EntityArgumentType.player())
                                    .then(
                                        argument("amount", IntegerArgumentType.integer(0))
                                            .executes { ctx ->
                                                val player = EntityArgumentType.getPlayer(ctx, "player")
                                                val amount = IntegerArgumentType.getInteger(ctx, "amount")
                                                setBalance(ctx.source, player.uuid, amount)
                                                val newBalance = SilverBalances.get(ctx.source.server, player.uuid)
                                                ctx.source.sendFeedback(
                                                    { Text.literal("${player.name.string} のシルバー残高を $newBalance に設定しました") },
                                                    true,
                                                )
                                                1
                                            },
                                    ),
                            ),
                    ),
            )
        })
    }

    private fun showBalance(
        ctx: CommandContext<ServerCommandSource>,
        viewer: ServerPlayerEntity,
        targetUuid: UUID,
    ): Int {
        val balance = SilverBalances.get(ctx.source.server, targetUuid)
        viewer.sendMessage(Text.literal("シルバー残高: $balance"), false)
        return 1
    }

    private fun setBalance(source: ServerCommandSource, uuid: UUID, value: Int) {
        val current = SilverBalances.get(source.server, uuid)
        SilverBalances.add(source.server, uuid, value - current)
    }
}
