package settingdust.lightmanscurrency.claimshop.claimtrader

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI
import dev.ftb.mods.ftblibrary.math.ChunkDimPos
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI
import dev.ftb.mods.ftbteams.data.TeamManagerImpl
import io.github.lightman314.lightmanscurrency.api.traders.TraderData
import io.github.lightman314.lightmanscurrency.api.traders.blockentity.TraderBlockEntity
import io.github.lightman314.lightmanscurrency.api.traders.blocks.TraderBlockRotatable
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.MapColor
import net.minecraftforge.common.util.FakePlayerFactory
import settingdust.lightmanscurrency.claimshop.ClaimShopForLightmansCurrency
import thedarkcolour.kotlinforforge.forge.ObjectHolderDelegate
import thedarkcolour.kotlinforforge.forge.registerObject
import java.util.*
import kotlin.jvm.optionals.getOrNull

class ClaimTraderBlock(properties: Properties) : TraderBlockRotatable(properties) {
    companion object {
        val CLAIM_TRADER by
            register(
                "claim_trader",
            ) {
                ClaimTraderBlock(
                    Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(5.0f, Float.POSITIVE_INFINITY)
                        .sound(SoundType.METAL))
            }

        private fun register(
            name: String,
            block: () -> ClaimTraderBlock
        ): ObjectHolderDelegate<ClaimTraderBlock> {
            val delegate =
                ClaimShopForLightmansCurrency.Registries.BLOCKS.registerObject(name, block)
            ClaimShopForLightmansCurrency.Registries.ITEMS.registerObject(name) {
                BlockItem(delegate(), Item.Properties())
            }
            return delegate
        }
    }

    override fun setPlacedBy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        player: LivingEntity?,
        stack: ItemStack
    ) {
        super.setPlacedBy(level, pos, state, player, stack)

        if (player !is ServerPlayer) return

        val fakePlayer by lazy {
            FakePlayerFactory.get(level as ServerLevel, ClaimShopForLightmansCurrency.FAKE_PROFILE)
        }

        if (!level.server!!.playerList.isOp(ClaimShopForLightmansCurrency.FAKE_PROFILE))
            level.server!!.playerList.op(ClaimShopForLightmansCurrency.FAKE_PROFILE)

        if (FTBTeamsAPI.api()
            .manager
            .getTeamForPlayerID(ClaimShopForLightmansCurrency.FAKE_PROFILE.id)
            .getOrNull() == null) {
            TeamManagerImpl.INSTANCE.playerLoggedIn(
                fakePlayer,
                ClaimShopForLightmansCurrency.FAKE_PROFILE.id,
                ClaimShopForLightmansCurrency.FAKE_PROFILE.name)
        }

        val blockEntity = ClaimTraderBlockEntity.CLAIM_TRADER.getBlockEntity(level, pos)!!

        val claimedChunk = FTBChunksAPI.api().manager.getChunk(ChunkDimPos(level, pos))
        if ((claimedChunk == null ||
            (claimedChunk.teamData.team.id != player.uuid &&
                claimedChunk.teamData.team.owner != player.uuid))) {
            player.sendSystemMessage(
                ClaimShopForLightmansCurrency.Texts.NOTIFICATION_TRADE_CLAIM_NOT_OWNER.get())
            blockEntity.cancelled = true
        } else {
            claimedChunk.unclaim(player.createCommandSourceStack(), true)

            val result =
                FTBChunksAPI.api()
                    .claimAsPlayer(fakePlayer, level.dimension(), ChunkPos(pos), false)

            if (!result.isSuccess) {
                blockEntity.cancelled = true
                player.sendSystemMessage(result.message)
            }
        }

        blockEntity.chunkOwner = claimedChunk?.teamData?.team?.id

        if (blockEntity.cancelled) {
            blockEntity.flagAsLegitBreak()
            player.addItem(stack)
            level.destroyBlock(pos, false)
        }
    }

    override fun onDestroyedByPlayer(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        willHarvest: Boolean,
        fluid: FluidState
    ): Boolean {
        if (level is ServerLevel) {
            FTBChunksAPI.api()
                .manager
                .getChunk(ChunkDimPos(level, pos))
                ?.unclaim(player.createCommandSourceStack(), true)
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid)
    }

    override fun makeTrader(pos: BlockPos, state: BlockState) = ClaimTraderBlockEntity(pos, state)

    override fun traderType() = ClaimTraderBlockEntity.CLAIM_TRADER
}

class ClaimTraderBlockEntity(pos: BlockPos, state: BlockState) :
    TraderBlockEntity<ClaimTraderData>(CLAIM_TRADER, pos, state) {
    companion object {
        val CLAIM_TRADER by
            register("claim_trader") {
                BlockEntityType.Builder.of(::ClaimTraderBlockEntity, ClaimTraderBlock.CLAIM_TRADER)
                    .build(null)
            }

        private fun register(
            name: String,
            type: () -> BlockEntityType<ClaimTraderBlockEntity>
        ): ObjectHolderDelegate<BlockEntityType<ClaimTraderBlockEntity>> {
            return ClaimShopForLightmansCurrency.Registries.BLOCK_ENTITIES.registerObject(
                name, type)
        }
    }

    var cancelled = false
    var chunkOwner: UUID? = null

    override fun buildNewTrader() = ClaimTraderData(level!!, worldPosition)

    override fun castOrNullify(data: TraderData) = data as? ClaimTraderData

    override fun load(compound: CompoundTag) {
        super.load(compound)
        if ("ChunkOwner" in compound) chunkOwner = compound.getUUID("ChunkOwner")
        if ("Cancelled" in compound) cancelled = compound.getBoolean("Cancelled")
    }

    override fun saveAdditional(compound: CompoundTag) {
        super.saveAdditional(compound)
        if (chunkOwner != null) compound.putUUID("ChunkOwner", chunkOwner!!)
        compound.putBoolean("Cancelled", cancelled)
    }

    override fun onBreak() {
        if (level !is ServerLevel) {
            super.onBreak()
            return
        }

        val commandSourceStack = (level as ServerLevel).server.createCommandSourceStack()
        val chunkDimPos = ChunkDimPos(level, worldPosition)
        FTBChunksAPI.api().manager.getChunk(chunkDimPos)?.unclaim(commandSourceStack, true)

        if (traderData == null) {
            super.onBreak()
            return
        }

        FTBChunksAPI.api()
            .manager
            .getPersonalData(chunkOwner)
            ?.claim(commandSourceStack, chunkDimPos, false)

        super.onBreak()
    }
}
