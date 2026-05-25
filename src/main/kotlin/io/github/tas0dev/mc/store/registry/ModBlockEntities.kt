package io.github.tas0dev.mc.store.registry

import io.github.tas0dev.mc.store.PlayerStore
import io.github.tas0dev.mc.store.blockentity.BetTableBlockEntity
import io.github.tas0dev.mc.store.blockentity.StoreTableBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModBlockEntities {
    lateinit var STORE_TABLE: BlockEntityType<StoreTableBlockEntity>
        private set

    lateinit var BET_TABLE: BlockEntityType<BetTableBlockEntity>
        private set

    fun register() {
        STORE_TABLE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier(PlayerStore.MOD_ID, "store_table"),
            FabricBlockEntityTypeBuilder.create(::StoreTableBlockEntity, ModBlocks.STORE_TABLE).build(),
        )

        BET_TABLE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier(PlayerStore.MOD_ID, "bet_table"),
            FabricBlockEntityTypeBuilder.create(::BetTableBlockEntity, ModBlocks.BET_TABLE).build()
        )
    }
}
