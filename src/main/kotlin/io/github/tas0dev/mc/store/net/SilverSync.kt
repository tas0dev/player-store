package io.github.tas0dev.mc.store.net

import io.github.tas0dev.mc.store.economy.SilverBalances
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import java.util.UUID

object SilverSync {
    fun register() {
        // Send current balance when the player joins.
        ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, _, _ ->
            send(handler.player)
        })
    }

    fun send(server: MinecraftServer, uuid: UUID) {
        val player = server.playerManager.getPlayer(uuid) ?: return
        send(player)
    }

    fun send(player: ServerPlayerEntity) {
        val balance = SilverBalances.get(player.server, player.uuid)
        val buf = PacketByteBufs.create()
        buf.writeVarInt(balance)
        ServerPlayNetworking.send(player, NetConstants.SILVER_SYNC, buf)
    }
}

