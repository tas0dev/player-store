package io.github.tas0dev.mc.store.client

import io.github.tas0dev.mc.store.registry.ModBlocks
import io.github.tas0dev.mc.store.registry.ModBlockEntities
import io.github.tas0dev.mc.store.client.render.StoreTableBlockEntityRenderer
import io.github.tas0dev.mc.store.client.command.HudCommands
import io.github.tas0dev.mc.store.client.config.PlayerStoreClientConfig
import io.github.tas0dev.mc.store.client.hud.SilverHud
import io.github.tas0dev.mc.store.client.net.ClientSilverSync
import io.github.tas0dev.mc.store.client.render.BetTableBlockEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories

object PlayerStoreClient : ClientModInitializer {
    override fun onInitializeClient() {
        // Render as solid to ensure the block properly occludes what's behind/below.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STORE_TABLE, RenderLayer.getSolid())

        BlockEntityRendererRegistry.register(ModBlockEntities.STORE_TABLE) { ctx ->
            StoreTableBlockEntityRenderer(ctx)
        }

        BlockEntityRendererFactories.register(
            ModBlockEntities.BET_TABLE,
            ::BetTableBlockEntityRenderer
        )

        PlayerStoreClientConfig.load()
        ClientSilverSync.register()
        SilverHud.register()
        HudCommands.register()
    }
}
