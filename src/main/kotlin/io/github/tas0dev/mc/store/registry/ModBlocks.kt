package io.github.tas0dev.mc.store.registry

import io.github.tas0dev.mc.store.PlayerStore
import io.github.tas0dev.mc.store.block.BetTableBlock
import io.github.tas0dev.mc.store.block.StoreTableBlock
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModBlocks {
    val STORE_TABLE: Block = StoreTableBlock(
        AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE)
            .strength(2.5f)
            .nonOpaque(),
    )

    val BET_TABLE: Block = BetTableBlock(
        AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE)
        .strength(2.5f * 3)
        .nonOpaque(),
    )

    fun register() {
        registerBlockWithItem("store_table", STORE_TABLE)
        registerBlockWithItem("bet_table", BET_TABLE)
    }

    private fun registerBlockWithItem(path: String, block: Block) {
        val id = Identifier(PlayerStore.MOD_ID, path)
        Registry.register(Registries.BLOCK, id, block)
        val item = Registry.register(
            Registries.ITEM,
            id,
            BlockItem(block, Item.Settings()),
        )

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register { entries ->
            entries.add(item)
        }
    }
}
