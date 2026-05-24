package io.github.tas0dev.mc.store.blockentity

import io.github.tas0dev.mc.store.registry.ModBlockEntities
import io.github.tas0dev.mc.store.server.LoadedStoreTableTracker
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

class StoreTableBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.STORE_TABLE, pos, state) {

    var ownerUuid: UUID? = null
        private set
    var ownerName: String? = null
        private set

    private var storedPrice: Int = 0
    private var storedSellItem: ItemStack = ItemStack.EMPTY
    private var storedStock: Int = 0

    var price: Int
        get() = storedPrice
        set(value) {
            storedPrice = value
            markDirty()
        }

    var sellItem: ItemStack
        get() = storedSellItem
        set(value) {
            storedSellItem = value
            markDirty()
        }

    var stock: Int
        get() = storedStock
        set(value) {
            storedStock = value
            markDirty()
        }

    fun setOwner(player: PlayerEntity) {
        if (ownerUuid != null) return
        ownerUuid = player.uuid
        ownerName = player.gameProfile.name
        markDirty()
        (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
    }

    fun forceOwner(uuid: UUID, name: String) {
        ownerUuid = uuid
        ownerName = name
        markDirty()
        (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
    }

    fun clearOwner() {
        ownerUuid = null
        ownerName = null
        markDirty()
        (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
    }

    fun isOwner(player: PlayerEntity): Boolean {
        val uuid = ownerUuid
        if (uuid != null) {
            if (uuid == player.uuid) return true

            // Some servers/environments can present different UUIDs across sessions.
            // If the name matches, treat as the same owner and repair the stored UUID.
            val name = ownerName
            if (name != null && name == player.gameProfile.name) {
                ownerUuid = player.uuid
                markDirty()
                (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
                return true
            }
            return false
        }
        return ownerName != null && ownerName == player.gameProfile.name
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        nbt.putInt(NBT_PRICE, storedPrice)
        nbt.putInt(NBT_STOCK, storedStock)
        if (!storedSellItem.isEmpty) {
            nbt.put(NBT_SELL_ITEM, storedSellItem.writeNbt(NbtCompound()))
        }
        ownerUuid?.let { nbt.putUuid(NBT_OWNER_UUID, it) }
        ownerName?.let { nbt.putString(NBT_OWNER_NAME, it) }
    }

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        // Avoid marking the chunk dirty on load.
        storedPrice = nbt.getInt(NBT_PRICE)
        storedStock = nbt.getInt(NBT_STOCK)
        storedSellItem = if (nbt.contains(NBT_SELL_ITEM)) ItemStack.fromNbt(nbt.getCompound(NBT_SELL_ITEM)) else ItemStack.EMPTY
        ownerUuid = if (nbt.containsUuid(NBT_OWNER_UUID)) nbt.getUuid(NBT_OWNER_UUID) else null
        ownerName = if (nbt.contains(NBT_OWNER_NAME)) nbt.getString(NBT_OWNER_NAME) else null
    }

    override fun toInitialChunkDataNbt(): NbtCompound {
        return createNbt()
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> {
        return BlockEntityUpdateS2CPacket.create(this)
    }

    override fun setWorld(world: World) {
        super.setWorld(world)
        (world as? ServerWorld)?.let { LoadedStoreTableTracker.register(it, pos) }
    }

    override fun markRemoved() {
        val w = world
        if (w is ServerWorld) {
            LoadedStoreTableTracker.unregister(w, pos)
        }
        super.markRemoved()
    }

    companion object {
        private const val NBT_PRICE = "Price"
        private const val NBT_SELL_ITEM = "SellItem"
        private const val NBT_STOCK = "Stock"
        private const val NBT_OWNER_UUID = "OwnerUuid"
        private const val NBT_OWNER_NAME = "OwnerName"
    }
}
