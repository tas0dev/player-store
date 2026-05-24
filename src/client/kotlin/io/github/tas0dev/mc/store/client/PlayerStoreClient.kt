package io.github.tas0dev.mc.store.client

import io.github.tas0dev.mc.store.registry.ModBlocks
import io.github.tas0dev.mc.store.registry.ModBlockEntities
import io.github.tas0dev.mc.store.client.render.StoreTableBlockEntityRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.minecraft.client.render.RenderLayer

object PlayerStoreClient : ClientModInitializer {
    override fun onInitializeClient() {
        // Render as solid to ensure the block properly occludes what's behind/below.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STORE_TABLE, RenderLayer.getSolid())

        BlockEntityRendererRegistry.register(ModBlockEntities.STORE_TABLE) { ctx ->
            StoreTableBlockEntityRenderer(ctx)
        }
    }
}
