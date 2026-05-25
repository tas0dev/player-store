package io.github.tas0dev.mc.store.block

import io.github.tas0dev.mc.store.blockentity.StoreTableBlockEntity
import io.github.tas0dev.mc.store.economy.SilverBalances
import net.minecraft.block.*
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
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.World


class BetTableBlock(settings: Settings) : BlockWithEntity(settings), BlockEntityProvider {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): StoreTableBlockEntity {
        return StoreTableBlockEntity(pos, state)
    }

    private val shape: VoxelShape? = createCuboidShape(
        -16.0, 0.0, -16.0,
        32.0, 16.0, 32.0
    )

    @Deprecated("Deprecated in Java")
    override fun getOutlineShape(
        state: BlockState?,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape? {
        return shape
    }

    @Deprecated("Deprecated in Java")
    override fun getCollisionShape(
        state: BlockState?,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape? {
        return shape
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
        val sneaking = player.isSneaking

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

        // Product setup / stock: owner only.
        if (be.isOwner(player)) {
            // Sneak + empty hand: clear product.
            if (sneaking && held.isEmpty) {
                returnStockToOwner(world, pos, player, be)
                be.sellItem = ItemStack.EMPTY
                be.stock = 0
                be.markDirty()
                (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
                player.sendMessage(Text.literal("商品設定をクリアしました"), false)
                return ActionResult.SUCCESS
            }

            // Deposit stock if it matches current product.
            if (!held.isEmpty && !be.sellItem.isEmpty && ItemStack.canCombine(held, be.sellItem)) {
                val space = (MAX_STOCK - be.stock).coerceAtLeast(0)
                val add = held.count.coerceAtMost(space)
                if (add <= 0) {
                    player.sendMessage(Text.literal("在庫が上限（$MAX_STOCK 個）です"), false)
                    return ActionResult.SUCCESS
                }
                held.decrement(add)
                be.stock = be.stock + add
                be.markDirty()
                (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
                player.sendMessage(Text.literal("在庫を $add 個追加しました（在庫: ${be.stock}）"), false)
                return ActionResult.SUCCESS
            }

            // Set/replace product (does not consume).
            // about.md: holding the item and right-click registers it as the sell item.
            if (!held.isEmpty && held.item != Items.PAPER) {
                // If there is stock for the previous item, return it to the owner before replacing.
                returnStockToOwner(world, pos, player, be)
                val product = held.copy()
                product.count = 1
                be.sellItem = product
                be.stock = 0
                be.markDirty()
                (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
                player.sendMessage(Text.literal("販売商品を設定しました"), false)
                return ActionResult.SUCCESS
            }
        } else {
            // Purchase: non-owner only (owner can test by not sneaking with empty hand, but we keep it strict).
            if (be.price <= 0) {
                player.sendMessage(Text.literal("価格が設定されていません"), false)
                return ActionResult.SUCCESS
            }
            val product = be.sellItem
            if (product.isEmpty) {
                player.sendMessage(Text.literal("商品が設定されていません"), false)
                return ActionResult.SUCCESS
            }
            if (be.stock <= 0) {
                player.sendMessage(Text.literal("在庫がありません"), false)
                return ActionResult.SUCCESS
            }

            val server = world.server ?: return ActionResult.SUCCESS
            val price = be.price
            if (!SilverBalances.tryTake(server, player.uuid, price)) {
                player.sendMessage(Text.literal("シルバーが足りません"), false)
                return ActionResult.SUCCESS
            }
            be.ownerUuid?.let { ownerUuid ->
                SilverBalances.add(server, ownerUuid, price)
            }

            val toGive = product.copy()
            toGive.count = 1
            val inserted = player.inventory.insertStack(toGive)
            if (!inserted) {
                // refund
                SilverBalances.add(server, player.uuid, price)
                be.ownerUuid?.let { ownerUuid -> SilverBalances.add(server, ownerUuid, -price) }
                player.sendMessage(Text.literal("インベントリに空きがありません"), false)
                return ActionResult.SUCCESS
            }

            be.stock = be.stock - 1
            be.markDirty()
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)
            player.sendMessage(Text.literal("${price} シルバーで購入しました（残り在庫: ${be.stock}）"), false)
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    companion object {
        private val PRICE_REGEX = Regex("^[1-9][0-9]*\$")
        private const val MAX_STOCK = 64

        private fun returnStockToOwner(world: World, pos: BlockPos, owner: PlayerEntity, be: StoreTableBlockEntity) {
            if (world.isClient) return
            val stock = be.stock
            val item = be.sellItem
            if (stock <= 0 || item.isEmpty) return

            val refund = item.copy()
            refund.count = stock.coerceAtMost(MAX_STOCK)
            val inserted = owner.inventory.insertStack(refund)
            if (!inserted && !refund.isEmpty) {
                owner.dropItem(refund, false)
            }
        }
    }
}
