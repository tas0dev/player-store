package io.github.tas0dev.mc.store.blockentity

import io.github.tas0dev.mc.store.registry.ModBlockEntities
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

enum class BetPhase {
    IDLE,
    WAITING,
    PLAYING
}

class BetTableBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.BET_TABLE, pos, state) {

    var phase: BetPhase = BetPhase.IDLE
    var gameMasterUuid: UUID? = null
    var stake: Int = 0
    var pot: Int = 0
    val participants: MutableSet<UUID> = mutableSetOf()

    companion object {
        val INSTANCES: MutableSet<BetTableBlockEntity> = mutableSetOf()
    }

    override fun setWorld(world: World) {
        super.setWorld(world)
        INSTANCES.add(this)
    }

    override fun markRemoved() {
        INSTANCES.remove(this)
        super.markRemoved()
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)

        nbt.putString("Phase", phase.name)

        gameMasterUuid?.let {
            nbt.putUuid("GameMasterUuid", it)
        }

        nbt.putInt("Stake", stake)
        nbt.putInt("Pot", pot)

        val list = NbtList()
        for (uuid in participants) {
            val entry = NbtCompound()
            entry.putUuid("Uuid", uuid)
            list.add(entry)
        }
        nbt.put("Participants", list)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        phase = try {
            BetPhase.valueOf(nbt.getString("Phase"))
        } catch (_: Exception) {
            BetPhase.IDLE
        }

        gameMasterUuid = if (nbt.containsUuid("GameMasterUuid")) {
            nbt.getUuid("GameMasterUuid")
        } else {
            null
        }

        stake = nbt.getInt("Stake")
        pot = nbt.getInt("Pot")

        participants.clear()

        val list = nbt.getList("Participants", NbtElement.COMPOUND_TYPE.toInt())
        for (i in 0 until list.size) {
            val entry = list.getCompound(i)
            if (entry.containsUuid("Uuid")) {
                participants.add(entry.getUuid("Uuid"))
            }
        }
    }

    fun resetBet() {
        phase = BetPhase.IDLE
        gameMasterUuid = null
        stake = 0
        pot = 0
        participants.clear()
        markDirty()
    }
}