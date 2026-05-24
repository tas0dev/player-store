package io.github.tas0dev.mc.store.server

import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import java.util.WeakHashMap

object LoadedStoreTableTracker {
    private val loaded: MutableMap<ServerWorld, MutableSet<BlockPos>> = WeakHashMap()

    fun register(world: ServerWorld, pos: BlockPos) {
        loaded.getOrPut(world) { HashSet() }.add(pos.toImmutable())
    }

    fun unregister(world: ServerWorld, pos: BlockPos) {
        loaded[world]?.remove(pos)
    }

    fun positions(world: ServerWorld): Set<BlockPos> {
        return loaded[world]?.toSet().orEmpty()
    }
}

