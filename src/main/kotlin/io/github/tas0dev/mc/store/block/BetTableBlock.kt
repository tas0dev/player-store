package io.github.tas0dev.mc.store.block

import io.github.tas0dev.mc.store.blockentity.BetPhase
import io.github.tas0dev.mc.store.blockentity.BetTableBlockEntity
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
import net.minecraft.text.ClickEvent


class BetTableBlock(settings: Settings) : BlockWithEntity(settings), BlockEntityProvider {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BetTableBlockEntity {
        return BetTableBlockEntity(pos, state)
    }

    private val shape: VoxelShape = createCuboidShape(
        0.0, 0.0, 0.0,
        16.0, 16.0, 16.0
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

    @Deprecated("Deprecated in Java")
    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult,
    ): ActionResult {
        if (world.isClient) return ActionResult.SUCCESS

        val be = world.getBlockEntity(pos) as? BetTableBlockEntity ?: return ActionResult.PASS
        val held = player.getStackInHand(hand)
        val server = world.server ?: return ActionResult.SUCCESS

        // IDLE: GMが紙で掛け金設定
        if (be.phase == BetPhase.IDLE) {
            if (held.item != Items.PAPER || !held.hasCustomName()) {
                player.sendMessage(Text.literal("掛け金を書いた紙で右クリックしてください"), false)
                return ActionResult.SUCCESS
            }

            val stake = held.name.string.trim().toIntOrNull()
            if (stake == null || stake <= 0) {
                player.sendMessage(Text.literal("紙の名前は正の整数にしてください"), false)
                return ActionResult.SUCCESS
            }

            be.gameMasterUuid = player.uuid
            be.stake = stake
            be.pot = 0
            be.participants.clear()

            if (!SilverBalances.tryTake(server, player.uuid, stake)) {
                player.sendMessage(Text.literal("シルバーが足りません"), false)
                return ActionResult.SUCCESS
            }

            be.participants.add(player.uuid)
            be.pot = stake
            be.phase = BetPhase.WAITING

            be.markDirty()
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)

            player.sendMessage(Text.literal("${stake}シルバーの賭けを作成しました。参加待機中です"), false)
            return ActionResult.SUCCESS
        }

        // WAITING: GMが空手で右クリック → ゲーム開始
        if (be.phase == BetPhase.WAITING && be.gameMasterUuid == player.uuid && held.isEmpty) {
            if (be.participants.size < 2) {
                player.sendMessage(Text.literal("参加者が2人以上必要です"), false)
                return ActionResult.SUCCESS
            }

            be.phase = BetPhase.PLAYING
            be.markDirty()
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)

            player.sendMessage(Text.literal("ゲームを開始しました"), false)
            return ActionResult.SUCCESS
        }

        // WAITING: 参加者が右クリック → 掛け金支払い
        if (be.phase == BetPhase.WAITING) {
            if (be.participants.contains(player.uuid)) {
                player.sendMessage(Text.literal("すでに参加しています"), false)
                return ActionResult.SUCCESS
            }

            val stake = be.stake
            if (!SilverBalances.tryTake(server, player.uuid, stake)) {
                player.sendMessage(Text.literal("シルバーが足りません"), false)
                return ActionResult.SUCCESS
            }

            be.participants.add(player.uuid)
            be.pot += stake

            be.markDirty()
            (world as? ServerWorld)?.chunkManager?.markForUpdate(pos)

            player.sendMessage(Text.literal("${stake}シルバーを賭けました。"), false)
            server.sendMessage(Text.literal("どこかの賭け場で ${player.name.string} が賭けに参加しました。破産か、それとも..."))
            server.sendMessage(Text.literal("現在のポット: ${be.pot}シルバー"))
            return ActionResult.SUCCESS
        }

        // PLAYING: GMが空手で右クリック → 勝者選択表示
        if (be.phase == BetPhase.PLAYING && be.gameMasterUuid == player.uuid && held.isEmpty) {
            player.sendMessage(Text.literal("勝者を選択してください。ポット: ${be.pot}シルバー"), false)

            for (uuid in be.participants) {
                val target = server.playerManager.getPlayer(uuid)
                val name = target?.name?.string ?: uuid.toString()

                val text = Text.literal("[ $name に渡す ]")
                    .styled { style ->
                        style.withClickEvent(
                            ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/bettable_payout ${pos.x} ${pos.y} ${pos.z} $uuid"
                            )
                        )
                    }

                player.sendMessage(text, false)
            }

            return ActionResult.SUCCESS
        }

        player.sendMessage(Text.literal("今は操作できません"), false)
        return ActionResult.SUCCESS
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
