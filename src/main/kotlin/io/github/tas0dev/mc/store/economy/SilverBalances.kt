package io.github.tas0dev.mc.store.economy

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.server.MinecraftServer
import net.minecraft.world.PersistentState
import net.minecraft.world.PersistentStateManager
import net.minecraft.world.World
import java.util.UUID

object SilverBalances {
    private const val KEY = "playerstore_silver_balances"

    fun get(server: MinecraftServer, uuid: UUID): Int {
        return state(server).get(uuid)
    }

    fun add(server: MinecraftServer, uuid: UUID, delta: Int) {
        if (delta == 0) return
        val state = state(server)
        state.set(uuid, (state.get(uuid) + delta).coerceAtLeast(0))
        state.markDirty()
    }

    fun tryTake(server: MinecraftServer, uuid: UUID, amount: Int): Boolean {
        if (amount <= 0) return true
        val state = state(server)
        val current = state.get(uuid)
        if (current < amount) return false
        state.set(uuid, current - amount)
        state.markDirty()
        return true
    }

    private fun state(server: MinecraftServer): SilverBalanceState {
        val overworld = server.getWorld(World.OVERWORLD) ?: error("Overworld not available")
        val manager: PersistentStateManager = overworld.persistentStateManager
        return manager.getOrCreate(
            { nbt -> SilverBalanceState.fromNbt(nbt) },
            { SilverBalanceState() },
            KEY,
        )
    }

    class SilverBalanceState : PersistentState() {
        private val balances: MutableMap<UUID, Int> = HashMap()

        fun get(uuid: UUID): Int = balances[uuid] ?: 0

        fun set(uuid: UUID, value: Int) {
            balances[uuid] = value.coerceAtLeast(0)
        }

        override fun writeNbt(nbt: NbtCompound): NbtCompound {
            val list = NbtList()
            for ((uuid, balance) in balances) {
                val entry = NbtCompound()
                entry.putUuid("Uuid", uuid)
                entry.putInt("Balance", balance)
                list.add(entry)
            }
            nbt.put("Balances", list)
            return nbt
        }

        companion object {
            fun fromNbt(nbt: NbtCompound): SilverBalanceState {
                val state = SilverBalanceState()
                val list = nbt.getList("Balances", NbtElement.COMPOUND_TYPE.toInt())
                for (i in 0 until list.size) {
                    val entry = list.getCompound(i)
                    if (!entry.containsUuid("Uuid")) continue
                    val uuid = entry.getUuid("Uuid")
                    val balance = entry.getInt("Balance")
                    state.set(uuid, balance)
                }
                return state
            }
        }
    }
}
