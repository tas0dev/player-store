package io.github.tas0dev.mc.store.blockentity

import io.github.tas0dev.mc.store.registry.ModBlockEntities
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
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

        nbt.putInt("Pot", pot)

        val list = NbtList()
        for (uuid in participants) {
            list.add(NbtString.of(uuid.toString()))
        }
        nbt.put("Participants", list)
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)

        pot = nbt.getInt("Pot")

        participants.clear()
        val list = nbt.getList("Participants", NbtElement.STRING_TYPE.toInt())
        for (i in list.indices) {
            participants.add(UUID.fromString(list.getString(i)))
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

    override fun toUpdatePacket(): BlockEntityUpdateS2CPacket? =
        BlockEntityUpdateS2CPacket.create(this)

    override fun toInitialChunkDataNbt(): NbtCompound {
        return createNbt()
    }
}