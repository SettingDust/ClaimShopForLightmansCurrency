package settingdust.lightmanscurrency.claimshop.claimtrader

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI
import dev.ftb.mods.ftblibrary.math.ChunkDimPos
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI
import dev.ftb.mods.ftbteams.data.TeamManagerImpl
import io.github.lightman314.lightmanscurrency.api.misc.player.PlayerReference
import io.github.lightman314.lightmanscurrency.api.traders.TraderData
import io.github.lightman314.lightmanscurrency.api.traders.blockentity.TraderBlockEntity
import io.github.lightman314.lightmanscurrency.api.traders.blocks.TraderBlockRotatable
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.MapColor
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import settingdust.lightmanscurrency.claimshop.ClaimShopForLightmansCurrency
import thedarkcolour.kotlinforforge.forge.ObjectHolderDelegate
import thedarkcolour.kotlinforforge.forge.registerObject
import kotlin.jvm.optionals.getOrNull

class ClaimTraderBlock(properties: Properties) : TraderBlockRotatable(properties) {
    @EventBusSubscriber(modid = ClaimShopForLightmansCurrency.ID)
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

        @SubscribeEvent
        fun onPlace(event: BlockEvent.EntityPlaceEvent) {
            val player = event.entity
            if (player !is ServerPlayer) return
            if (event.placedBlock.block != CLAIM_TRADER) return

            val level = player.level() as ServerLevel
            val pos = event.pos
            val fakePlayer by lazy {
                FakePlayerFactory.get(level, ClaimShopForLightmansCurrency.FAKE_PROFILE)
            }

            if (!level.server.playerList.isOp(ClaimShopForLightmansCurrency.FAKE_PROFILE))
                level.server.playerList.op(ClaimShopForLightmansCurrency.FAKE_PROFILE)

            if (FTBTeamsAPI.api()
                .manager
                .getTeamForPlayerID(ClaimShopForLightmansCurrency.FAKE_PROFILE.id)
                .getOrNull() == null) {
                TeamManagerImpl.INSTANCE.playerLoggedIn(
                    fakePlayer,
                    ClaimShopForLightmansCurrency.FAKE_PROFILE.id,
                    ClaimShopForLightmansCurrency.FAKE_PROFILE.name)
            }

            val claimedChunk = FTBChunksAPI.api().manager.getChunk(ChunkDimPos(level, pos))
            if (claimedChunk != null && claimedChunk.teamData.isTeamMember(player.uuid)) {
                claimedChunk.unclaim(player.createCommandSourceStack(), true)
            }

            val result =
                FTBChunksAPI.api()
                    .claimAsPlayer(fakePlayer, level.dimension(), ChunkPos(pos), false)

            player.sendSystemMessage(result.message)

            if (!result.isSuccess) {
                event.isCanceled = true
                return
            }
        }
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        flag: Boolean
    ) {
        if (level !is ServerLevel) {
            super.onRemove(state, level, pos, newState, flag)
            return
        }

        FTBChunksAPI.api()
            .manager
            .getChunk(ChunkDimPos(level, pos))
            ?.unclaim(level.server.createCommandSourceStack(), true)

        val blockEntity = ClaimTraderBlockEntity.CLAIM_TRADER.getBlockEntity(level, pos)!!
        val traderData = blockEntity.traderData
        if (traderData == null) {
            super.onRemove(state, level, pos, newState, flag)
            return
        }

        val playerForContext = traderData.owner.playerForContext
        if (playerForContext == PlayerReference.NULL) {
            super.onRemove(state, level, pos, newState, flag)
            return
        }

        val result =
            FTBChunksAPI.api()
                .manager
                .getPersonalData(playerForContext.id)
                ?.claim(level.server.createCommandSourceStack(), ChunkDimPos(level, pos), false)
        result?.message?.let { playerForContext.player.sendSystemMessage(it) }

        super.onRemove(state, level, pos, newState, flag)
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

    override fun buildNewTrader() = ClaimTraderData(level!!, worldPosition)

    override fun castOrNullify(data: TraderData) = data as? ClaimTraderData
}
