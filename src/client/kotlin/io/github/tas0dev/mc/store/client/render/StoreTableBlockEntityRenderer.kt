package io.github.tas0dev.mc.store.client.render

import io.github.tas0dev.mc.store.blockentity.StoreTableBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.render.model.json.ModelTransformationMode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.math.RotationAxis

class StoreTableBlockEntityRenderer(ctx: BlockEntityRendererFactory.Context) : BlockEntityRenderer<StoreTableBlockEntity> {
    private val textRenderer: TextRenderer = ctx.textRenderer

    override fun render(
        entity: StoreTableBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int,
    ) {
        val product = entity.sellItem
        if (!product.isEmpty) {
            matrices.push()
            matrices.translate(0.5, 1.02, 0.5)
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45f))
            matrices.scale(0.6f, 0.6f, 0.6f)
            MinecraftClient.getInstance().itemRenderer.renderItem(
                product,
                ModelTransformationMode.FIXED,
                light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                entity.world,
                0,
            )
            matrices.pop()
        }

        val price = entity.price
        val stock = entity.stock
        if (price <= 0 && product.isEmpty) return

        val client = MinecraftClient.getInstance()
        val dispatcher = client.entityRenderDispatcher

        val header: Text? = if (!product.isEmpty) {
            Text.literal("${product.name.string} x$stock")
        } else {
            null
        }
        val priceText: Text? = if (price > 0) Text.literal("$price シルバー") else null

        matrices.push()
        matrices.translate(0.5, 1.5, 0.5)
        matrices.multiply(dispatcher.rotation)

        val scale = 0.02f
        matrices.scale(-scale, -scale, scale)

        val background = (0.4f * 255).toInt() shl 24

        var y = 0.0f
        if (header != null) {
            val w = textRenderer.getWidth(header).toFloat()
            val x = -w / 2.0f
            textRenderer.draw(
                header,
                x,
                y,
                0xFFFFFF,
                false,
                matrices.peek().positionMatrix,
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                background,
                light,
            )
            y += 10.0f
        }

        if (priceText != null) {
            val w = textRenderer.getWidth(priceText).toFloat()
            val x = -w / 2.0f
            textRenderer.draw(
                priceText,
                x,
                y,
                0xFFFFFF,
                false,
                matrices.peek().positionMatrix,
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                background,
                light,
            )
        }
        matrices.pop()
    }
}
