package io.github.tas0dev.mc.store.block

import io.github.tas0dev.mc.store.blockentity.StoreTableBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.BlockRenderType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class StoreTableBlock(settings: Settings) : BlockWithEntity(settings), BlockEntityProvider {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): StoreTableBlockEntity {
        return StoreTableBlockEntity(pos, state)
    }

    override fun getRenderType(state: BlockState): BlockRenderType {
        // BlockWithEntity defaults to INVISIBLE; we want the normal baked model to render.
        return BlockRenderType.MODEL
    }

    override fun onPlaced(
        world: World,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        itemStack: ItemStack,
    ) {
        super.onPlaced(world, pos, state, placer, itemStack)
        if (world.isClient) return
        val player = placer as? PlayerEntity ?: return
        val be = world.getBlockEntity(pos) as? StoreTableBlockEntity ?: return
        be.setOwner(player)
    }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult,
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS

        val be = world.getBlockEntity(pos) as? StoreTableBlockEntity ?: return ActionResult.PASS
        val held = player.getStackInHand(hand)

        // Price setting: owner + named paper with positive integer
        if (held.item == Items.PAPER && held.hasCustomName()) {
            if (!be.isOwner(player)) {
                player.sendMessage(Text.literal("店主以外は価格を変更できません"), false)
                return ActionResult.SUCCESS
            }

            val raw = held.name.string.trim()
            val match = PRICE_REGEX.matchEntire(raw)
            if (match == null) {
                player.sendMessage(Text.literal("数字のみ入力してください"), false)
                return ActionResult.SUCCESS
            }

            val price = raw.toIntOrNull()
            if (price == null || price <= 0) {
                player.sendMessage(Text.literal("数字のみ入力してください"), false)
                return ActionResult.SUCCESS
            }

            be.price = price
            be.markDirty()
            // Ensure the updated BlockEntity NBT reaches clients for rendering.
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
            player.sendMessage(Text.literal("価格を $price シルバーに設定しました"), false)
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    companion object {
        private val PRICE_REGEX = Regex("^[1-9][0-9]*\$")
    }
}
