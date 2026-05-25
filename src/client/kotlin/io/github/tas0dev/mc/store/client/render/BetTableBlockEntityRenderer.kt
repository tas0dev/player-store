package io.github.tas0dev.mc.store.client.render

import io.github.tas0dev.mc.store.blockentity.BetTableBlockEntity
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.util.math.RotationAxis

class BetTableBlockEntityRenderer(
    ctx: BlockEntityRendererFactory.Context
) : BlockEntityRenderer<BetTableBlockEntity> {
    private val textRenderer: TextRenderer = ctx.textRenderer

    override fun render(
        entity: BetTableBlockEntity,
        tickDelta: Float,
        matrices: MatrixStack,
        vertexConsumers: VertexConsumerProvider,
        light: Int,
        overlay: Int,
    ) {
        val participantsText = Text.literal("参加: ${entity.participants.size}人")
        val potText = Text.literal("ポット: ${entity.pot} シルバー")

        matrices.push()

        // 机の上
        matrices.translate(0.5, 1.03, 0.5)

        // 水平に寝かせる
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f))

        val scale = 0.0125f
        matrices.scale(-scale, -scale, scale)

        val background = (0.4f * 255).toInt() shl 24

        val w1 = textRenderer.getWidth(participantsText).toFloat()
        textRenderer.draw(
            participantsText,
            -w1 / 2.0f,
            -10.0f,
            0xFFFFFF,
            false,
            matrices.peek().positionMatrix,
            vertexConsumers,
            TextRenderer.TextLayerType.NORMAL,
            background,
            light,
        )

        val w2 = textRenderer.getWidth(potText).toFloat()
        textRenderer.draw(
            potText,
            -w2 / 2.0f,
            2.0f,
            0xFFFFFF,
            false,
            matrices.peek().positionMatrix,
            vertexConsumers,
            TextRenderer.TextLayerType.NORMAL,
            background,
            light,
        )

        matrices.pop()
    }
}