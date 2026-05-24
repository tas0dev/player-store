package io.github.tas0dev.mc.store.client.net

import io.github.tas0dev.mc.store.client.state.ClientSilverState
import io.github.tas0dev.mc.store.net.NetConstants
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object ClientSilverSync {
    fun register() {
        ClientPlayNetworking.registerGlobalReceiver(NetConstants.SILVER_SYNC) { client, _, buf, _ ->
            val balance = buf.readVarInt()
            client.execute {
                ClientSilverState.balance = balance
            }
        }
    }
}

