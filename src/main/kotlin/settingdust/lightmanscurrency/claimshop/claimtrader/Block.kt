package settingdust.lightmanscurrency.claimshop.claimtrader

import io.github.lightman314.lightmanscurrency.api.traders.TraderData
import io.github.lightman314.lightmanscurrency.api.traders.blockentity.TraderBlockEntity
import io.github.lightman314.lightmanscurrency.api.traders.blocks.TraderBlockRotatable
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor

class ClaimTraderBlock(properties: Properties) : TraderBlockRotatable(properties) {
    companion object {
        val CLAIM_TRADER =
            ClaimTraderBlock(
                Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, Float.POSITIVE_INFINITY)
                    .sound(SoundType.METAL))
    }

    override fun makeTrader(pos: BlockPos, state: BlockState) = ClaimTraderBlockEntity(pos, state)

    override fun traderType() = ClaimTraderBlockEntity.CLAIM_TRADER
}

class ClaimTraderBlockEntity(pos: BlockPos, state: BlockState) :
    TraderBlockEntity<ClaimTraderData>(CLAIM_TRADER, pos, state) {
    companion object {
        val CLAIM_TRADER =
            BlockEntityType.Builder.of(::ClaimTraderBlockEntity, ClaimTraderBlock.CLAIM_TRADER)
                .build(null)
    }

    override fun buildNewTrader() = ClaimTraderData(level!!, worldPosition)

    override fun castOrNullify(data: TraderData) = data as? ClaimTraderData
}
