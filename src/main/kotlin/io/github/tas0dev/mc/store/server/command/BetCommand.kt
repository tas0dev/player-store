package io.github.tas0dev.mc.store.server.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import io.github.tas0dev.mc.store.blockentity.BetPhase
import io.github.tas0dev.mc.store.blockentity.BetTableBlockEntity
import io.github.tas0dev.mc.store.economy.SilverBalances
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import java.util.UUID

object BetCommand {

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerInternal(dispatcher)
        }
    }

    private fun registerInternal(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("bet")
                .requires { it.hasPermissionLevel(4) }
                .then(
                    literal("gm")
                        .then(literal("on").executes { setGM(it.source.playerOrThrow) })
                        .then(literal("off").executes { removeGM(it.source.playerOrThrow) })
                )
        )

        dispatcher.register(
            literal("bettable_payout")
                .then(argument("x", IntegerArgumentType.integer())
                    .then(argument("y", IntegerArgumentType.integer())
                        .then(argument("z", IntegerArgumentType.integer())
                            .then(argument("winner", StringArgumentType.word())
                                .executes { ctx ->
                                    val source = ctx.source
                                    val player = source.playerOrThrow

                                    val x = IntegerArgumentType.getInteger(ctx, "x")
                                    val y = IntegerArgumentType.getInteger(ctx, "y")
                                    val z = IntegerArgumentType.getInteger(ctx, "z")
                                    val winnerUuid = UUID.fromString(
                                        StringArgumentType.getString(ctx, "winner")
                                    )

                                    payout(player, BlockPos(x, y, z), winnerUuid)
                                }
                            )
                        )
                    )
                )
        )
    }

    private fun payout(player: ServerPlayerEntity, pos: BlockPos, winnerUuid: UUID): Int {
        val world = player.serverWorld
        val be = world.getBlockEntity(pos) as? BetTableBlockEntity
            ?: return fail(player, "賭けテーブルが見つかりません")

        if (be.phase != BetPhase.PLAYING) {
            return fail(player, "ゲーム中ではありません")
        }

        if (be.gameMasterUuid != player.uuid) {
            return fail(player, "GMだけが払い出しできます")
        }

        if (!be.participants.contains(winnerUuid)) {
            return fail(player, "そのプレイヤーは参加者ではありません")
        }

        if (be.pot <= 0) {
            return fail(player, "ポットが空です")
        }

        SilverBalances.add(player.server, winnerUuid, be.pot)

        val winner = player.server.playerManager.getPlayer(winnerUuid)
        val winnerName = winner?.name?.string ?: winnerUuid.toString()

        player.sendMessage(Text.literal("${winnerName} に ${be.pot}シルバーを払い出しました"), false)
        winner?.sendMessage(Text.literal("${be.pot}シルバーを獲得しました"), false)

        be.phase = BetPhase.IDLE
        be.gameMasterUuid = null
        be.stake = 0
        be.pot = 0
        be.participants.clear()

        be.markDirty()
        world.chunkManager.markForUpdate(pos)

        return 1
    }

    private fun fail(player: ServerPlayerEntity, message: String): Int {
        player.sendMessage(Text.literal(message), false)
        return 0
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

        player.sendMessage(Text.literal("${count}個の賭けテーブルでGMになりました"), false)
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

        player.sendMessage(Text.literal("${count}個の賭けテーブルのGMを解除しました"), false)
        return count
    }
}